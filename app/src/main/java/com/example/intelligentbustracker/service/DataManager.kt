package com.example.intelligentbustracker.service

import android.content.Context
import android.util.Log
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.Station
import java.io.BufferedReader
import java.io.InputStreamReader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

class DataManager(private val context: Context) {

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

}