package com.example.intelligentbustracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.intelligentbustracker.util.Constants

class MainActivity : AppCompatActivity() {

    lateinit var loadMapButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("***************", "onCreate Main: asd")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadMapButton = findViewById(R.id.loadMap)

        loadMapButton.setOnClickListener {
//            if (checkPermission()) {
//                if (isLocationEnabled()) {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
//                } else {
//                    Toast.makeText(this, "Please enable Location Services", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//////                requestPermission()
//                requestLocationPermission()
//            }
        }
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        // this function checks the permission result
//        if (requestCode == Constants.PERMISSION_ID) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.i(Constants.MAPSACTIVITY, "You have permission")
//            } else {
//                Log.w(Constants.MAPSACTIVITY, "You don't have permission")
//            }
//        }
//    }

    /**
     * Check for permission
     */
    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    /**
     * Request permission
     */
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), Constants.PERMISSION_ID)
    }

    /**
     * Check if location is enabled
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestLocationPermission() {
        val foreground = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        if (foreground) {
            if (runningQOrLater) {
                val background = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (background) {
                    handleLocationUpdates()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), Constants.PERMISSION_ID)
                }
            }
        } else {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), Constants.PERMISSION_ID)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), Constants.PERMISSION_ID)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PERMISSION_ID) {
            var foreground = false
            var background = false
            for (i in permissions.indices) {
                if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION, ignoreCase = true)) {
                    //foreground permission allowed
                    if (grantResults[i] >= 0) {
                        foreground = true
                        Toast.makeText(applicationContext, "Foreground location permission allowed", Toast.LENGTH_SHORT).show()
                        continue
                    } else {
                        Toast.makeText(applicationContext, "Location Permission denied", Toast.LENGTH_SHORT).show()
                        break
                    }
                }
                if (permissions[i].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION, ignoreCase = true)) {
                    if (grantResults[i] >= 0) {
                        foreground = true
                        background = true
                        Toast.makeText(applicationContext, "Background location location permission allowed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Background location location permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (foreground) {
                if (background) {
                    handleLocationUpdates()
                } else {
                    handleForegroundLocationUpdates()
                }
            }
        }
    }

    private fun handleLocationUpdates() {
        //foreground and background
        Toast.makeText(applicationContext, "Start Foreground and Background Location Updates", Toast.LENGTH_SHORT).show()
    }

    private fun handleForegroundLocationUpdates() {
        //handleForeground Location Updates
        Toast.makeText(applicationContext, "Start foreground location updates", Toast.LENGTH_SHORT).show()
    }
}