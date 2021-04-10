package com.example.intelligentbustracker.model

//class BusToStation(val busNumbers: List<Int>, val scheduleRoute: List<String>, val stationFrom: Station, val StationTo: Station)
class BusToStation(val busNumber: Int, val scheduleRoute: List<String>, val stationFrom: Station, val stationTo: Station, val direction: Direction)