package com.example.intelligentbustracker

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.model.Status
import com.example.intelligentbustracker.util.DataManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

class BusTrackerApplication : Application() {

    companion object {
        private lateinit var instance: BusTrackerApplication
        private const val TAG = "BusTrackerApplication"

        fun getInstance(): BusTrackerApplication {
            return instance
        }

        private fun isInitialised() = ::instance.isInitialized

        /**
         * Stores the stations retrieved
         * from stations.csv
         */
        var stations: List<Station> = ArrayList()
            get() {
                if (field.isNullOrEmpty()) {
                    field = DataManager(getInstance()).initializeStations()
                }
                return field
            }

        /**
         * Stores the buses retrieved
         * from buses.csv
         */
        var buses: List<Bus> = ArrayList()
            get() {
                if (field.isNullOrEmpty()) {
                    field = DataManager(getInstance()).initializeBuses()
                }
                return field
            }

        /**
         * Stores the schedules retrieved
         * from schedules.csv
         */
        var schedules: List<Schedule> = ArrayList()
            get() {
                if (field.isNullOrEmpty()) {
                    field = DataManager(getInstance()).initializeSchedules()
                }
                return field
            }

        var focusOnCenter: String = "false"
            get() {
                if (field.isEmpty()) {
                    field = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_focus_map_center_on_current_location))
                }
                return field
            }
        var updateInterval: String = "5000"
            get() {
                if (field.isEmpty()) {
                    field = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_location_update_interval))
                }
                return field
            }
        var updateAccuracy: String = "100"
            get() {
                if (field.isEmpty()) {
                    field = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_location_update_accuracy))
                }
                return field
            }

        var mapTheme: MutableLiveData<String> = MutableLiveData("map_style_retro")
            get() {
                if (field.value!!.isEmpty()) {
                    field.value = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_map_theme))
                }
                return field
            }

        var intelligentTracker: MutableLiveData<String> = MutableLiveData("true")
            get() {
                if (field.value!!.isEmpty()) {
                    field.value = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_intelligent_bus_track))
                }
                return field
            }

        var status: MutableLiveData<Status> = MutableLiveData(Status.WAITING_FOR_BUS)

        var askLocationChange: String = "true"
            get() {
                if (field.isEmpty()) {
                    field = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_ask_location_change))
                }
                return field
            }
    }

    override fun onCreate() {
        super.onCreate()
        if (!isInitialised()) {
            instance = this
            readData()
        }
    }

    private fun readData() = runBlocking(Dispatchers.Default) {
        supervisorScope {
            val dataManager = DataManager(getInstance())
            val deferredStations: Deferred<List<Station>> = async { dataManager.initializeStations() }
            val deferredBuses: Deferred<List<Bus>> = async { dataManager.initializeBuses() }
            val deferredSchedules: Deferred<List<Schedule>> = async { dataManager.initializeSchedules() }

            focusOnCenter = dataManager.getSettingValueString(getString(R.string.key_focus_map_center_on_current_location))
            updateInterval = dataManager.getSettingValueString(getString(R.string.key_location_update_interval))
            updateAccuracy = dataManager.getSettingValueString(getString(R.string.key_location_update_accuracy))
            mapTheme.postValue(dataManager.getSettingValueString(getString(R.string.key_map_theme)))
            intelligentTracker.postValue(dataManager.getSettingValueString(getString(R.string.key_intelligent_bus_track)))
            askLocationChange = dataManager.getSettingValueString(getString(R.string.key_ask_location_change))

            stations = try {
                deferredStations.await()
            } catch (ex: Exception) {
                ArrayList()
            }

            buses = try {
                deferredBuses.await()
            } catch (ex: Exception) {
                ArrayList()
            }

            schedules = try {
                deferredSchedules.await()
            } catch (ex: Exception) {
                ArrayList()
            }
        }
    }
}