package com.example.intelligentbustracker.util

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.view.Window
import android.widget.RelativeLayout
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.LeavingHour
import com.example.intelligentbustracker.model.LeavingHours
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.Station
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GeneralUtils {

    companion object {

        /**
         * Returns a given number of stations with a given maximum
         * distance that are the closest to the given current location.
         */
        fun getNumberOfClosestStationsFromListOfStations(currentLatLng: LatLng, stations: List<Station>, numberOfStations: Int, maxDistance: Double): List<Station> {
            val stationsAndDistancesToThem = calculateDistanceToStationsWithMaxDistance(currentLatLng, stations, maxDistance)
                .toList().sortedBy { (_, value) -> value }.toMap()

            val numberOfElementsToReturn = stationsAndDistancesToThem.size
            return if (numberOfStations > numberOfElementsToReturn) {
                stationsAndDistancesToThem.keys.take(numberOfElementsToReturn)
            } else {
                stationsAndDistancesToThem.keys.take(numberOfStations)
            }
        }

        fun calculateDistanceToStationsWithMaxDistance(currentLatLng: LatLng, stations: List<Station>, maxDistance: Double): Map<Station, Double> {
            val map = HashMap<Station, Double>()
            for (station in stations) {
                val distanceHaversineLocation = getDistance(currentLatLng.latitude, currentLatLng.longitude, station.latitude, station.longitude)
                if (distanceHaversineLocation <= maxDistance) {
                    map[station] = distanceHaversineLocation
                }
            }
            return map
        }

        fun getClosestStationFromListOfStations(currentLatLng: LatLng, stations: List<Station>): Station {
            var smallestDistance: Double = getDistance(currentLatLng.latitude, currentLatLng.longitude, stations[0].latitude, stations[0].longitude)
            var closestStation: Station = stations[0]
            for (station in stations) {
                val distanceHaversineLocation = getDistance(currentLatLng.latitude, currentLatLng.longitude, station.latitude, station.longitude)
                if (distanceHaversineLocation < smallestDistance) {
                    smallestDistance = distanceHaversineLocation
                    closestStation = station
                }
            }
            return closestStation
        }

        fun getClosestStation(currentLatLng: LatLng): Station {
            var smallestDistance: Double = getDistance(currentLatLng.latitude, currentLatLng.longitude, BusTrackerApplication.stations[0].latitude, BusTrackerApplication.stations[0].longitude)
            var closestStation: Station = BusTrackerApplication.stations[0]
            for (station in BusTrackerApplication.stations) {
                val distanceHaversineLocation = getDistance(currentLatLng.latitude, currentLatLng.longitude, station.latitude, station.longitude)
                if (distanceHaversineLocation < smallestDistance) {
                    smallestDistance = distanceHaversineLocation
                    closestStation = station
                }
            }
            return closestStation
        }

        private fun getDistance(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
            // The math module contains a function
            // named toRadians which converts from
            // degrees to radians.
            val lat1 = Math.toRadians(latitude1)
            val lon1 = Math.toRadians(longitude1)
            val lat2 = Math.toRadians(latitude2)
            val lon2 = Math.toRadians(longitude2)

            // Haversine formula
            val dlon = lon2 - lon1
            val dlat = lat2 - lat1
            val a = (sin(dlat / 2).pow(2.0) + (cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2.0)))
            val c = 2 * asin(sqrt(a))

            // Radius of earth in kilometers.
            // Use 3956 for miles
            val r = 6371.0 * 1000

            // calculate the result
            return c * r
        }

        fun getEarliestLeaveTimeForBusTowardsStation(bus: Bus, stationName: String): LeavingHour? {
            val currentTime: String = getHourAndMinuteString()
            if (bus.scheduleRoutes.scheduleRoute1.contains(stationName) && bus.scheduleRoutes.scheduleRoute1.indexOf(stationName) > 0) {
                val matchingSchedule = BusTrackerApplication.schedules.firstOrNull { x -> x.busNumber == bus.number }
                matchingSchedule?.let { schedule ->
                    val leavingHour: String = returnEarliestLeavingHourForSchedule(schedule.leavingHours1, currentTime)
                    return LeavingHour(schedule.leavingHours1.fromStation, leavingHour)
                }
            } else if (bus.scheduleRoutes.scheduleRoute2.contains(stationName) && bus.scheduleRoutes.scheduleRoute2.indexOf(stationName) > 0) {
                val matchingSchedule = BusTrackerApplication.schedules.firstOrNull { x -> x.busNumber == bus.number }
                matchingSchedule?.let { schedule ->
                    val leavingHour: String = returnEarliestLeavingHourForSchedule(schedule.leavingHours2, currentTime)
                    return LeavingHour(schedule.leavingHours2.fromStation, leavingHour)
                }
            }
            return null
        }

        private fun returnEarliestLeavingHourForSchedule(leavingHours: LeavingHours, currentTime: String): String {
            return when (getDay()) {
                Calendar.SATURDAY -> {
                    getEarliestLeavingHour(leavingHours.saturdayLeavingHours, currentTime)
                }
                Calendar.SUNDAY -> {
                    getEarliestLeavingHour(leavingHours.sundayLeavingHours, currentTime)
                }
                else -> {
                    getEarliestLeavingHour(leavingHours.weekdayLeavingHours, currentTime)
                }
            }
        }

        private fun getEarliestLeavingHour(leavingHours: List<String>, currentTime: String): String {
            for (leavingHour in leavingHours) {
                if (currentTime < leavingHour) {
                    return leavingHour
                }
            }
            return "Couldn't find closest schedule time"
        }

        fun getBusesWithDirectionForStation(stationName: String, buses: List<Bus>): Map<Bus, Direction> {
            val listOfBusesWithGivenStation: MutableMap<Bus, Direction> = HashMap()
            for (bus in buses) {
                val direction: Direction? = bus.getDirectionForStation(stationName)
                direction?.let {
                    listOfBusesWithGivenStation.put(bus, direction)
                }
            }
            return listOfBusesWithGivenStation
        }

        // TODO: Finish
        fun getBusesWithDirectionForStations(stations: List<Station>, buses: List<Bus>): Map<Bus, Direction> {
            val listOfBusesWithGivenStation: MutableMap<Bus, Direction> = HashMap()
            for (bus in buses) {
                for (station in stations) {
                    if (bus.containsStation(station.name)) {
                        //                val direction: Direction? = bus.getDirectionForStation(stationName)
//                direction?.let {
//                    listOfBusesWithGivenStation.put(bus, direction)
//                }
                    }
                }
            }
            return listOfBusesWithGivenStation
        }

        fun getBusesWithGivenStations(stations: List<Station>): List<Bus> {
            val listOfBusesWithGivenStation = ArrayList<Bus>()
            for (bus in BusTrackerApplication.buses) {
                if (stations.any { x -> bus.containsStation(x.name) }) {
                    listOfBusesWithGivenStation.add(bus)
                }
            }
            return listOfBusesWithGivenStation
        }

        fun getBusesWithGivenStation(stationName: String): List<Bus> {
            val listOfBusesWithGivenStation = ArrayList<Bus>()
            for (bus in BusTrackerApplication.buses) {
                if (bus.containsStation(stationName)) {
                    listOfBusesWithGivenStation.add(bus)
                }
            }
            return listOfBusesWithGivenStation
        }

        fun getBusNumbersWithGivenStation(stationName: String): IntArray {
            val listOfBusesWithGivenStation: MutableList<Int> = ArrayList()
            for (bus in BusTrackerApplication.buses) {
                if (bus.containsStation(stationName)) {
                    listOfBusesWithGivenStation.add(bus.number)
                }
            }
            return listOfBusesWithGivenStation.toIntArray()
        }


        fun populateListWithMatchingStations(stations: MutableList<Station>, scheduleRoute: ArrayList<String>) {
            for (station in BusTrackerApplication.stations) {
                if (scheduleRoute.contains(station.name)) {
                    stations.add(station)
                }
            }
        }

        fun buildFullscreenDialog(context: Context, backgroundColor: Int): Dialog {
            // the content
            val root = RelativeLayout(context)
            root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // creating the fullscreen dialog
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(root)
            dialog.window!!.setBackgroundDrawableResource(backgroundColor)
            dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return dialog
        }

        fun getPermissionList(): Collection<String> {
            val permissions: Collection<String>
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE
//                        ,
                        // Activity recognition check
//                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                } else {
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE
                    )
                }
            } else {
                permissions = listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            return permissions
        }

        fun getDay(): Int {
            val calendar: Calendar = Calendar.getInstance()
            return calendar.get(Calendar.DAY_OF_WEEK)
        }

        private fun getHourAndMinuteString(): String {
            val hourString = getHour().toString().let {
                if (it.length == 1) {
                    "0$it"
                } else {
                    it
                }
            }
            val minuteString: String = getMinute().toString().let {
                if (it.length == 1) {
                    "0$it"
                } else {
                    it
                }
            }
            return "$hourString:$minuteString"
        }

        private fun getHour(): Int {
            val calendar: Calendar = Calendar.getInstance()
            return calendar.get(Calendar.HOUR_OF_DAY)
        }

        private fun getMinute(): Int {
            val calendar: Calendar = Calendar.getInstance()
            return calendar.get(Calendar.MINUTE)
        }

        fun generateStationListFromStationNames(stations: List<Station>, stationNames: List<String>): List<Station> {
            val generatedStations: MutableList<Station> = ArrayList()
            for (stationName in stationNames) {
                val station = stations.firstOrNull { x -> x.name == stationName }
                station?.let {
                    generatedStations.add(it)
                }
            }
            return generatedStations
        }

        fun generateBusListFromBusNumbers(buses: List<Bus>, busNumbers: IntArray): List<Bus> {
            val generatedBuses: MutableList<Bus> = ArrayList()
            for (busNumber in busNumbers) {
                val bus = buses.firstOrNull { x -> x.number == busNumber }
                bus?.let {
                    generatedBuses.add(it)
                }
            }
            return generatedBuses
        }

        fun getScheduleFromBusNumber(busNumber: Int, schedules: List<Schedule>): Schedule? {
            return schedules.firstOrNull { x -> x.busNumber == busNumber }
        }
    }
}