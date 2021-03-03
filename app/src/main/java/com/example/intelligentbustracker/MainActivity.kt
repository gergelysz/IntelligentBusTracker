package com.example.intelligentbustracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.service.DataManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var loadMapButton: Button

    private lateinit var deferredStations: Deferred<ArrayList<Station>>
    private lateinit var deferredBuses: Deferred<ArrayList<Bus>>

    companion object {
        lateinit var stations: ArrayList<Station>
        lateinit var buses: ArrayList<Bus>
    }

    init {
        val dataManager = DataManager(this)
        GlobalScope.launch(Dispatchers.Main) {
            deferredStations = async(Dispatchers.IO) { dataManager.initializeStations() }
            deferredBuses = async(Dispatchers.IO) { dataManager.initializeBuses() }
            stations = deferredStations.await()
            buses = deferredBuses.await()
            Log.i("MainActivity", "read ${stations.size} stations and ${buses.size} buses")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadMapButton = findViewById(R.id.loadMap)

        loadMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}