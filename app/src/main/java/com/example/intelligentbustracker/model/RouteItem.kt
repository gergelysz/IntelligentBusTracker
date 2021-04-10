package com.example.intelligentbustracker.model

class RouteItem(val stationStart: Station, val stationEnd: Station, val buses: List<Bus>, val walkFromStationToStation: List<RouteStationToStation>) {
}