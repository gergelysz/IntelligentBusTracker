package com.example.intelligentbustracker.util

import android.util.Log
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.model.BusToStation
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.Station
import com.google.android.gms.maps.model.LatLng

class RoutingUtil {

    // TODO: Remove duplicates

    companion object {
        private const val TAG = "RoutingUtil"

        fun initializeManualRouting(currentPosition: LatLng, destinationPosition: LatLng): List<BusToStation> {
            val closestStationsToCurrentPosition = GeneralUtils.getNumberOfClosestStationsFromListOfStations(currentPosition, BusTrackerApplication.stations, 3, 1000.0)
            val closestStationsToDestinationPosition = GeneralUtils.getNumberOfClosestStationsFromListOfStations(destinationPosition, BusTrackerApplication.stations, 3, 1000.0)

            val resultingBuses = getRoutesFromStationsToStations(closestStationsToCurrentPosition, closestStationsToDestinationPosition, 3)
            return resultingBuses
        }

        private fun getRoutesFromStationsToStations(closestStationsToCurrentPosition: List<Station>, closestStationsToDestinationPosition: List<Station>, numberOfSuggestedBuses: Int): List<BusToStation> {
            val busesFromStationToStation = ArrayList<BusToStation>()
            for (stationFrom in closestStationsToCurrentPosition) {
                for (stationTo in closestStationsToDestinationPosition) {
                    val buses = getDirectBusesFromStationToStation(stationFrom, stationTo)
                    busesFromStationToStation.addAll(buses.filter { x -> !busesFromStationToStation.stream().anyMatch { y -> y.busNumber == x.busNumber } })
                    if (busesFromStationToStation.size >= numberOfSuggestedBuses) {
                        return busesFromStationToStation
                    }
                }
            }
            return busesFromStationToStation
        }

        /**
         * Returns list of buses containing
         * the fromStation and toStation.
         */
        private fun getDirectBusesFromStationToStation(fromStation: Station, toStation: Station): List<BusToStation> {
            val listOfBusesWithGivenStation = ArrayList<BusToStation>()
            for (bus in BusTrackerApplication.buses) {
                val direction = bus.scheduleRoutes.getStationToStationDirection(fromStation.name, toStation.name)
                direction?.let {
                    if (it == Direction.DIRECTION_1) {
                        listOfBusesWithGivenStation.add(BusToStation(bus.number, bus.scheduleRoutes.scheduleRoute1, fromStation, toStation, it))
                    } else {
                        listOfBusesWithGivenStation.add(BusToStation(bus.number, bus.scheduleRoutes.scheduleRoute2, fromStation, toStation, it))
                    }
                } ?: run {
                    Log.e(TAG, "getBusesWithStationToStation: bus ${bus.number} doesn't have a direct transit from $fromStation to $toStation")
                }
            }
            return listOfBusesWithGivenStation
        }
    }
}