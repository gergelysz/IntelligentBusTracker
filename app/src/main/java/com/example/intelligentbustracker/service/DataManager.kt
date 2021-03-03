package com.example.intelligentbustracker.service

import com.example.intelligentbustracker.model.Station

class DataManager {

    val stations = HashMap<String, Station>()

    init {
        initializeStations()
    }

    private fun initializeStations() {
        var station1 = Station("03213213", "A")
        var station2 = Station("03253213", "Asd")
        var station3 = Station("64321321", "GGG")
        var station4 = Station("22112453", "Wdwaf")
        var station5 = Station("63266678", "Ardhrdhrd")

        stations[station1.stationId] = station1
        stations[station2.stationId] = station2
        stations[station3.stationId] = station3
        stations[station4.stationId] = station4
        stations[station5.stationId] = station5
    }
}