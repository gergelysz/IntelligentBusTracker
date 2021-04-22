package com.example.intelligentbustracker.model

data class Bus(val number: String, val scheduleRoutes: ScheduleRoutes) {

    fun containsStation(stationName: String): Boolean {
        return scheduleRoutes.scheduleRoute1.contains(stationName) || scheduleRoutes.scheduleRoute2.contains(stationName)
    }

    fun getDirectionForFromToStation(stationNameFrom: String, stationNameTo: String): Direction? {
        if (scheduleRoutes.scheduleRoute1.contains(stationNameFrom) && scheduleRoutes.scheduleRoute1.contains(stationNameTo)) {
            return if (scheduleRoutes.scheduleRoute1.indexOf(stationNameFrom) < scheduleRoutes.scheduleRoute1.indexOf(stationNameTo)) {
                Direction.DIRECTION_1
            } else {
                null
            }
        } else if (scheduleRoutes.scheduleRoute2.contains(stationNameFrom) && scheduleRoutes.scheduleRoute2.contains(stationNameTo)) {
            return if (scheduleRoutes.scheduleRoute2.indexOf(stationNameFrom) < scheduleRoutes.scheduleRoute2.indexOf(stationNameTo)) {
                Direction.DIRECTION_2
            } else {
                null
            }
        } else {
            return null
        }
    }

    fun containsStationWithDirection(stationName: String, direction: Direction): Boolean {
        return if (direction == Direction.DIRECTION_1) {
            scheduleRoutes.scheduleRoute1.contains(stationName)
        } else {
            scheduleRoutes.scheduleRoute2.contains(stationName)
        }
    }

    /**
     * Returns direction from a given station.
     */
    fun getDirectionForFromStation(stationName: String): Direction? {
        if (scheduleRoutes.scheduleRoute1.contains(stationName) && scheduleRoutes.scheduleRoute1.last() != stationName) {
            return Direction.DIRECTION_1
        } else if (scheduleRoutes.scheduleRoute2.contains(stationName) && scheduleRoutes.scheduleRoute2.last() != stationName) {
            return Direction.DIRECTION_2
        }
        return null
    }

    /**
     * Returns direction towards a given station.
     */
    fun getDirectionForToStation(stationName: String): Direction? {
        if (scheduleRoutes.scheduleRoute1.contains(stationName) && scheduleRoutes.scheduleRoute1.first() != stationName) {
            return Direction.DIRECTION_1
        } else if (scheduleRoutes.scheduleRoute2.contains(stationName) && scheduleRoutes.scheduleRoute2.first() != stationName) {
            return Direction.DIRECTION_2
        }
        return null
    }
}