package com.example.intelligentbustracker

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.intelligentbustracker.location.BackgroundLocation
import com.example.intelligentbustracker.service.LocationService
import com.example.intelligentbustracker.util.Common
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.*
import kotlinx.android.synthetic.main.activity_maps.remove_location_updates_button
import kotlinx.android.synthetic.main.activity_maps.request_location_updates_button
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mMap: GoogleMap

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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBackgroundLocationRetrieve(event: BackgroundLocation) {
        if (event.location != null)
            Toast.makeText(this, Common.getLocationText(event.location), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("***************", "onCreate Maps: asd")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this@MapsActivity)

        Dexter.withActivity(this)
            .withPermissions(
                Arrays.asList(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    android.Manifest.permission.FOREGROUND_SERVICE
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
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        if (p1.equals(Common.KEY_REQUEST_LOCATION_UPDATE))
            setButtonState(p0!!.getBoolean(Common.KEY_REQUEST_LOCATION_UPDATE, false))
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

    private fun isLocationServiceRunning(): Boolean {
        val activityManager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager != null) {
            for (runningService in activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService::class.simpleName.equals(runningService.service.className)) {
                    if (runningService.foreground) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val intent = Intent(applicationContext, LocationService::class.java)
            intent.action = "START"
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "Location Service started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationService() {
        if (isLocationServiceRunning()) {
            val intent = Intent(applicationContext, LocationService::class.java)
            intent.action = "STOP"
            startService(intent)
            Toast.makeText(this, "Location Service stopped", Toast.LENGTH_SHORT).show()
        }
    }

//    override fun onStart() {
//        super.onStart()
//        when {
//            PermissionUtils.isAccessFineLocationGranted(this) -> {
//                when {
//                    PermissionUtils.isLocationEnabled(this) -> {
//                        setUpLocationListener()
//                    }
//                    else -> {
//                        PermissionUtils.showGPSNotEnabledDialog(this)
//                    }
//                }
//            }
//            else -> {
//                PermissionUtils.requestAccessFineLocationPermission(this, Constants.PERMISSION_ID)
//            }
//        }
//    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val locationMessage = intent.getParcelableExtra<Location>("data")
            Toast.makeText(this@MapsActivity, "Received location data from TrackingService: lat=${locationMessage.latitude}, lng=${locationMessage.longitude}", Toast.LENGTH_SHORT).show()

            if (mMap != null) {
                moveCamera(locationMessage)
            }
        }
    }

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
        mMap.setMaxZoomPreference(20F)
        mMap.setMinZoomPreference(15F)
        val tgMuresDefault = LatLng(46.539892, 24.558334)
        mMap.addMarker(MarkerOptions().position(tgMuresDefault).title("Marker in Marosvásárhely"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(tgMuresDefault))

    }


}