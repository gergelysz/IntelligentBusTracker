package com.example.intelligentbustracker.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.util.DataManager
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var deferredStations: Deferred<ArrayList<Station>>
    private lateinit var deferredBuses: Deferred<ArrayList<Bus>>
    private lateinit var deferredFocusOnCenter: Deferred<String>
    private lateinit var deferredUpdateInterval: Deferred<String>
    private lateinit var deferredUpdateAccuracy: Deferred<String>
    private lateinit var deferredMapTheme: Deferred<String>

    companion object {
        lateinit var stations: ArrayList<Station>
        lateinit var buses: ArrayList<Bus>
        lateinit var focusOnCenter: String
        lateinit var updateInterval: String
        lateinit var updateAccuracy: String
        lateinit var mapTheme: String
    }

    init {
        val dataManager = DataManager(this)
        GlobalScope.launch(Dispatchers.Main) {
            deferredStations = async(Dispatchers.Default) { dataManager.initializeStations() }
            deferredBuses = async(Dispatchers.Default) { dataManager.initializeBuses() }

            deferredFocusOnCenter = async(Dispatchers.Default) { dataManager.getSettingValueString(getString(R.string.key_focus_map_center_on_current_location)) }
            deferredUpdateInterval = async(Dispatchers.Default) { dataManager.getSettingValueString(getString(R.string.key_location_update_interval)) }
            deferredUpdateAccuracy = async(Dispatchers.Default) { dataManager.getSettingValueString(getString(R.string.key_location_update_accuracy)) }
            deferredMapTheme = async(Dispatchers.Default) { dataManager.getSettingValueString(getString(R.string.key_map_theme)) }

            stations = deferredStations.await()
            buses = deferredBuses.await()

            focusOnCenter = deferredFocusOnCenter.await()
            updateInterval = deferredUpdateInterval.await()
            updateAccuracy = deferredUpdateAccuracy.await()
            mapTheme = deferredMapTheme.await()

            Log.i("MainActivity", "read ${stations.size} stations and ${buses.size} buses")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        load_map_button.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        settings_button.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}