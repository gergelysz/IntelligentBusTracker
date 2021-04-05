package com.example.intelligentbustracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.activity.MapsActivity
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.model.Status
import com.example.intelligentbustracker.model.User
import com.example.intelligentbustracker.util.Common
import com.example.intelligentbustracker.util.GeneralUtils
import com.example.intelligentbustracker.util.IntelligentTrackerUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.Timer
import java.util.TimerTask
import java.util.stream.Collectors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class LocationService : Service() {

    companion object {
        private const val CHANNEL_ID = "channel_01"
        private const val PACKAGE_NAME = "com.example.intelligentbustracker"
        private const val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"
        private const val NOTIFICATION_ID = 1234
        private const val TAG = "LocationService"
    }

    private lateinit var busesWithStations: List<Bus>
    private lateinit var closestStations: List<Station>
    private var numberOfStations: Int = 5
    private var maxDistance: Double = 1000.0

    private var firestore: FirebaseFirestore? = null
    private var usersCollectionRef: CollectionReference? = null

    private var timer: Timer? = null

    private var runningInBackground: Boolean = false
    var running: Boolean = false

    private val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        internal val service: LocationService
            get() = this@LocationService
    }

    private var mChangingConfiguration = false
    private var mNotificationManager: NotificationManager? = null
    private var locationRequest: LocationRequest? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var mServiceHandler: Handler? = null
    private var mLocation: Location? = null

    private var uploaded = false

    private var currentUser: User? = null
    private var currentUserDocumentReference: DocumentReference? = null

    private val notification: Notification
        get() {
            val intent = Intent(this, LocationService::class.java)
            val intentToActivity = Intent(this, MapsActivity::class.java)
            val text = Common.getLocationText(mLocation)
            intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
            val servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val activityPendingIntent = PendingIntent.getActivity(this, 0, intentToActivity, PendingIntent.FLAG_UPDATE_CURRENT)
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .addAction(R.drawable.ic_bus_launcher_foreground, "Launch", activityPendingIntent)
                .addAction(R.drawable.ic_cancel, "Cancel", servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle())
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_bus_station_white)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                builder.setChannelId(CHANNEL_ID)
//            }
            return builder.build()
        }

    private fun uploadUser(user: User) = CoroutineScope(Dispatchers.IO).launch {
        try {
            usersCollectionRef?.let {
                currentUserDocumentReference = it.add(user).await()
                currentUserDocumentReference?.let { currentUserRef ->
                    user.id = currentUserRef.id
                    withContext(Dispatchers.Main) {
                        uploaded = true
                        EventBus.getDefault().postSticky(user)
                        Log.i(TAG, "uploadUser: Successfully uploaded user data.")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LocationService, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    //    private fun updateUser(latitude: Double, longitude: Double) = CoroutineScope(Dispatchers.IO).launch {
    private fun updateUser(user: User) = CoroutineScope(Dispatchers.IO).launch {
        try {
            currentUserDocumentReference?.let {
                it.update(
                    mapOf(
                        "bus" to user.bus,
                        "direction" to user.direction,
                        "latitude" to user.latitude,
                        "longitude" to user.longitude,
                    )
                ).await()
                withContext(Dispatchers.Main) {
                    EventBus.getDefault().postSticky(user)
                    Log.i(TAG, "updateUser: Successfully updated user data.")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LocationService, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestUsersData(currentUserId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            usersCollectionRef?.get()?.addOnSuccessListener { documents ->
                val users: MutableList<User> = documents.documents.stream()
                    .filter { x -> x.id != currentUserId }
                    .map { y -> y.toObject<User>()?.withId(y.id) }
                    .collect(Collectors.toList())
                if (users.isNotEmpty()) {
                    EventBus.getDefault().postSticky(users)
                }
            }?.addOnFailureListener { exception ->
                Toast.makeText(this@LocationService, exception.message, Toast.LENGTH_LONG).show()
            }?.await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LocationService, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // TODO: Check if next station (with direction that has current station) is getting closer
    private fun processLocationDataWithIntelligentTracker(locationResult: LocationResult) = CoroutineScope(Dispatchers.Default).launch {
        val lastLocation = locationResult.lastLocation
        val speed = IntelligentTrackerUtils.getSpeedkmph(lastLocation.speed)
        Log.i(TAG, "processLocationDataWithIntelligentTracker: current speed = $speed km/h")
        // Average walking speed is 5 km/h
        if (speed > 5F) {
            BusTrackerApplication.status.value?.let {
                if (it == Status.WAITING_FOR_BUS) {
                    BusTrackerApplication.status.postValue(Status.ON_BUS)
                }
            }
            val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            Log.i(TAG, "processLocationDataWithIntelligentTracker: speed is higher than 5 km/h, user probably on bus")
            if (this@LocationService::closestStations.isInitialized) {
                val stations = GeneralUtils.getNumberOfClosestStationsFromListOfStations(currentLatLng, BusTrackerApplication.stations, numberOfStations, maxDistance)
                if (stations.size >= closestStations.size && numberOfStations > 1) {
                    numberOfStations -= 1
                    closestStations = GeneralUtils.getNumberOfClosestStationsFromListOfStations(currentLatLng, BusTrackerApplication.stations, numberOfStations, maxDistance)
                } else {
                    if (stations.isEmpty() && numberOfStations < 5) {
                        numberOfStations += 1
                        closestStations = GeneralUtils.getNumberOfClosestStationsFromListOfStations(currentLatLng, BusTrackerApplication.stations, numberOfStations, maxDistance)
                    }
                }
            } else {
                closestStations = GeneralUtils.getNumberOfClosestStationsFromListOfStations(currentLatLng, BusTrackerApplication.stations, numberOfStations, maxDistance)
            }
            val closestStationsLog = closestStations.joinToString(", ") { it.name }
            Log.i(TAG, "processLocationDataWithIntelligentTracker: closest stations for based on current location = $closestStationsLog")
            busesWithStations = GeneralUtils.getBusesWithGivenStations(closestStations)
            val busesWithStationsLog = busesWithStations.joinToString(", ") { it.number.toString() }
            Log.i(TAG, "processLocationDataWithIntelligentTracker: possible buses = $busesWithStationsLog")
        } else {
            BusTrackerApplication.status.value?.let {
                if (it == Status.ON_BUS) {
                    BusTrackerApplication.status.postValue(Status.WAITING_FOR_BUS)
                    currentUser?.let { user ->
                        if (uploaded) {
                            user.bus = 0
                        }
                    }
                }
            }
            Log.i(TAG, "processLocationDataWithIntelligentTracker: speed is lower than 5 km/h, user probably walking")
        }
    }

    override fun onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {

            private lateinit var intelligentTrackerTask: Job

            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                if (BusTrackerApplication.intelligentTracker.value.toBoolean()) {
                    intelligentTrackerTask = processLocationDataWithIntelligentTracker(p0)
                }
                mLocation = p0.lastLocation
                Log.i(TAG, "onLocationResult: ${p0.lastLocation} speed = ${p0.lastLocation.speed} m/s")
                mLocation?.let { myLocation ->
                    currentUser?.let {
                        if (uploaded) {
                            it.latitude = myLocation.latitude
                            it.longitude = myLocation.longitude
                            updateUser(it)
                        }
                    } ?: run {
                        currentUser = User(0, myLocation.latitude, myLocation.longitude, 0)
                        uploadUser(currentUser!!)
                    }
                }
                runBlocking {
                    intelligentTrackerTask.join()
                }
            }
        }

        // firestore db
        firestore = FirebaseFirestore.getInstance()
        firestore?.let {
            usersCollectionRef = it.collection("users")
        }

        createLocationRequest()

        val handlerThread = HandlerThread("BusTracker")
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = packageName
            val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            mNotificationManager!!.createNotificationChannel(mChannel)
        }

        // set up timer task
        timer = Timer()
        timer?.schedule(
            object : TimerTask() {
                override fun run() {
                    currentUser?.let {
                        if (it.id.isNotEmpty()) {
                            Log.i(TAG, "run: requesting users data from TimerTask")
                            requestUsersData(it.id)
                        } else {
                            Log.d(TAG, "run: currentUser doesn't have an ID yet, initializing with current user ID as empty String")
                            requestUsersData("")
                        }
                    } ?: run {
                        Log.d(TAG, "run: currentUser not initialized yet, initializing with current user ID as empty String")
                        requestUsersData("")
                    }
                }
            },
            2000,
            5000
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedFromNotification = intent!!.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)
        if (startedFromNotification) {
            removeLocationUpdates()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    fun requestLocationUpdates() {
        try {
            Common.setRequestingLocationUpdates(this, true)
            startService(Intent(applicationContext, LocationService::class.java))
            running = true
            uploaded = false
            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper())
        } catch (ex: SecurityException) {
            Common.setRequestingLocationUpdates(this, false)
            Log.e(TAG, "Lost location permission. $ex")
        }
    }

    fun removeLocationUpdates() {
        try {
            val removeUpdatesTask = fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
            removeUpdatesTask.addOnSuccessListener {
                running = false
                Common.setRequestingLocationUpdates(this, false)
                currentUserDocumentReference?.delete()
                currentUserDocumentReference = null
                currentUser = null
                stopSelf()
            }.addOnFailureListener {
                Toast.makeText(this@LocationService, "Failed to remove location updates.", Toast.LENGTH_LONG).show()
            }
        } catch (ex: SecurityException) {
            Common.setRequestingLocationUpdates(this, true)
            Log.e(TAG, "Lost location permission. Could not remove update. $ex")
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest!!.interval = BusTrackerApplication.updateInterval.toLong()
        locationRequest!!.fastestInterval = BusTrackerApplication.updateInterval.toLong()
        locationRequest!!.priority = BusTrackerApplication.updateAccuracy.toInt()
    }

    override fun onBind(intent: Intent?): IBinder {
        stopForeground(true)
        runningInBackground = false
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        runningInBackground = false
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!mChangingConfiguration && Common.requestingLocationUpdates(this)) {
            startForeground(NOTIFICATION_ID, notification)
            runningInBackground = true
        }
        return true
    }

    override fun onDestroy() {
        mServiceHandler!!.removeCallbacksAndMessages(null)
        timer?.let {
            it.cancel()
            it.purge()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Delete current user data
        if (!Common.requestingLocationUpdates(this)) {
            val removeUpdatesTask = fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
            removeUpdatesTask.addOnSuccessListener {
                currentUserDocumentReference?.delete()
                stopSelf()
                super.onTaskRemoved(rootIntent)
            }.addOnFailureListener {
                Toast.makeText(this@LocationService, "Failed to remove location updates.", Toast.LENGTH_LONG).show()
            }
        }
    }
}