package com.example.intelligentbustracker

import android.app.Application
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.util.DataManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

class BusTrackerApplication : Application() {

    companion object {
        private lateinit var instance: BusTrackerApplication

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
        var mapTheme: String = "map_style_retro"
            get() {
                if (field.isEmpty()) {
                    field = DataManager(getInstance()).getSettingValueString(getInstance().getString(R.string.key_map_theme))
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
            val deferredFocusOnCenter: Deferred<String> = async { dataManager.getSettingValueString(getString(R.string.key_focus_map_center_on_current_location)) }
            val deferredUpdateInterval: Deferred<String> = async { dataManager.getSettingValueString(getString(R.string.key_location_update_interval)) }
            val deferredUpdateAccuracy: Deferred<String> = async { dataManager.getSettingValueString(getString(R.string.key_location_update_accuracy)) }
            val deferredMapTheme: Deferred<String> = async { dataManager.getSettingValueString(getString(R.string.key_map_theme)) }

            focusOnCenter = try {
                deferredFocusOnCenter.await()
            } catch (ex: Exception) {
                ""
            }

            updateInterval = try {
                deferredUpdateInterval.await()
            } catch (ex: Exception) {
                ""
            }

            updateAccuracy = try {
                deferredUpdateAccuracy.await()
            } catch (ex: Exception) {
                ""
            }

            mapTheme = try {
                deferredMapTheme.await()
            } catch (ex: Exception) {
                ""
            }

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