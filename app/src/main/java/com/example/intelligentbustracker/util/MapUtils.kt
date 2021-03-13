package com.example.intelligentbustracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.directions.route.AbstractRouting
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Station
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapUtils {

    companion object {
        private const val TAG = "MapUtils"

        /**
         * Draws markers for stations on the map based on
         * their coordinates and adds their name as title.
         */
        @JvmStatic
        fun addStationMarkers(mMap: GoogleMap, stationsMarkers: List<MarkerOptions>) {
            Log.i(TAG, "addStationMarkers: adding ${stationsMarkers.size} markers")
            for (stationMarker in stationsMarkers) {
                mMap.addMarker(stationMarker)
            }
            Log.i(TAG, "addStationMarkers: added ${stationsMarkers.size} markers")
        }

        /**
         * Builds a list with the Markers for the stations
         * to be later added onto the map.
         */
        @JvmStatic
        fun createStationsMarkers(stations: List<Station>, context: Context): List<MarkerOptions> {
            Log.i(TAG, "createStationsMarkers: creating ${stations.size} markers")
            val markerId: Int = when (BusTrackerApplication.mapTheme) {
                "map_style_dark" -> {
                    R.drawable.ic_bus_station_white
                }
                "map_style_night" -> {
                    R.drawable.ic_bus_station_white
                }
                "map_style_standard" -> {
                    R.drawable.ic_bus_station_black
                }
                "map_style_retro" -> {
                    R.drawable.ic_bus_station_black
                }
                else -> R.drawable.ic_bus_station_white
            }
            val stationsMarkers = ArrayList<MarkerOptions>()
            for (station in stations) {
                stationsMarkers.add(
                    MarkerOptions()
                        .position(LatLng(station.latitude, station.longitude))
                        .title(station.name)
                        .icon(bitmapFromVector(context, markerId))
                )
            }
            Log.i(TAG, "createStationsMarkers: created ${stationsMarkers.size} markers")
            return stationsMarkers
        }

        @JvmStatic
        private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
            // below line is use to generate a drawable.
            val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
            // below line is use to set bounds to our vector drawable.
            vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
            // below line is use to create a bitmap for our
            // drawable which we have added.
            val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            // below line is use to add bitmap in our canvas.
            val canvas = Canvas(bitmap)
            // below line is use to draw our
            // vector drawable in canvas.
            vectorDrawable.draw(canvas)
            // after generating our bitmap we are returning our bitmap.
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        /**
         * Returns the closest Station
         * to the selected location.
         */
        @JvmStatic
        fun getClosestStation(destination: LatLng): Station? {
            var smallestDistance: Float = getDistance(destination.latitude, destination.longitude, BusTrackerApplication.stations[0].latitude, BusTrackerApplication.stations[0].longitude)
            var closestStation: Station? = null
            for (station in BusTrackerApplication.stations) {
                val currentDistance = getDistance(destination.latitude, destination.longitude, station.latitude, station.longitude)
                if (currentDistance < smallestDistance) {
                    smallestDistance = currentDistance
                    closestStation = station
                }
            }
            return closestStation
        }

        @JvmStatic
        private fun getDistance(startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double): Float {
            val results = FloatArray(1)
            Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results)
            return results[0]
        }

        /**
         * Method to find the route.
         */
        @JvmStatic
        private fun findRoutes(wayPoints: List<LatLng>, listener: RoutingListener, context: Context): Routing? {
            return if (wayPoints.isNullOrEmpty()) {
                context.let {
                    Toast.makeText(context, "Unable to get location", Toast.LENGTH_LONG).show()
                }
                null
            } else {
                val routing = Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(listener)
                    .alternativeRoutes(true)
                    .waypoints(wayPoints)
                    .key(context.getString(R.string.google_maps_key))
                    .build()
                routing
            }
        }

        /**
         * Method to return WayPoints (list of LatLng)
         * from given list of Stations.
         */
        @JvmStatic
        private fun getWayPointsForStations(stations: List<Station>): List<LatLng> {
            val wayPoints: MutableList<LatLng> = ArrayList()
            for (station in stations) {
                wayPoints.add(LatLng(station.latitude, station.longitude))
            }
            return wayPoints
        }

        /**
         * Returns routing based on given list of Strings
         * containing the names of the stations.
         */
        @JvmStatic
        fun getRouteFromStations(stations: List<String>, context: Context, listener: RoutingListener): Routing? {
            val stationsList = BusTrackerApplication.stations
            val stationsInDirection: List<Station> = stations.mapNotNull { x ->
                stationsList.firstOrNull { y -> y.name == x }
            }
            val wayPoints = getWayPointsForStations(stationsInDirection)
            return findRoutes(wayPoints, listener, context)
        }
    }
}