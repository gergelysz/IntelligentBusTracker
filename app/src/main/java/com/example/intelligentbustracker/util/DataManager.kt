package com.example.intelligentbustracker.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.LeavingHours
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.ScheduleRoutes
import com.example.intelligentbustracker.model.Station
import java.io.BufferedReader
import java.io.InputStreamReader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

class DataManager(private val context: Context) {

    companion object {
        private const val TAG = "DataManager"
    }

    /**
     * Reads the contents of the 'stations.csv' file
     * in the 'raw' resource folder and returns the
     * data in an ArrayList<Station> format.
     */
    fun initializeStations(): List<Station> {
        Log.i(TAG, "initializeStationDataList: starting read")
        try {
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
            Log.i(TAG, "initializeStationDataList: done read")
            return stations
        } catch (ex: Exception) {
            Log.e(TAG, "initializeStations: exception occurred while reading from stations.csv, returning empty list. More details: ${ex.message}")
            return ArrayList()
        }
    }

    /**
     * Reads the contents of the 'buses.csv' file
     * in the 'raw' resource folder and returns the
     * data in an ArrayList<Bus> format.
     */
    fun initializeBuses(): List<Bus> {
        try {
            Log.i(TAG, "initializeBuses: starting read")
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
                buses.add(Bus(csvRecord[0].toInt(), ScheduleRoutes(scheduleRoute1, scheduleRoute2)))
            }
            Log.i(TAG, "initializeBuses: done read")
            return buses
        } catch (ex: Exception) {
            Log.e(TAG, "initializeBuses: exception occurred while reading from buses.csv, returning empty list. More details: ${ex.message}")
            return ArrayList()
        }
    }

    /**
     * Reads the contents of the 'schedule.csv' file
     * in the 'raw' resource folder and returns the
     * data in an ArrayList<Schedule> format.
     */
    fun initializeSchedules(): List<Schedule> {
        try {
            Log.i(TAG, "initializeSchedules: starting read")
            val inputStream = context.resources.openRawResource(R.raw.schedules)
            val schedules = ArrayList<Schedule>()
            val reader = BufferedReader(InputStreamReader(inputStream))
            val csvParser = CSVParser(
                reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
            )
            for (csvRecord in csvParser) {
                val scheduleRoute1 = ArrayList<String>()
                val scheduleRoute2 = ArrayList<String>()
                scheduleRoute1.addAll(csvRecord[2].split(';'))
                scheduleRoute2.addAll(csvRecord[2].split(';'))
                schedules.add(
                    Schedule(
                        csvRecord[0].toInt(),
                        LeavingHours(
                            csvRecord[1],
                            csvRecord[2].split(';') as ArrayList<String>,
                            csvRecord[3].split(';') as ArrayList<String>,
                            csvRecord[4].split(';') as ArrayList<String>
                        ),
                        LeavingHours(
                            csvRecord[5],
                            csvRecord[6].split(';') as ArrayList<String>,
                            csvRecord[7].split(';') as ArrayList<String>,
                            csvRecord[8].split(';') as ArrayList<String>
                        )
                    )
                )
            }
            Log.i(TAG, "initializeSchedules: done read")
            return schedules
        } catch (ex: Exception) {
            Log.e(TAG, "initializeSchedules: exception occurred while reading from schedules.csv, returning empty list. More details: ${ex.message}")
            return ArrayList()
        }
    }

    /**
     * Retrieves the value of the given key saved in the
     * preferences. Otherwise returns the default value.
     */
    fun getSettingValueString(key: String): String {
        Log.i(TAG, "getSettingValueString: reading value for $key")
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
                "key_intelligent_bus_track" -> {
                    sh.getBoolean(key, true).toString()
                }
                else -> {
                    ""
                }
            }
        Log.i(TAG, "getSettingValueString: returning value $value for $key")
        return value
    }
}