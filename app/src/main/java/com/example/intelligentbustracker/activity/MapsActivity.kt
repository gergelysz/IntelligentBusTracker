package com.example.intelligentbustracker.activity

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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.RoutingListener
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.fragment.MapAssistFragment
import com.example.intelligentbustracker.fragment.RoutingMapFragment
import com.example.intelligentbustracker.fragment.SearchDialogFragment
import com.example.intelligentbustracker.fragment.UserStatusFragment
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.model.Status
import com.example.intelligentbustracker.model.User
import com.example.intelligentbustracker.model.UserStatus
import com.example.intelligentbustracker.service.LocationService
import com.example.intelligentbustracker.util.Common
import com.example.intelligentbustracker.util.DataManager
import com.example.intelligentbustracker.util.GeneralUtils
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.activity_maps.bottom_navigation_view
import kotlinx.android.synthetic.main.activity_maps.text_view_closest_station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener, RoutingListener, SearchDialogFragment.SearchDialogListener, GoogleMap.OnMapClickListener, UserStatusFragment.OnStatusChangeListener,
    MapAssistFragment.OnMapPositionClickListener {

    private lateinit var mMap: GoogleMap

    private lateinit var chosenStation: Station
    private lateinit var closestStation: Station
//    private lateinit var closestStationToSelectedDestination: Station

    private var currentUser: MutableLiveData<User> = MutableLiveData()
    private var usersList: MutableLiveData<List<User>> = MutableLiveData()

    private var currentUserMarker: Marker? = null
    private var usersMarkers: MutableList<Marker>? = null
    private var stationMarkers: List<Marker>? = null

    private var latestMapTheme: String = BusTrackerApplication.mapTheme.value ?: DataManager(this@MapsActivity).getSettingValueString(BusTrackerApplication.getInstance().getString(R.string.key_map_theme))

    private var routingMapFragment: RoutingMapFragment? = null
    private var searchDialogFragment: SearchDialogFragment? = null
    private var userStatusFragment: UserStatusFragment? = null
    private var mapAssistFragment: MapAssistFragment? = null

//    private lateinit var myLocation: Location

    private var polyLines = arrayListOf<Polyline>()
    private var clickedMarker: Marker? = null

    private var polyLinesDirection1 = arrayListOf<Polyline>()

    private var mService: LocationService? = null
    private var mBound = false

//    private lateinit var client: ActivityRecognitionClient

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as LocationService.LocalBinder
            mService = binder.service
            mBound = true
            if (BusTrackerApplication.intelligentTracker.value.toBoolean() && !Common.requestingLocationUpdates(this@MapsActivity)) {
                mService?.requestLocationUpdates()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    companion object {
        private const val TAG = "MapsActivity"
    }

    private fun processCurrentUser(user: User) {
        val latLngLocation = LatLng(user.latitude, user.longitude)

        /** Get closest station for new location */
        if (!this::closestStation.isInitialized) {
            closestStation = GeneralUtils.getClosestStation(latLngLocation)
        }
        updateClosestStationTextView(latLngLocation, closestStation.name)

        /** Update/Create marker */
        currentUserMarker?.let {
            MapUtils.animateMarker(it, LatLng(user.latitude, user.longitude), mMap.projection)
        } ?: run {
            if (Common.requestingLocationUpdates(this@MapsActivity)) {
                val markerId: Int = MapUtils.getMarkerIdForCurrentUser()
                currentUserMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(user.latitude, user.longitude))
                        .title("You")
                        .snippet("Your ID is: ${user.id}")
                        .icon(MapUtils.bitmapFromVector(this@MapsActivity, markerId))
                )
                MapUtils.moveCameraToLocation(mMap, LatLng(user.latitude, user.longitude))
                text_view_closest_station.text = this@MapsActivity.getString(R.string.closest_station, closestStation.name)
            }
        }

        if (BusTrackerApplication.focusOnCenter.toBoolean()) {
            MapUtils.moveCameraToLocation(mMap, latLngLocation)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUserDataRetrieve(event: User) {
        Log.i(TAG, "onUserDataRetrieve: ${event.id} - ${event.latitude} / ${event.longitude}")
        currentUser.value = event
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBackgroundDBDataRetrieve(users: MutableList<User>) {
        Log.i(TAG, "onBackgroundDBDataRetrieve: received ${users.size} updated users")
        usersList.value = users
    }

    private fun updateClosestStationTextView(location: LatLng, closestStationName: String) {
        val closestStationNew = GeneralUtils.getClosestStation(location)
        if (closestStationName != closestStationNew.name) {
            text_view_closest_station.text = this@MapsActivity.getString(R.string.closest_station, closestStationNew.name)
        }
    }

//    private fun requestActivityUpdates() {
//        client.requestActivityTransitionUpdates(
//            IntelligentTrackerUtils.getActivityTransitionRequest(), getPendingIntent()
//        ).addOnSuccessListener {
//            Log.i(TAG, "requestActivityUpdates: Success")
//        }.addOnFailureListener {
//            Log.i(TAG, "requestActivityUpdates: Failure")
//        }
//    }

//    private fun getPendingIntent(): PendingIntent {
//        val intent = Intent(this, ActivityTransitionReceiver::class.java)
//        return PendingIntent.getBroadcast(this, Constants.REQUEST_CODE_INTENT_ACTIVITY_TRANSITION, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//    }

//    private fun removeActivityUpdates() {
//        client.removeActivityUpdates(getPendingIntent())
//    }

    /**
     * When status is updated from
     * UserStatusFragment.
     */
    override fun onStatusChange(status: UserStatus) {
        Log.i(TAG, "onStatusChange: $status")
        val menu = bottom_navigation_view.menu
        if (status.tracking) {
            if (Common.requestingLocationUpdates(this@MapsActivity)) {
                Toast.makeText(this@MapsActivity, "The application is already tracking your location", Toast.LENGTH_SHORT).show()
                menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_on)
            } else {
                mService?.let {
                    it.requestLocationUpdates()
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_on)
                }
            }
        } else if (!status.tracking) {
            if (Common.requestingLocationUpdates(this@MapsActivity)) {
                mService?.let {
                    it.removeLocationUpdates()
                    currentUserMarker?.remove()
                    currentUserMarker = null
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_off)
                }
            } else {
                Toast.makeText(this@MapsActivity, "The application wasn't tracking your location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        removeActivityUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

//        client = ActivityRecognition.getClient(this)
//        requestActivityUpdates()

        text_view_closest_station.text = this@MapsActivity.getString(R.string.closest_station, "Not found")

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this@MapsActivity)

        val menu = bottom_navigation_view.menu

        BusTrackerApplication.status.observe(this@MapsActivity, { currentStatus ->
            if (BusTrackerApplication.intelligentTracker.value.toBoolean()) {
                if (currentStatus == Status.ON_BUS) {
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_bus_station_white)
                } else {
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_user_standing_white)
                }
            }
        })

        BusTrackerApplication.intelligentTracker.observe(this@MapsActivity, { intelligentTracker ->
            if (intelligentTracker.toBoolean()) {
                menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_user_standing_white)
                if (!Common.requestingLocationUpdates(this@MapsActivity)) {
                    mService?.requestLocationUpdates()
                }
            } else {
                if (Common.requestingLocationUpdates(this@MapsActivity)) {
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_on)
                } else {
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_off)
                }
            }
        })

        bottom_navigation_view.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                /** Search for a station, opens SearchDialog Fragment */
                R.id.nav_search -> {
                    searchDialogFragment = SearchDialogFragment()
                    searchDialogFragment?.show(supportFragmentManager, "SearchForStation")
                }
                /** Toggle location tracking */
                R.id.nav_status -> {
                    if (BusTrackerApplication.intelligentTracker.value.toBoolean()) {
                        if (!Common.requestingLocationUpdates(this@MapsActivity)) {
                            mService?.requestLocationUpdates()
                            menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_user_standing_white)
                        } else {
                            Toast.makeText(this@MapsActivity, "Intelligent tracker is currently managing your location data.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        userStatusFragment = UserStatusFragment(this)
                        userStatusFragment?.show(supportFragmentManager, "UserStatusFragment")
                    }
                }
                /** Access settings by launching Settings Activity */
                R.id.nav_settings -> startActivity(Intent(this@MapsActivity, SettingsActivity::class.java))
            }
            true
        }
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
        MapUtils.setupMap(mMap, this@MapsActivity, BusTrackerApplication.mapTheme.value!!)

        /**
         * Center map to user's current location
         * if it's been set.
         */
        currentUser.value?.let {
            if (it.latitude > 0 && it.longitude > 0) {
                MapUtils.moveCameraToLocation(mMap, LatLng(it.latitude, it.longitude))
            }
        }

        /**
         * Observe Map Theme changes and if color styles
         * differ then change the icons of the markers.
         */
        BusTrackerApplication.mapTheme.observe(this@MapsActivity, { themeString ->
            if (MapUtils.mapThemeChangeNeeded(themeString, latestMapTheme)) {
                latestMapTheme = themeString
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapsActivity, resources.getIdentifier(themeString, "raw", packageName)))
                stationMarkers?.let {
                    MapUtils.updateStationMarkersColor(it, this@MapsActivity)
                }
                usersMarkers?.let {
                    MapUtils.updateUserMarkersColor(it, this@MapsActivity)
                }
                currentUserMarker?.let {
                    MapUtils.updateCurrentUserMarkerColor(it, this@MapsActivity)
                }
            }
        })

        /**
         * Observe current user's location changes.
         */
        currentUser.observe(this@MapsActivity, {
            processCurrentUser(it)
        })

        /**
         * Observe active users.
         */
        usersList.observe(this@MapsActivity, { users ->
            // Filter out users that have bus as "0"
            // meaning they are waiting for a bus.
            val filteredUsers = users.filter { x -> x.bus != 0 }

            usersMarkers?.let {
                val currentUserId = currentUser.value?.id ?: ""
                MapUtils.updateUsersMarkers(filteredUsers, it, currentUserId, mMap, this@MapsActivity)
            } ?: run {
                usersMarkers = MapUtils.createAndAddUsersMarkers(filteredUsers, this@MapsActivity, mMap)
            }
        })

        val scope = CoroutineScope(Dispatchers.Default)
        val jobCreateMarkers: Deferred<List<MarkerOptions>> = scope.async { MapUtils.createStationsMarkers(BusTrackerApplication.stations, applicationContext) }

//        if (this::myLocation.isInitialized) {
//            mMap.setOnMapClickListener(null)
//        } else {
//            if (this::chosenStation.isInitialized) {
        mMap.setOnMapClickListener(this@MapsActivity)
//            } else {
//                Toast.makeText(this@MapsActivity, "Please choose a station from search first.", Toast.LENGTH_SHORT).show()
//            }
//        }

        // Get results from async methods
        runBlocking {
            stationMarkers = try {
                MapUtils.addStationMarkers(mMap, jobCreateMarkers.await())
            } catch (ex: Exception) {
                ArrayList()
            }
        }
    }

    /**
     * Listener for selected station
     * in station search dialog.
     */
    override fun searchedForStation(station: Station) {
        chosenStation = station

        currentUserMarker?.let {
            if (BusTrackerApplication.askLocationChange.toBoolean()) {
                showLocationChangeDialog(LatLng(chosenStation.latitude, chosenStation.longitude), LatLng(it.position.latitude, it.position.longitude))
            } else {
                initializeRouting(LatLng(it.position.latitude, it.position.longitude), LatLng(station.latitude, station.longitude))
            }
        } ?: run {
            initializeMapAssistForManualCurrentLocation()
        }
    }

    /**
     * Callback from MapAssistFragment to
     * choose current user location.
     */
    override fun onMapAssistClick(position: LatLng) {
        val markerId: Int = MapUtils.getMarkerIdForCurrentUser()
        currentUserMarker = mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title("You")
                .snippet("Your current position")
                .icon(MapUtils.bitmapFromVector(this@MapsActivity, markerId))
        )
        MapUtils.moveCameraToLocation(mMap, position)
        if (!this::closestStation.isInitialized) {
            closestStation = GeneralUtils.getClosestStation(position)
        }
        text_view_closest_station.text = this@MapsActivity.getString(R.string.closest_station, closestStation.name)

//        initializeManualRouting(position)
//        onMapClick(position)
//        initializeRouting()
    }

    /**
     * Listener for selected location on map
     * to be used as current location.
     */
    override fun onMapClick(position: LatLng) {
        currentUserMarker?.let {
            if (BusTrackerApplication.askLocationChange.toBoolean()) {
                showLocationChangeDialog(position, LatLng(it.position.latitude, it.position.longitude))
            } else {
                initializeRouting(LatLng(it.position.latitude, it.position.longitude), position)
            }
        } ?: run {
            initializeMapAssistForManualCurrentLocation()
        }
    }

    private fun showLocationChangeDialog(destinationPosition: LatLng, currentPosition: LatLng) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MapsActivity)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.custom_location_setup_dialog, null)
        builder.setView(dialogView)
        val buttonYes: Button = dialogView.findViewById(R.id.custom_location_setup_yes_button)
        val buttonNo: Button = dialogView.findViewById(R.id.custom_location_setup_no_button)

        val alertDialog: AlertDialog = builder.create()

        buttonYes.setOnClickListener {
            initializeMapAssistForManualCurrentLocation()
            alertDialog.cancel()
        }

        buttonNo.setOnClickListener {
            initializeRouting(destinationPosition, currentPosition)
            alertDialog.cancel()
        }

        alertDialog.show()
    }

//    private fun initializeManualRouting(destinationPosition: LatLng, currentPosition: LatLng) {
////        closestStationToSelectedDestination = GeneralUtils.getClosestStation(destinationPosition)
//        initializeRouting(destinationPosition, currentPosition)
//    }

    private fun initializeMapAssistForManualCurrentLocation() {
        Toast.makeText(this@MapsActivity, "Please tap on the map to select your current location.", Toast.LENGTH_SHORT).show()
        mapAssistFragment = MapAssistFragment(this@MapsActivity)
        mapAssistFragment?.show(supportFragmentManager, "MapAssist")
    }

    private fun initializeRouting(currentPosition: LatLng, destinationPosition: LatLng) {
        routingMapFragment = RoutingMapFragment()
        routingMapFragment?.let {
            val bundle = Bundle()
            bundle.putParcelable("current_latlng", currentPosition)
            bundle.putParcelable("destination_latlng", destinationPosition)
            it.arguments = bundle
            it.show(supportFragmentManager, "Routing")
        }
//        Log.i(TAG, "initializeRouting: Searched for station: ${station.name}, found ${busesWithNumber.size} buses")
    }

//    private fun initializeRouting(station: Station, currentPosition: LatLng) {
//        val busesWithNumber: IntArray = GeneralUtils.getBusNumbersWithGivenStation(station.name)
//        if (busesWithNumber.isNotEmpty()) {
//            routingMapFragment = RoutingMapFragment()
//            routingMapFragment?.let {
//                val bundle = Bundle()
//                bundle.putIntArray("buses_with_number", busesWithNumber)
//                bundle.putString("station_name", station.name)
//                bundle.putParcelable("current_latlng", currentPosition)
//                it.arguments = bundle
//                it.show(supportFragmentManager, "Routing")
//            }
//            Log.i(TAG, "initializeRouting: Searched for station: ${station.name}, found ${busesWithNumber.size} buses")
//        } else {
//            Toast.makeText(this@MapsActivity, "Couldn't find any bus with ${station.name} in their route.", Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onStart() {
        super.onStart()

        if (!mBound) {
            bindService(Intent(this@MapsActivity, LocationService::class.java), mServiceConnection, Context.BIND_AUTO_CREATE)
            mBound = true
        }

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
    }

    /**
     * Listens for changes in preferences.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val menu = bottom_navigation_view.menu
        when (key) {
            Common.KEY_REQUEST_LOCATION_UPDATE -> {
                if (sharedPreferences!!.getBoolean(Common.KEY_REQUEST_LOCATION_UPDATE, false)) {
                    if (BusTrackerApplication.intelligentTracker.value.toBoolean()) {
                        menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_user_standing_white)
                    } else {
                        menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_on)
                    }
                } else {
                    menu.findItem(R.id.nav_status).setIcon(R.drawable.ic_location_white_off)
                }
            }
        }
    }

    private var routingListenerToStation: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex?.message}")
        }

        override fun onRoutingStart() {
            Log.i(TAG, "onRoutingStart: Finding Route")
        }

        override fun onRoutingSuccess(route: java.util.ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesDirection1 = ArrayList()
            val polyOptions = PolylineOptions()
            //add route(s) to the map using polyline
            for (i in 0 until route.size) {
                if (i == shortestRouteIndex) {
                    polyOptions.color(ContextCompat.getColor(this@MapsActivity, R.color.dark_orange))
                    polyOptions.width(7f)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyLinesDirection1.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
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
    override fun onRoutingFailure(ex: RouteException) {
        Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex.message}")
//        val parentLayout: View = findViewById(android.R.id.content)
//        val snackbar = Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_SHORT)
//        snackbar.show()
    }

    override fun onRoutingStart() {
        Log.i(TAG, "onRoutingStart: Finding Route")
    }

    override fun onRoutingCancelled() {
        Log.w(TAG, "onRoutingCancelled: Routing cancelled")
    }
}