package com.example.intelligentbustracker.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class ScheduleMapFragment : DialogFragment() {

    private lateinit var mMap: GoogleMap
    private var polyLinesDirection1 = arrayListOf<Polyline>()
    private var polyLinesDirection2 = arrayListOf<Polyline>()
    private val scheduleMapContext: Context = BusTrackerApplication.getInstance()

    private lateinit var stationNameDirection1: String
    private lateinit var stationNameDirection2: String

    private var listenerDirection1: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Toast.makeText(scheduleMapContext, "Routing failed. More details: ${ex?.message}", Toast.LENGTH_LONG).show()
        }

        override fun onRoutingStart() {
            Toast.makeText(scheduleMapContext, "Finding Route...", Toast.LENGTH_LONG).show()
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesDirection1 = if (!polyLinesDirection1.isNullOrEmpty()) {
                for (polyline in polyLinesDirection1) {
                    polyline.remove()
                }
                ArrayList()
            } else {
                ArrayList()
            }
            val polyOptions = PolylineOptions()
            //add route(s) to the map using polyline
            for (i in 0 until route.size) {
                if (i == shortestRouteIndex) {
                    polyOptions.color(ContextCompat.getColor(requireContext(), R.color.blue))
                    polyOptions.width(7f)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyline.tag = stationNameDirection1
                    polyLinesDirection1.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Toast.makeText(scheduleMapContext, "Routing cancelled", Toast.LENGTH_LONG).show()
        }
    }

    private var listenerDirection2: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Toast.makeText(scheduleMapContext, "Routing failed. More details: ${ex?.message}", Toast.LENGTH_LONG).show()
        }

        override fun onRoutingStart() {
            Toast.makeText(scheduleMapContext, "Finding Route...", Toast.LENGTH_LONG).show()
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesDirection2 = if (!polyLinesDirection2.isNullOrEmpty()) {
                for (polyline in polyLinesDirection2) {
                    polyline.remove()
                }
                ArrayList()
            } else {
                ArrayList()
            }
            val polyOptions = PolylineOptions()
            //add route(s) to the map using polyline
            for (i in 0 until route.size) {
                if (i == shortestRouteIndex) {
                    polyOptions.color(ContextCompat.getColor(requireContext(), R.color.red))
                    polyOptions.width(7f)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyline.tag = stationNameDirection2
                    polyLinesDirection2.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Toast.makeText(scheduleMapContext, "Routing cancelled", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_schedule_map, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_schedule_map) as SupportMapFragment

        val busNumber: String = arguments?.getString("bus_number") ?: "0"

        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap

            mMap.setOnMapClickListener { position ->
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 10F))
            }

            mMap.setMaxZoomPreference(20F)
            mMap.setMinZoomPreference(15F)
            val tgMuresDefault = LatLng(46.539892, 24.558334)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(tgMuresDefault))

            if (busNumber != "0") {
                val selectedBus: Bus? = BusTrackerApplication.buses.firstOrNull { x -> busNumber == x.number }
                selectedBus?.let {
                    Toast.makeText(scheduleMapContext, "Selected bus with number ${selectedBus.number}", Toast.LENGTH_LONG).show()
                    stationNameDirection1 = selectedBus.scheduleRoutes.scheduleRoute1[0]
                    stationNameDirection2 = selectedBus.scheduleRoutes.scheduleRoute2[0]

                    val scope = CoroutineScope(Dispatchers.Default)

                    val asyncCreateRoutingDirection1: Deferred<Routing?> = scope.async { MapUtils.getRouteFromStations(selectedBus.scheduleRoutes.scheduleRoute1, scheduleMapContext, listenerDirection1) }
                    val asyncCreateRoutingDirection2: Deferred<Routing?> = scope.async { MapUtils.getRouteFromStations(selectedBus.scheduleRoutes.scheduleRoute2, scheduleMapContext, listenerDirection2) }

                    runBlocking {
                        val routing1 = asyncCreateRoutingDirection1.await()
                        val routing2 = asyncCreateRoutingDirection2.await()
                        routing1?.execute()
                        routing2?.execute()
                    }
                }
            } else {
                Toast.makeText(scheduleMapContext, "Failed to retrieve the number of the selected bus, please try again", Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
        return rootView
    }
}