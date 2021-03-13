package com.example.intelligentbustracker.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.directions.route.AbstractRouting
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.location.BackgroundLocation
import com.example.intelligentbustracker.service.LocationService
import com.example.intelligentbustracker.util.Common
import com.example.intelligentbustracker.util.GeneralUtils
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_maps.remove_location_updates_button
import kotlinx.android.synthetic.main.activity_maps.request_location_updates_button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener, RoutingListener {

    private lateinit var mMap: GoogleMap
    private lateinit var stationMarkers: List<MarkerOptions>

    private var myLocation: Location? = null
    private var polyLines = arrayListOf<Polyline>()
    private var clickedMarker: Marker? = null

    private var mService: LocationService? = null
    private var mBound = false
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as LocationService.LocalBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    @Suppress("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBackgroundLocationRetrieve(event: BackgroundLocation) {
        Toast.makeText(this@MapsActivity, Common.getLocationText(event.location), Toast.LENGTH_SHORT).show()
        myLocation = event.location
        if (BusTrackerApplication.focusOnCenter.toBoolean()) {
            moveCamera(event.location)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this@MapsActivity)

        // get list of permissions
        val permissions = GeneralUtils.getPermissionList()

        Dexter.withContext(applicationContext)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    request_location_updates_button.setOnClickListener {
                        mService!!.requestLocationUpdates()
                    }
                    remove_location_updates_button.setOnClickListener {
                        mService!!.removeLocationUpdates()
                    }

                    setButtonState(Common.requestingLocationUpdates(this@MapsActivity))
                    bindService(Intent(this@MapsActivity, LocationService::class.java), mServiceConnection, Context.BIND_AUTO_CREATE)
                }

                override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                    // Not implemented
                }
            }).check()
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
        unbindService(mServiceConnection)
    }

    /**
     * Listens for changes in preferences.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Common.KEY_REQUEST_LOCATION_UPDATE -> {
                setButtonState(sharedPreferences!!.getBoolean(Common.KEY_REQUEST_LOCATION_UPDATE, false))
            }
        }
    }

    private fun setButtonState(boolean: Boolean) {
        if (boolean) {
            remove_location_updates_button.isEnabled = true
            request_location_updates_button.isEnabled = false
        } else {
            remove_location_updates_button.isEnabled = false
            request_location_updates_button.isEnabled = true
        }
    }

    /**
     * Moves the camera to the given location.
     */
    private fun moveCamera(location: Location) {
        val newLocation = LatLng(location.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLocation))
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapsActivity, resources.getIdentifier(BusTrackerApplication.mapTheme, "raw", this.packageName)))

        val scope = CoroutineScope(Dispatchers.Default)
        val jobCreateMarkers: Deferred<List<MarkerOptions>> = scope.async { MapUtils.createStationsMarkers(BusTrackerApplication.stations, applicationContext) }

        mMap.setOnMapClickListener { clickedLocation ->
            val closestStation = MapUtils.getClosestStation(clickedLocation)
            closestStation?.let {
                findRoutes(clickedLocation, LatLng(closestStation.latitude, closestStation.longitude))
                Toast.makeText(this@MapsActivity, "Found closest station: ${closestStation.name}", Toast.LENGTH_SHORT).show()
            }
        }

        mMap.setMaxZoomPreference(20F)
        mMap.setMinZoomPreference(15F)
        val tgMuresDefault = LatLng(46.539892, 24.558334)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(tgMuresDefault))

        // Get results from async methods
        runBlocking {
            stationMarkers = try {
                jobCreateMarkers.await()
            } catch (ex: Exception) {
                ArrayList()
            }
            MapUtils.addStationMarkers(mMap, stationMarkers)
        }
    }

    /**
     * Method to find the route.
     */
    private fun findRoutes(start: LatLng?, end: LatLng?) {
        if (start == null || end == null) {
            Toast.makeText(this@MapsActivity, "Unable to get location", Toast.LENGTH_LONG).show()
        } else {
            val routing = Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.WALKING)
                .withListener(this@MapsActivity)
                .alternativeRoutes(true)
                .waypoints(start, end)
                .key(getString(R.string.google_maps_key))
                .build()
            routing.execute()
        }
    }

    /**
     * When route if found.
     */
    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
        polyLines = if (!polyLines.isNullOrEmpty()) {
            for (polyline in polyLines) {
                polyline.remove()
            }
            ArrayList()
        } else {
            ArrayList()
        }
        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        //add route(s) to the map using polyline
        for (i in 0 until route.size) {
            if (i == shortestRouteIndex) {
                polyOptions.color(ContextCompat.getColor(this@MapsActivity, R.color.green))
                polyOptions.width(7f)
                polyOptions.addAll(route[shortestRouteIndex].points)
                val polyline = mMap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                polyLines.add(polyline)
            } else {
            }
        }

        clickedMarker = if (clickedMarker == null) {
            mMap.addMarker(MarkerOptions().position(polylineStartLatLng!!).title("My location"))
        } else {
            clickedMarker!!.remove()
            mMap.addMarker(MarkerOptions().position(polylineStartLatLng!!).title("My location"))
        }
    }

    //Routing call back functions.
    override fun onRoutingFailure(e: RouteException) {
        val parentLayout: View = findViewById(android.R.id.content)
        val snackbar = Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    override fun onRoutingStart() {
        Toast.makeText(this@MapsActivity, "Finding Route...", Toast.LENGTH_LONG).show()
    }

    override fun onRoutingCancelled() {
        Toast.makeText(this@MapsActivity, "Routing cancelled", Toast.LENGTH_LONG).show()
    }
}