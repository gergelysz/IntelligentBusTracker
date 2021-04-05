package com.example.intelligentbustracker.model

data class Bus(val number: Int, val scheduleRoutes: ScheduleRoutes) {

    fun containsStation(stationName: String): Boolean {
        return (scheduleRoutes.scheduleRoute1.contains(stationName) && scheduleRoutes.scheduleRoute1.indexOf(stationName) > 0) ||
                (scheduleRoutes.scheduleRoute2.contains(stationName) && scheduleRoutes.scheduleRoute2.indexOf(stationName) > 0)
    }

    fun getDirectionForStation(stationName: String): Direction? {
        if (scheduleRoutes.scheduleRoute1.contains(stationName) && scheduleRoutes.scheduleRoute1.indexOf(stationName) > 0) {
            return Direction.DIRECTION_1
        } else if (scheduleRoutes.scheduleRoute2.contains(stationName) && scheduleRoutes.scheduleRoute2.indexOf(stationName) > 0) {
            return Direction.DIRECTION_2
        }
        return null
    }
}