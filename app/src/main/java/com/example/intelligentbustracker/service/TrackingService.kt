package com.example.intelligentbustracker.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.intelligentbustracker.MainActivity
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class TrackingService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var currentLocation: Location

    val mHandler: Handler = Handler()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel();
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification: Notification = NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle("Foreground Service")
            .setContentText("content text")
            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        initializeLocationComponents()
        startLocationUpdates()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel("ForegroundServiceChannel", "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("***************", "onCreate Tracking: asd")
        showToast("Job Execution Started")
    }

    override fun onDestroy() {
//        super.onDestroy()
        Log.e("***************", "onDestroy Tracking: asd")
        showToast("Job Execution Destroyed")
    }

    fun showToast(text: CharSequence?) {
        mHandler.post { Toast.makeText(this@TrackingService, text, Toast.LENGTH_SHORT).show() }
    }

    private fun sendMessageToActivity(currentLocation: Location) {
        val intent = Intent(Constants.SERVICE_KEY)
        intent.putExtra("data", currentLocation)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun initializeLocationComponents() {
        // Initialize the FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the LocationCallback to handle
        // location updates based on given interval.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return // if null, exit
                currentLocation = locationResult.lastLocation
                sendMessageToActivity(currentLocation)
                Log.i(Constants.TRACKINGSERVICE, "Your location is: Latitude: ${currentLocation.latitude} Longitude: ${currentLocation.longitude}")
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 2000
            locationRequest.fastestInterval = 2000
            locationRequest.numUpdates = Integer.MAX_VALUE
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }
}