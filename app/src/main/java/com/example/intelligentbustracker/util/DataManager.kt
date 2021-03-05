package com.example.intelligentbustracker.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.Station
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.InputStreamReader

class DataManager(private val context: Context) {

    /**
     * Reads the contents of the 'stations.csv' file
     * in the 'raw' resource folder and returns the
     * data in an ArrayList<Station> format.
     */
    fun initializeStations(): ArrayList<Station> {
        Log.i("DataManager", "initializeStations: starting read")
        val inputStream = context.resources.openRawResource(R.raw.stations)
        val stations = ArrayList<Station>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        val csvParser = CSVParser(
            reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
        )
        for (csvRecord in csvParser) {
            stations.add(Station(csvRecord[0], csvRecord[1].toDouble(), csvRecord[2].toDouble()))
        }
        Log.i("DataManager", "initializeStations: done read")
        return stations
    }

    /**
     * Reads the contents of the 'buses.csv' file
     * in the 'raw' resource folder and returns the
     * data in an ArrayList<Bus> format.
     */
    fun initializeBuses(): ArrayList<Bus> {
        Log.i("DataManager", "initializeBuses: starting read")
        val inputStream = context.resources.openRawResource(R.raw.buses)
        val buses = ArrayList<Bus>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        val csvParser = CSVParser(
            reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
        )
        for (csvRecord in csvParser) {
            val scheduleRoute1 = ArrayList<String>()
            val scheduleRoute2 = ArrayList<String>()
            scheduleRoute1.addAll(csvRecord[1].split(';'))
            scheduleRoute2.addAll(csvRecord[2].split(';'))
            buses.add(Bus(csvRecord[0], Schedule(scheduleRoute1, scheduleRoute2)))
        }
        Log.i("DataManager", "initializeBuses: done read")
        return buses
    }

    /**
     * Retrieves the value of the given key saved in the
     * preferences. Otherwise returns the default value.
     */
    fun getSettingValueString(key: String): String {
        Log.i("DataManager", "getSettingValueString: reading value for $key")
        val sh: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val value =
            when (key) {
                "key_location_update_interval" -> {
                    sh.getString(key, "5000") ?: "5000"
                }
                "key_location_update_accuracy" -> {
                    sh.getString(key, "100") ?: "100"
                }
                "key_focus_map_center_on_current_location" -> {
                    sh.getBoolean(key, false).toString()
                }
                "key_map_theme" -> {
                    sh.getString(key, "map_style_retro") ?: "map_style_retro"
                }
                else -> {
                    ""
                }
            }
        Log.i("DataManager", "getSettingValueString: returning value $value for $key")
        return value
    }
}