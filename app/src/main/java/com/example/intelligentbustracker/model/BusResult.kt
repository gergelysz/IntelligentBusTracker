package com.example.intelligentbustracker.model

import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.util.GeneralUtils
import com.google.android.gms.maps.model.LatLng
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import kotlin.math.roundToInt

class BusResult(val bus: Bus, val direction: Direction, val stationUp: String, val stationDown: String, val positionFrom: LatLng, val positionTo: LatLng) {

    private fun getDifferenceBetweenHours(time1: String, time2: String): Int {
//        val fractions1 = hour1.split(":").toTypedArray()
//        val fractions2 = hour2.split(":").toTypedArray()
//        val hours1 = fractions1[0].toInt()
//        val hours2 = fractions2[0].toInt()
//        val minutes1 = fractions1[1].toInt()
//        val minutes2 = fractions2[1].toInt()
//        var hourDiff = hours1 - hours2
//        var minutesDiff = minutes1 - minutes2
//        if (minutesDiff < 0) {
//            minutesDiff += 60
//            hourDiff--
//        }
//        if (hourDiff < 0) {
//            hourDiff += 24
//        }
//        return minutesDiff + (hourDiff / 60)
        val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val t1: LocalTime = LocalTime.parse(time1, fmt)
        val t2: LocalTime = LocalTime.parse(time2, fmt)
        val minutes: Long = ChronoUnit.MINUTES.between(t1, t2)
        return minutes.toInt()
    }

    private fun processTime(leavingHours: LeavingHours): Int {
        val currentTime = GeneralUtils.getHourAndMinuteString()
        val leavingHour: String = GeneralUtils.returnEarliestLeavingHourForSchedule(leavingHours, currentTime)
        return getDifferenceBetweenHours(currentTime, leavingHour)
    }

    fun getDurationToStationForHour(station: Station): String {
        var duration = 0
        val scheduleForBus = BusTrackerApplication.schedules.first { x -> x.busNumber == bus.number }
        if (direction == Direction.DIRECTION_1) {
            val firstStation = GeneralUtils.getStationFromName(stationUp)
            val distance = GeneralUtils.getDistance(positionFrom, LatLng(firstStation!!.latitude, firstStation.longitude))
            // time difference
            duration += processTime(scheduleForBus.leavingHours1)
            // 8.33 = 30 km/h -> m/s
            duration += (distance * 8.33 / 60).toInt()
            for (stationName in bus.scheduleRoutes.scheduleRoute1) {
                if (stationName == station.name) {
                    break
                }
                duration += 1
            }
        } else {
            val firstStation = GeneralUtils.getStationFromName(stationUp)
            val distance = GeneralUtils.getDistance(positionFrom, LatLng(firstStation!!.latitude, firstStation.longitude))
            // time difference
            duration += processTime(scheduleForBus.leavingHours2)
            // 8.33 = 30 km/h -> m/s
            duration += (distance * 8.33 / 60).toInt()
            for (stationName in bus.scheduleRoutes.scheduleRoute2) {
                if (stationName == station.name) {
                    break
                }
                duration += 1
            }
        }
        return duration.toString()
    }

    // TODO: Finalize
    fun getDurationToStation(station: Station): String {
        var duration = 0
        if (direction == Direction.DIRECTION_1) {
            val firstStation = GeneralUtils.getStationFromName(stationUp)
            val distance = GeneralUtils.getDistance(positionFrom, LatLng(firstStation!!.latitude, firstStation.longitude))
            // 8.33 = 30 km/h -> m/s
            duration += distance.roundToInt() * 300 / 36
            // convert to minutes
            duration /= 60
            for (stationName in bus.scheduleRoutes.scheduleRoute1) {
                if (stationName == station.name) {
                    break
                }
                duration += 1
            }
        } else {
            val firstStation = GeneralUtils.getStationFromName(stationUp)
            val distance = GeneralUtils.getDistance(positionFrom, LatLng(firstStation!!.latitude, firstStation.longitude))
            // 8.33 = 30 km/h -> m/s
            duration += distance.roundToInt() * 300 / 36
            // convert to minutes
            duration /= 60
            for (stationName in bus.scheduleRoutes.scheduleRoute2) {
                if (stationName == station.name) {
                    break
                }
                duration += 1
            }
        }
        return duration.toString()
    }

    fun getStationsFromInterval(): List<Station> {
        val stations: MutableList<Station> = if (direction == Direction.DIRECTION_1) {
            val indexOfFirstStation = bus.scheduleRoutes.scheduleRoute1.indexOf(stationUp)
            val indexOfLastStation = bus.scheduleRoutes.scheduleRoute1.indexOf(stationDown)

            bus.scheduleRoutes.scheduleRoute1.stream()
                .filter { x ->
                    bus.scheduleRoutes.scheduleRoute1.indexOf(x) in indexOfFirstStation..indexOfLastStation
                }
                .map { x -> GeneralUtils.getStationFromName(x) }
                .collect(Collectors.toList())
        } else {
            val indexOfFirstStation = bus.scheduleRoutes.scheduleRoute2.indexOf(stationUp)
            val indexOfLastStation = bus.scheduleRoutes.scheduleRoute2.indexOf(stationDown)

            bus.scheduleRoutes.scheduleRoute2.stream()
                .filter { x ->
                    bus.scheduleRoutes.scheduleRoute2.indexOf(x) in indexOfFirstStation..indexOfLastStation
                }
                .map { x -> GeneralUtils.getStationFromName(x) }
                .collect(Collectors.toList())
        }
        return stations
    }
}