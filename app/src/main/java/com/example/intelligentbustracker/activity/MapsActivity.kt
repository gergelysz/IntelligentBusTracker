package com.example.intelligentbustracker.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.location.BackgroundLocation
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.service.LocationService
import com.example.intelligentbustracker.util.Common
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener, RoutingListener {

    private lateinit var mMap: GoogleMap

    private lateinit var stations: ArrayList<Station>
    private lateinit var buses: ArrayList<Bus>

    var myLocation: Location? = null
    var destinationLocation: Location? = null
    private var start: LatLng? = null
    private var end: LatLng? = null
    private var polylines: ArrayList<Polyline>? = null

    private lateinit var deferredStationMarkers: Deferred<ArrayList<MarkerOptions>>
    lateinit var stationMarkers: ArrayList<MarkerOptions>


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
        Toast.makeText(this, Common.getLocationText(event.location), Toast.LENGTH_SHORT).show()
        myLocation = event.location
        if (MainActivity.focusOnCenter.toBoolean()) {
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

        // get items
        stations = MainActivity.stations
        buses = MainActivity.buses

        // permissions
        Dexter.withContext(applicationContext)
            .withPermissions(
                listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            )
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
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapsActivity, resources.getIdentifier(MainActivity.mapTheme, "raw", this.packageName)))

        mMap.setOnMapClickListener {
            end = it
            start = LatLng(myLocation!!.latitude, myLocation!!.longitude)
            findRoutes(start, end)
        }

        mMap.setMaxZoomPreference(20F)
        mMap.setMinZoomPreference(15F)
        val tgMuresDefault = LatLng(46.539892, 24.558334)
        mMap.addMarker(MarkerOptions().position(tgMuresDefault).title("Marker in Marosvásárhely"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(tgMuresDefault))

        GlobalScope.launch(Dispatchers.Main) {
            deferredStationMarkers = async(Dispatchers.Default) { MapUtils.createStationsMarkers(MainActivity.stations, applicationContext) }
            // TODO: Other async tasks
            stationMarkers = deferredStationMarkers.await()
            MapUtils.addStationMarkers(mMap, stationMarkers)
        }
        Log.e("MapUtils", "MapUtils: after called ")
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

    //If Route finding success..
    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
        val center = CameraUpdateFactory.newLatLng(start)
        val zoom = CameraUpdateFactory.zoomTo(16f)
        if (polylines != null) {
            polylines = ArrayList()
        }
        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        var polylineEndLatLng: LatLng? = null
        polylines = ArrayList()
        //add route(s) to the map using polyline
        for (i in 0 until route.size) {
            if (i == shortestRouteIndex) {
                polyOptions.color(ContextCompat.getColor(this, R.color.blue))
                polyOptions.width(7f)
                polyOptions.addAll(route[shortestRouteIndex].points)
                val polyline = mMap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k: Int = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                polylines!!.add(polyline)
            } else {
            }
        }

        //Add Marker on route starting position
        val startMarker = MarkerOptions()
        startMarker.position(polylineStartLatLng!!)
        startMarker.title("My Location")
        mMap.addMarker(startMarker)

        //Add Marker on route ending position
        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title("Destination")
        mMap.addMarker(endMarker)
    }

    override fun onRoutingCancelled() {
        findRoutes(start, end)
    }

    fun onConnectionFailed(connectionResult: ConnectionResult) {
        findRoutes(start, end)
    }

    // function to find Routes.
    private fun findRoutes(start: LatLng?, end: LatLng?) {
        if (start == null || end == null) {
            Toast.makeText(this@MapsActivity, "Unable to get location", Toast.LENGTH_LONG).show()
        } else {
            val routing = Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(start, end)
                .key(getString(R.string.google_maps_key))
                .build()
            routing.execute()
        }
    }
}