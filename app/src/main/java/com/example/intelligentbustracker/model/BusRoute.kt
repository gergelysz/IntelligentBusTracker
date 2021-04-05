package com.example.intelligentbustracker.model

import com.google.android.gms.maps.model.LatLng

class BusRoute(val busNumber: Int, val routePoints1: List<LatLng>, val routePoints2: List<LatLng>)