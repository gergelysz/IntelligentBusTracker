package com.example.intelligentbustracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.activity.MainActivity
import com.example.intelligentbustracker.model.Station
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapUtils {

    companion object {
        /**
         * Draws markers for stations on the map based on
         * their coordinates and adds their name as title.
         */
        fun addStationMarkers(mMap: GoogleMap, stationsMarkers: ArrayList<MarkerOptions>) {
            Log.i("MapUtils", "addStationMarkers: adding ${stationsMarkers.size} markers")
            for (stationMarker in stationsMarkers) {
                mMap.addMarker(stationMarker)
            }
            Log.i("MapUtils", "addStationMarkers: added ${stationsMarkers.size} markers")
        }

        /**
         * Builds a list with the Markers for the stations
         * to be later added onto the map.
         */
        fun createStationsMarkers(stations: ArrayList<Station>, context: Context): ArrayList<MarkerOptions> {
            Log.i("MapUtils", "createStationsMarkers: creating ${stations.size} markers")
            val markerId: Int = when (MainActivity.mapTheme) {
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
            Log.i("MapUtils", "createStationsMarkers: created ${stationsMarkers.size} markers")
            return stationsMarkers
        }

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
    }
}