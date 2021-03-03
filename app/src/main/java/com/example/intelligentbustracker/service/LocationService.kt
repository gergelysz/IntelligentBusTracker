package com.example.intelligentbustracker.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.location.BackgroundLocation
import com.example.intelligentbustracker.util.Common
import com.example.intelligentbustracker.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.greenrobot.eventbus.EventBus

class LocationService : Service() {

    companion object {
        private val CHANNEL_ID = "channel_01"
        private val PACKAGE_NAME = "com.example.intelligentbustracker"
        private val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"
        private val UPDATE_INTERVAL_IN_MIL: Long = 2000
        private val FASTEST_UPDATE_INTERVAL_IN_MIL: Long = UPDATE_INTERVAL_IN_MIL / 2
        private val NOTIFICATION_ID = 1234
    }

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

    private val notification: Notification
        get() {
            val intent = Intent(this, LocationService::class.java)
            val text = Common.getLocationText(mLocation)
            intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
            val servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val activityPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val builder = NotificationCompat.Builder(this)
                .addAction(R.drawable.common_google_signin_btn_icon_dark_focused, "Launch", activityPendingIntent)
                .addAction(R.drawable.common_full_open_on_phone, "Cancel", servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID)
            }
            return builder.build()
        }

    override fun onCreate() {
        Log.e("***************", "onCreate LocationService: asd")
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                onNewLocation(p0!!.lastLocation)
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread("BusTracker")
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = packageName
            val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedFromNotification = intent!!.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)
        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }
        return Service.START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    fun removeLocationUpdates() {
        try {
            fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
            Common.setRequestingLocationUpdates(this, false)
            stopSelf()
        } catch (ex: SecurityException) {
            Common.setRequestingLocationUpdates(this, true)
            Log.e("removeLocationUpdates", "Lost location permission. Could not remove update. $ex")
        }
    }

    private fun getLastLocation() {
        try {
            fusedLocationProviderClient!!.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null)
                        mLocation = task.result
                    else
                        Log.e("getLastLocation", "Failed to get location")
                }
        } catch (ex: SecurityException) {
            Log.e("getLastLocation", "" + ex.message)
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest!!.interval = UPDATE_INTERVAL_IN_MIL
        locationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MIL
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun onNewLocation(lastLocation: Location?) {
        mLocation = lastLocation!!
        EventBus.getDefault().postSticky(BackgroundLocation(mLocation!!))
        if (serviceIsRunningInForeground(this))
            mNotificationManager!!.notify(NOTIFICATION_ID, notification)
    }

    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (javaClass.name.equals(service.service.className)) {
                if (service.foreground)
                    return true
            }
        }
        return false
    }

    override fun onBind(intent: Intent?): IBinder? {
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!mChangingConfiguration && Common.requestingLocationUpdates(this))
            startForeground(NOTIFICATION_ID, notification)
        return true
    }

    override fun onDestroy() {
        mServiceHandler!!.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    fun requestLocationUpdates() {
        Common.setRequestingLocationUpdates(this, true)
        startService(Intent(applicationContext, LocationService::class.java))
        try {
            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper())
        } catch (ex: SecurityException) {
            Common.setRequestingLocationUpdates(this, false)
            Log.e("requestLocationUpdates", "Lost location permission. $ex")
        }
    }

    //    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
//    private lateinit var locationRequest: LocationRequest
//    private lateinit var locationCallback: LocationCallback
//    private lateinit var currentLocation: Location
//
//    @SuppressLint("MissingPermission")
//    private fun initializeLocationComponents() {
//        // Initialize the FusedLocationProviderClient.
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
//
//        // Initialize the LocationCallback to handle
//        // location updates based on given interval.
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                locationResult ?: return // if null, exit
//                currentLocation = locationResult.lastLocation
//                sendMessageToActivity(currentLocation)
//                Log.i(Constants.TRACKINGSERVICE, "Your location is: Latitude: ${currentLocation.latitude} Longitude: ${currentLocation.longitude}")
//            }
//        }
//
//        locationRequest = LocationRequest.create()
//        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        locationRequest.interval = 2000
//        locationRequest.fastestInterval = 2000
//        locationRequest.numUpdates = Integer.MAX_VALUE
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
//    }
//
//    private fun sendMessageToActivity(currentLocation: Location) {
//        val intent = Intent(Constants.SERVICE_KEY)
//        intent.putExtra("data", currentLocation)
//        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun startLocationService() {
//        val channelId = "location_notification_channel"
//        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val resultIntent = Intent()
//        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//        val builder = NotificationCompat.Builder(applicationContext, channelId)
//        builder.setSmallIcon(R.drawable.googleg_disabled_color_18)
//        builder.setContentTitle("Title")
//        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
//        builder.setContentText("Running...")
//        builder.setContentIntent(pendingIntent)
//        builder.setAutoCancel(false)
//        builder.priority = NotificationCompat.PRIORITY_MIN
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (notificationManager.getNotificationChannel(channelId) == null) {
//                val notificationChannel = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_HIGH)
//                notificationChannel.description = "This channel is used by Location Service"
//                notificationManager.createNotificationChannel(notificationChannel)
//            }
//        }
//
//        initializeLocationComponents()
//
//        Log.i("***************", "startLocationService: getting last location")
//        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                Log.i("***************", "startLocationService: got last location: $location")
//            }
//        }
//
////        startForeground(Constants.LOCATION_SERVICE_ID, builder.build())
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            startForeground(Constants.LOCATION_SERVICE_ID, builder.build(), FOREGROUND_SERVICE_TYPE_LOCATION);
//        } else {
//            startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
//        }
//    }
//
//    private fun stopLocationService() {
//        LocationServices.getFusedLocationProviderClient(this)
//            .removeLocationUpdates(locationCallback)
//        stopForeground(true)
//        stopSelf()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        if (intent != null) {
//            val action = intent.action
//            if (action != null) {
//                if (action == "START") {
//                    startLocationService()
//                } else if (action == "STOP") {
//                    stopLocationService()
//                }
//            }
//        }
//        return super.onStartCommand(intent, flags, startId)
//    }
//
}