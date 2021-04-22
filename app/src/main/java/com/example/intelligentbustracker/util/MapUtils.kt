package com.example.intelligentbustracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.directions.route.AbstractRouting
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.model.User
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.stream.Collectors

class MapUtils {

    companion object {
        private const val TAG = "MapUtils"

        private val DARK_MAPS = listOf("map_style_dark", "map_style_night")
        private val LIGHT_MAPS = listOf("map_style_standard", "map_style_retro")

        fun animateMarker(marker: Marker, toPosition: LatLng, projection: Projection) {
            val handler = Handler()
            val start = SystemClock.uptimeMillis()
            val proj: Projection = projection
            val startPoint: Point = proj.toScreenLocation(marker.position)
            val startLatLng: LatLng = proj.fromScreenLocation(startPoint)
            val duration: Long = 500
            val interpolator: Interpolator = LinearInterpolator()
            handler.post(object : Runnable {
                override fun run() {
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                    val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude
                    val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude
                    marker.position = LatLng(lat, lng)
                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16)
                    } else {
                        marker.isVisible = true
                    }
                }
            })
        }

        /**
         * Moves the camera to the given location.
         */
        fun moveCameraToLocation(mMap: GoogleMap, location: LatLng) {
            val cameraPosition = CameraPosition.Builder()
                .target(location)
                .zoom(15F)
                .build()
            val cu = CameraUpdateFactory.newCameraPosition(cameraPosition)
            mMap.animateCamera(cu)
        }

        fun moveCameraToLatLngWithZoom(mMap: GoogleMap, latLng: LatLng, zoomLevel: Float) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        }

        /**
         * Check if new map theme and old map theme are of type light or
         * dark and then check if they match so trigger style change.
         */
        fun mapThemeChangeNeeded(themeString: String, latestMapTheme: String): Boolean {
            val currentAndLatestTheme = listOf(themeString, latestMapTheme)
            if (DARK_MAPS.containsAll(currentAndLatestTheme) || LIGHT_MAPS.containsAll(currentAndLatestTheme)) {
                return themeString != latestMapTheme
            }
            return true
        }

        /**
         * Initial setup of map.
         */
        fun setupMap(mMap: GoogleMap, context: Context, themeString: String) {
            mMap.uiSettings.isTiltGesturesEnabled = false
            mMap.uiSettings.isMapToolbarEnabled = false
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, context.resources.getIdentifier(themeString, "raw", context.packageName)))
            mMap.setMaxZoomPreference(20F)
            mMap.setMinZoomPreference(12F)
            val tgMuresDefault = LatLng(46.539892, 24.558334)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tgMuresDefault, 15F))
        }

        /**
         * Method to find the route
         * between two points.
         */
        fun findRoutesBetweenTwoPoints(start: LatLng, end: LatLng, context: Context, listener: RoutingListener, travelMode: AbstractRouting.TravelMode): Routing {
            return Routing.Builder()
                .travelMode(travelMode)
                .withListener(listener)
                .alternativeRoutes(true)
                .waypoints(start, end)
                .key(context.getString(R.string.google_maps_key))
                .build()
        }

        /**
         * Method to find the route
         * between multiple points.
         */
        fun findRoutesBetweenMultiplePoints(wayPoints: List<LatLng>, context: Context, listener: RoutingListener, travelMode: AbstractRouting.TravelMode): Routing {
            return Routing.Builder()
                .travelMode(travelMode)
                .withListener(listener)
                .alternativeRoutes(true)
                .waypoints(wayPoints)
                .key(context.getString(R.string.google_maps_key))
                .build()
        }

        fun getMarkerIdForStations(): Int {
            return when (BusTrackerApplication.mapTheme.value) {
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
        }

        fun getMarkerIdForUsers(): Int {
            return when (BusTrackerApplication.mapTheme.value) {
                "map_style_dark" -> {
                    R.drawable.ic_on_bus_white
                }
                "map_style_night" -> {
                    R.drawable.ic_on_bus_white
                }
                "map_style_standard" -> {
                    R.drawable.ic_on_bus_black
                }
                "map_style_retro" -> {
                    R.drawable.ic_on_bus_black
                }
                else -> R.drawable.ic_on_bus_white
            }
        }

        fun getMarkerIdForCurrentUser(): Int {
            return when (BusTrackerApplication.mapTheme.value) {
                "map_style_dark" -> {
                    R.drawable.ic_user_standing_white
                }
                "map_style_night" -> {
                    R.drawable.ic_user_standing_white
                }
                "map_style_standard" -> {
                    R.drawable.ic_user_standing_black
                }
                "map_style_retro" -> {
                    R.drawable.ic_user_standing_black
                }
                else -> R.drawable.ic_user_standing_white
            }
        }

        fun updateStationMarkersColor(markers: List<Marker>, context: Context) {
            val markerId: Int = getMarkerIdForStations()
            for (marker in markers) {
                marker.setIcon(bitmapFromVector(context, markerId))
            }
        }

        fun updateUserMarkersColor(markers: List<Marker>, context: Context) {
            val markerId: Int = getMarkerIdForUsers()
            for (marker in markers) {
                marker.setIcon(bitmapFromVector(context, markerId))
            }
        }

        fun updateCurrentUserMarkerColor(marker: Marker, context: Context) {
            val markerId: Int = getMarkerIdForCurrentUser()
            marker.setIcon(bitmapFromVector(context, markerId))
        }

        /**
         * Draws markers for users on the map based on their
         * coordinates and adds the bus number as title.
         */
        fun addUsersMarkers(mMap: GoogleMap, usersMarkers: List<MarkerOptions>) {
            Log.i(TAG, "addUsersMarkers: adding ${usersMarkers.size} markers")
            for (stationMarker in usersMarkers) {
                mMap.addMarker(stationMarker)
            }
            Log.i(TAG, "addUsersMarkers: added ${usersMarkers.size} markers")
        }

        fun updateUsersMarkers(users: List<User>, usersMarkers: MutableList<Marker>, currentUserId: String, mMap: GoogleMap, context: Context) {
            val markerId: Int = getMarkerIdForUsers()
            for (user in users) {
                val userMarker = usersMarkers.firstOrNull { x -> x.snippet.equals(user.id) }
                userMarker?.let {
                    animateMarker(it, LatLng(user.latitude, user.longitude), mMap.projection)
//                    it.position = LatLng(user.latitude, user.longitude)
                    Log.i(TAG, "updated ${user.id} user's position")
                } ?: run {
                    if (currentUserId != user.id) {
                        usersMarkers.add(
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(user.latitude, user.longitude))
                                    .title(user.bus.toString())
                                    .snippet(user.id)
                                    .icon(bitmapFromVector(context, markerId))
                            )
                        )
                    }
                }
            }
            val removedUsers: List<Marker> = usersMarkers.filter { x -> users.stream().noneMatch { y -> x.snippet == y.id } }
            for (removedUser in removedUsers) {
                removedUser.remove()
                usersMarkers.remove(removedUser)
            }
        }

        /**
         * Builds a list with the Markers for the stations
         * to be later added onto the map.
         */
        fun createAndAddUsersMarkers(users: List<User>, context: Context, mMap: GoogleMap): MutableList<Marker> {
            Log.i(TAG, "createStationsMarkers: creating ${users.size} markers")
            val markerId: Int = getMarkerIdForUsers()
            val usersMarkers = ArrayList<Marker>()
            for (user in users) {
                usersMarkers.add(
                    mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(user.latitude, user.longitude))
                            .title(user.bus.toString())
                            .snippet(user.id)
                            .icon(bitmapFromVector(context, markerId))
                    )
                )
            }
            Log.i(TAG, "createStationsMarkers: created ${usersMarkers.size} markers")
            return usersMarkers
        }

        /**
         * Draws markers for stations on the map based on
         * their coordinates and adds their name as title.
         */
        fun addStationMarkers(mMap: GoogleMap, stationsMarkers: List<MarkerOptions>): List<Marker> {
            val stations: MutableList<Marker> = ArrayList()
            Log.i(TAG, "addStationMarkers: adding ${stationsMarkers.size} markers")
            for (stationMarker in stationsMarkers) {
                stations.add(mMap.addMarker(stationMarker))
            }
            Log.i(TAG, "addStationMarkers: added ${stationsMarkers.size} markers")
            return stations
        }

        /**
         * Builds a list with the Markers for the stations
         * to be later added onto the map.
         */
        fun createStationsMarkers(stations: List<Station>, context: Context): List<MarkerOptions> {
            Log.i(TAG, "createStationsMarkers: creating ${stations.size} markers")
            val markerId: Int = getMarkerIdForStations()
            val stationsMarkers = ArrayList<MarkerOptions>()
            for (station in stations) {
                val busesWithStation = GeneralUtils.getBusesWithGivenStation(station).stream().map { x -> x.number }.collect(Collectors.toList())
                val snippetData = "Related buses: " + busesWithStation.joinToString(", ")
                stationsMarkers.add(
                    MarkerOptions()
                        .position(LatLng(station.latitude, station.longitude))
                        .title(station.name)
                        .snippet(snippetData)
                        .icon(bitmapFromVector(context, markerId))
                )
            }
            Log.i(TAG, "createStationsMarkers: created ${stationsMarkers.size} markers")
            return stationsMarkers
        }

        fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
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
         * Method to find the route.
         */
        private fun findRoutes(wayPoints: List<LatLng>, listener: RoutingListener, context: Context): Routing? {
            return if (wayPoints.isNullOrEmpty()) {
                context.let {
                    Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
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
        private fun getWayPointsForStations(stations: List<Station>): List<LatLng> {
            val wayPoints = arrayListOf<LatLng>()
            for (station in stations) {
                wayPoints.add(LatLng(station.latitude, station.longitude))
            }
            return wayPoints
        }

        /**
         * Returns routing based on given list of Strings
         * containing the names of the stations.
         */
        fun getRouteFromStations(stations: List<Station>, context: Context, listener: RoutingListener): Routing? {
            val wayPoints = getWayPointsForStations(stations)
            return findRoutes(wayPoints, listener, context)
        }
    }
}