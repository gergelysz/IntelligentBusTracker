package com.example.intelligentbustracker.util

import android.util.Log
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.BusResult
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.Station
import com.google.android.gms.maps.model.LatLng
import java.util.ArrayList

class RoutingUtil {

    companion object {
        private const val TAG = "RoutingUtil"

        fun initializeManualRouting(currentPosition: LatLng, destinationPosition: LatLng): List<List<BusResult>> {
//        fun initializeManualRouting(currentPosition: LatLng, destinationPosition: LatLng): List<BusToStation> {
            // TODO: Check their distance (check for buses if higher than 200m)
            val closestStationsToCurrentPosition = GeneralUtils.getNumberOfClosestStationsFromListOfStations(currentPosition, BusTrackerApplication.stations, 3, 1000.0)
            val closestStationsToDestinationPosition = GeneralUtils.getNumberOfClosestStationsFromListOfStations(destinationPosition, BusTrackerApplication.stations, 3, 1000.0)

            val busesWithStationsFrom = GeneralUtils.getBusesWithGivenStations(closestStationsToCurrentPosition)
            val busesWithStationsTo = GeneralUtils.getBusesWithGivenStations(closestStationsToDestinationPosition)

            if (closestStationsToCurrentPosition[0] == closestStationsToDestinationPosition[0]) {
                return listOf<ArrayList<BusResult>>()
            } else {
                val resultingBuses = getRoutesFromStationsToStations(closestStationsToCurrentPosition, closestStationsToDestinationPosition, 3, busesWithStationsFrom, busesWithStationsTo)
                return resultingBuses
            }
        }

        //        private fun getRoutesFromStationsToStations(closestStationsToCurrentPosition: List<Station>, closestStationsToDestinationPosition: List<Station>, numberOfSuggestedBuses: Int, listOfBuses: List<Bus>): List<BusToStation> {
        private fun getRoutesFromStationsToStations(
            closestStationsToCurrentPosition: List<Station>,
            closestStationsToDestinationPosition: List<Station>,
            numberOfSuggestedBuses: Int,
            busesWithStationsFrom: List<Bus>,
            busesWithStationsTo: List<Bus>
        ): List<List<BusResult>> {
            val busResults = arrayListOf<List<BusResult>>()
            for (stationFrom in closestStationsToCurrentPosition) {
                for (stationTo in closestStationsToDestinationPosition) {
                    val buses = getBusesFromStationToStation(stationFrom, stationTo, busesWithStationsFrom, busesWithStationsTo)
                    buses.forEach { bus -> busResults.add(bus) }
//                    buses.forEach { bus -> if (!busResultInBusResults(busResults, bus)) busResults.add(bus) }
                    if (busResults.size >= numberOfSuggestedBuses) {
                        return busResults
                    }
                }
            }
            return busResults
        }

        private fun busResultInBusResults(busResults: List<List<BusResult>>, busResult: List<BusResult>): Boolean {
            busResults.forEach { busResultItem ->
                if (busResultItem[0].bus.number == busResult[0].bus.number) {
                    return true
                }
            }
            return false
        }

        private fun busResultAlreadyExists(busResults: List<List<BusResult>>, busResult: List<BusResult>): Boolean {
            if (busResults.isEmpty() || busResult.isEmpty()) {
                return false
            }
            if (busResults[0][0].bus.number == busResult[0].bus.number) {
                return true
            }
//            for (busResultItem in busResults) {
//                if (busResultItem[0].bus.number == busResult[0].bus.number) {
//                    return true
//                }
//            }
            return false
        }

        /**
         * Returns list of buses containing the fromStation and toStation.
         */
        private fun getBusesFromStationToStation(fromStation: Station, toStation: Station, busesWithStationsFrom: List<Bus>, busesWithStationsTo: List<Bus>): List<List<BusResult>> {
            val listOfBusesWithGivenStation = arrayListOf<List<BusResult>>()
            for (bus in busesWithStationsFrom) {
                val direction = bus.scheduleRoutes.getStationToStationDirection(fromStation.name, toStation.name)
                direction?.let {
                    /** Direct bus */
                    listOfBusesWithGivenStation.add(listOf(BusResult(bus, it, fromStation.name, toStation.name)))
                } ?: run {
                    Log.e(TAG, "getBusesWithStationToStation: bus ${bus.number} doesn't have a direct transit from $fromStation to $toStation")
                    val changeBuses = getBusWithCommonStation(bus, fromStation, toStation, busesWithStationsTo.filter { x -> x.number != bus.number })
                    listOfBusesWithGivenStation.addAll(changeBuses)
//                    changeBuses?.let { listOfBusesWithGivenStation.add(it) }
                }
            }
            return listOfBusesWithGivenStation
        }

        /**
         * Returns a bus from a list of buses with a common
         * station with the passed currentBus parameter.
         */
        private fun getBusWithCommonStation(currentBus: Bus, fromStation: Station, toStation: Station, busesWithStationsTo: List<Bus>): List<List<BusResult>> {
//        private fun getBusWithCommonStation(currentBus: Bus, fromStation: Station, toStation: Station, busesWithStationsTo: List<Bus>): List<BusResult>? {
            val busesWithCommonStation = arrayListOf<List<BusResult>>()
            val directionCurrentBus = currentBus.getDirectionForStation(fromStation.name)
            directionCurrentBus?.let { currentBusDirection ->
                for (bus in busesWithStationsTo) {
                    val direction = bus.getDirectionForStation(toStation.name)
                    direction?.let {
                        val commonStation: String = if (it == Direction.DIRECTION_1) {
                            val stations = ArrayList(bus.scheduleRoutes.scheduleRoute1)
                            getLastCommonStation(currentBus, currentBusDirection, stations, toStation.name)
                        } else {
                            val stations = ArrayList(bus.scheduleRoutes.scheduleRoute2)
                            getLastCommonStation(currentBus, currentBusDirection, stations, toStation.name)
                        }
                        /** Found a common station */
                        if (commonStation.isNotEmpty() && commonStation != fromStation.name && commonStation != toStation.name) {
                            busesWithCommonStation.add(listOf(BusResult(currentBus, currentBusDirection, fromStation.name, commonStation), BusResult(bus, it, commonStation, toStation.name)))
//                        return listOf(BusResult(currentBus, directionCurrentBus, fromStation.name, commonStation), BusResult(bus, it, commonStation, toStation.name))
                        }
//                    else {
//                        return null
//                    }
                    }
                }
            }
            return busesWithCommonStation
        }

        /**
         * Returns the last common station for two given buses.
         */
        private fun getLastCommonStation(currentBus: Bus, currentBusDirection: Direction, scheduleRoute: ArrayList<String>, toStationName: String): String {
            var lastCommonStation = ""
            /** Remove last item */
            scheduleRoute.removeLast()
            for (scheduleStation in scheduleRoute) {
                if (currentBus.containsStationWithDirection(scheduleStation, currentBusDirection) && scheduleStation != toStationName) {
//                if (currentBus.containsStation(scheduleStation) && scheduleStation != toStationName) {
                    lastCommonStation = scheduleStation
                }
            }
            return lastCommonStation
        }
    }
}