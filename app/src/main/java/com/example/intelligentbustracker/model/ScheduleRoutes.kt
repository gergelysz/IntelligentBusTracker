package com.example.intelligentbustracker.model

data class ScheduleRoutes(val scheduleRoute1: ArrayList<String>, val scheduleRoute2: ArrayList<String>) {

    fun getStationToStationDirection(fromStation: String, toStation: String): Direction? {
        if (scheduleRoute1.contains(fromStation) && scheduleRoute1.contains(toStation) && scheduleRoute1.indexOf(fromStation) < scheduleRoute1.indexOf(toStation)) {
            return Direction.DIRECTION_1
        } else if (scheduleRoute2.contains(fromStation) && scheduleRoute2.contains(toStation) && scheduleRoute2.indexOf(fromStation) < scheduleRoute2.indexOf(toStation)) {
            return Direction.DIRECTION_2
        }
        return null
    }
}