package com.example.intelligentbustracker.util

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.view.Window
import android.widget.RelativeLayout
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.LeavingHour
import com.example.intelligentbustracker.model.LeavingHours
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.model.Station
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.google.maps.model.DirectionsLeg
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.Duration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GeneralUtils {

    companion object {

        private const val NOT_FOUND = "NOT FOUND"
        private const val EARTH_RADIUS_M: Double = 6371.0 * 1000
        private const val PATTERN_TIME = "HH:mm"

        fun getStationFromName(stationName: String): Station? {
            return BusTrackerApplication.stations.firstOrNull { x -> x.name == stationName }
        }

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

        /**
         * Calculates the distance to each station given from the current LatLng
         * with a possible max value set in parameter 'maxDistance'.
         */
        private fun calculateDistanceToStationsWithMaxDistance(currentLatLng: LatLng, stations: List<Station>, maxDistance: Double): Map<Station, Double> {
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

        /**
         * Returns the closest Station object to
         * the given LatLng location parameter.
         */
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

        /**
         * Calculates the distance between two points
         * based on the Haversine formula.
         */
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

            // calculate the result
            return c * EARTH_RADIUS_M
        }

        fun addPolyline(results: DirectionsResult, mMap: GoogleMap): Polyline {
            val decodedPath: List<LatLng> = PolyUtil.decode(results.routes[0].overviewPolyline.encodedPath)
            return mMap.addPolyline(PolylineOptions().addAll(decodedPath))
        }

        fun getDurationForRoute(origin: LatLng, destination: LatLng, context: Context): String {
            // We need a context to access the API
            val geoApiContext: GeoApiContext = GeoApiContext.Builder()
                .apiKey(context.getString(R.string.google_maps_key))
                .build()

            // Perform the actual request
            val directionsResult: DirectionsResult = DirectionsApi.newRequest(geoApiContext)
                .mode(com.google.maps.model.TravelMode.DRIVING)
                .origin(origin.toString())
                .destination(destination.toString())
                .await()

            // Parse the result
            val route: DirectionsRoute = directionsResult.routes[0]
            val leg: DirectionsLeg = route.legs[0]
            val duration: Duration = leg.duration
            return duration.humanReadable
        }

        fun getEarliestNLeaveTimesForBusTowardsStation(busNumber: Int, stationName: String, numberOfLeaveTimes: Int): List<LeavingHour> {
            val bus = BusTrackerApplication.buses.firstOrNull { x -> x.number == busNumber }
            val leaveTimes = ArrayList<LeavingHour>()
            bus?.let {
                var currentTime: String = getHourAndMinuteString()
                if (it.scheduleRoutes.scheduleRoute1.contains(stationName) && it.scheduleRoutes.scheduleRoute1.indexOf(stationName) > 0) {
                    val matchingSchedule = BusTrackerApplication.schedules.firstOrNull { x -> x.busNumber == it.number }
                    matchingSchedule?.let { schedule ->
                        repeat(numberOfLeaveTimes) {
                            val leavingHour: String = returnEarliestLeavingHourForSchedule(schedule.leavingHours1, currentTime)
                            currentTime = leavingHour
                            leaveTimes.add(LeavingHour(schedule.leavingHours1.fromStation, leavingHour))
                        }
                    }
                } else if (it.scheduleRoutes.scheduleRoute2.contains(stationName) && it.scheduleRoutes.scheduleRoute2.indexOf(stationName) > 0) {
                    val matchingSchedule = BusTrackerApplication.schedules.firstOrNull { x -> x.busNumber == it.number }
                    matchingSchedule?.let { schedule ->
                        repeat(numberOfLeaveTimes) {
                            val leavingHour: String = returnEarliestLeavingHourForSchedule(schedule.leavingHours2, currentTime)
                            currentTime = leavingHour
                            leaveTimes.add(LeavingHour(schedule.leavingHours2.fromStation, leavingHour))
                        }
                    }
                }
                return leaveTimes
            }
            return leaveTimes
        }

        fun getEarliestLeaveTimeForBusTowardsStation(busNumber: Int, stationName: String): LeavingHour? {
            val bus = BusTrackerApplication.buses.firstOrNull { x -> x.number == busNumber }
            bus?.let {
                val currentTime: String = getHourAndMinuteString()
                if (it.scheduleRoutes.scheduleRoute1.contains(stationName) && it.scheduleRoutes.scheduleRoute1.indexOf(stationName) > 0) {
                    val matchingSchedule = BusTrackerApplication.schedules.firstOrNull { x -> x.busNumber == it.number }
                    matchingSchedule?.let { schedule ->
                        val leavingHour: String = returnEarliestLeavingHourForSchedule(schedule.leavingHours1, currentTime)
                        return LeavingHour(schedule.leavingHours1.fromStation, leavingHour)
                    }
                } else if (it.scheduleRoutes.scheduleRoute2.contains(stationName) && it.scheduleRoutes.scheduleRoute2.indexOf(stationName) > 0) {
                    val matchingSchedule = BusTrackerApplication.schedules.firstOrNull { x -> x.busNumber == it.number }
                    matchingSchedule?.let { schedule ->
                        val leavingHour: String = returnEarliestLeavingHourForSchedule(schedule.leavingHours2, currentTime)
                        return LeavingHour(schedule.leavingHours2.fromStation, leavingHour)
                    }
                }
                return null
            }
            return null
        }

        /**
         * Return the earliest leaving hour from the getEarliestLeavingHour method
         * @see getEarliestLeavingHour
         * based on the type of the current day. If not found then returns
         * the first leaving hour of tomorrow based on the type of the day it'll be.
         */
        private fun returnEarliestLeavingHourForSchedule(leavingHours: LeavingHours, currentTime: String): String {
            val time: String
            when (getTodayDayType()) {
                Calendar.SATURDAY -> {
                    time = getEarliestLeavingHour(leavingHours.saturdayLeavingHours, currentTime)
                    return if (time == NOT_FOUND) {
                        leavingHours.sundayLeavingHours[0]
                    } else {
                        time
                    }
                }
                Calendar.SUNDAY -> {
                    time = getEarliestLeavingHour(leavingHours.sundayLeavingHours, currentTime)
                    return if (time == NOT_FOUND) {
                        leavingHours.weekdayLeavingHours[0]
                    } else {
                        time
                    }
                }
                else -> {
                    time = getEarliestLeavingHour(leavingHours.weekdayLeavingHours, currentTime)
                    return if (time == NOT_FOUND) {
                        // check if tomorrow is Saturday
                        if (getTomorrowDayType() == Calendar.SATURDAY) {
                            leavingHours.saturdayLeavingHours[0]
                        } else {
                            leavingHours.weekdayLeavingHours[0]
                        }
                    } else {
                        time
                    }
                }
            }
        }

        /**
         * Converts default speed (meters/second)
         * to kilometers/hour.
         */
        fun getSpeedkmph(speedmps: Float): Float {
            return speedmps.times(3.6F)
        }

        /**
         * Returns the earliest leaving hour based on a list containing the hours
         * and minutes and the current time. If no match then return "NOT_FOUND".
         */
        private fun getEarliestLeavingHour(leavingHours: List<String>, currentTime: String): String {
            for (leavingHour in leavingHours) {
                if (currentTime < leavingHour) {
                    return leavingHour
                }
            }
            return NOT_FOUND
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

        /**
         * Returns a list of Bus objects containing any of
         * the stations listed in the 'stations' parameter.
         */
        fun getBusesWithGivenStations(stations: List<Station>): List<Bus> {
            val listOfBusesWithGivenStation = ArrayList<Bus>()
            for (bus in BusTrackerApplication.buses) {
                if (stations.any { x -> bus.containsStation(x.name) }) {
                    listOfBusesWithGivenStation.add(bus)
                }
            }
            return listOfBusesWithGivenStation
        }

        /**
         * Returns a list of Bus objects containing
         * the station passed in the 'station' parameter.
         */
        fun getBusesWithGivenStation(station: Station): List<Bus> {
            val listOfBusesWithGivenStation = ArrayList<Bus>()
            for (bus in BusTrackerApplication.buses) {
                if (bus.containsStation(station.name)) {
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
                        Manifest.permission.FOREGROUND_SERVICE,
                        // Activity recognition check
                        Manifest.permission.ACTIVITY_RECOGNITION
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

        /**
         * Returns today's DAY_OF_WEEK representation.
         */
        fun getTodayDayType(): Int {
            val calendar: Calendar = Calendar.getInstance()
            return calendar.get(Calendar.DAY_OF_WEEK)
        }

        /**
         * Returns tomorrow's DAY_OF_WEEK representation.
         */
        private fun getTomorrowDayType(): Int {
            val calendar: Calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            return calendar.get(Calendar.DAY_OF_WEEK)
        }

        /**
         * Returns the current time
         * in 24 hour format.
         */
        private fun getHourAndMinuteString(): String {
            val simpleDateFormat = SimpleDateFormat(PATTERN_TIME, Locale.getDefault())
            return simpleDateFormat.format(Date())
        }

        /**
         * Creates and returns a list of Station
         * objects from given list of their names.
         */
        fun generateStationListFromStationNames(stations: List<Station>, stationNames: List<String>): List<Station> {
            val generatedStations = ArrayList<Station>()
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

        /**
         * Returns the schedule for a bus with
         * it's number passed as parameter.
         */
        fun getScheduleFromBusNumber(busNumber: Int, schedules: List<Schedule>): Schedule? {
            return schedules.firstOrNull { x -> x.busNumber == busNumber }
        }
    }
}