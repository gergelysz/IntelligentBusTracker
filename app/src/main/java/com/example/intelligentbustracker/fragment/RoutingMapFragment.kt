package com.example.intelligentbustracker.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.directions.route.AbstractRouting
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.adapter.RouteRecyclerAdapter
import com.example.intelligentbustracker.model.BusResult
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.util.GeneralUtils
import com.example.intelligentbustracker.util.MapUtils
import com.example.intelligentbustracker.util.RoutingUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.ArrayList
import kotlinx.android.synthetic.main.fragment_route_dialog_layout.view.route_dialog_recycler_view

class RoutingMapFragment : DialogFragment() {

    private val routingMapContext: Context = BusTrackerApplication.getInstance()

    private lateinit var mMap: GoogleMap

    private lateinit var buses: List<List<BusResult>>

    private lateinit var currentLatLng: LatLng
    private lateinit var destinationLatLng: LatLng

    private lateinit var routeAdapter: RouteRecyclerAdapter

    private var routeToClosestStation: Routing? = null
    private var routeFromClosestStation: Routing? = null
    private var routeStationToStation1: Routing? = null
    private var routeStationToStation2: Routing? = null

    private var polyLinesRouteToClosestStation = arrayListOf<Polyline>()
    private var polyLinesRouteFromClosestStation = arrayListOf<Polyline>()
    private var polyLinesRouteFromStationToStation1 = arrayListOf<Polyline>()
    private var polyLinesRouteFromStationToStation2 = arrayListOf<Polyline>()

    companion object {
        private const val TAG = "RoutingMapFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {
            return GeneralUtils.buildFullscreenDialog(it, R.color.darker_orange)
        } ?: return GeneralUtils.buildFullscreenDialog(requireActivity(), R.color.darker_orange)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_route_dialog_layout, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_route_dialog_map_container) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            MapUtils.setupMap(mMap, routingMapContext, BusTrackerApplication.mapTheme.value!!)

            mMap.setOnMapClickListener { position ->
                // Not implemented yet
            }
        }

        currentLatLng = arguments?.getParcelable("current_latlng") ?: LatLng(46.539892, 24.558334)
        destinationLatLng = arguments?.getParcelable("destination_latlng") ?: LatLng(46.539892, 24.558334)

        buses = RoutingUtil.initializeManualRouting(currentLatLng, destinationLatLng)

        if (buses.isEmpty()) {
            Toast.makeText(routingMapContext, "No buses were found.", Toast.LENGTH_LONG).show()
            dismiss()
        } else {
            rootView.route_dialog_recycler_view.apply {
                layoutManager = LinearLayoutManager(routingMapContext)
                routeAdapter = RouteRecyclerAdapter(routingListenerToStation)
                adapter = routeAdapter
                routeAdapter.submitBuses(buses)
            }
        }

        return rootView
    }

    private fun drawRouteForOne(busResult: BusResult) {
        val stationFrom = GeneralUtils.getStationFromName(busResult.stationUp)
        val stationTo = GeneralUtils.getStationFromName(busResult.stationDown)

        if (stationFrom != null && stationTo != null) {
            val latLngFrom = LatLng(stationFrom.latitude, stationFrom.longitude)
            val latLngTo = LatLng(stationTo.latitude, stationTo.longitude)

            routeToClosestStation = MapUtils.findRoutesBetweenTwoPoints(
                busResult.positionFrom,
                latLngFrom,
                routingMapContext,
                routingListenerToClosestStation,
                AbstractRouting.TravelMode.WALKING
            )
            routeToClosestStation?.execute()

            val stations = busResult.getStationsFromInterval()
            val waypointsStationToStation = MapUtils.getWayPointsForStations(stations)

            routeStationToStation1 = MapUtils.findRoutesBetweenMultiplePoints(
                waypointsStationToStation,
                routingMapContext,
                routingListenerFromStationToStation1,
                AbstractRouting.TravelMode.DRIVING
            )
            routeStationToStation1?.execute()

            routeFromClosestStation = MapUtils.findRoutesBetweenTwoPoints(
                latLngTo,
                busResult.positionTo,
                routingMapContext,
                routingListenerFromClosestStation,
                AbstractRouting.TravelMode.WALKING
            )
            routeFromClosestStation?.execute()

            MapUtils.moveCameraToLatLngWithZoom(mMap, currentLatLng, 15F)
        }
    }

    private fun drawRouteForTwo(busResult: List<BusResult>) {
        val stationFrom1 = GeneralUtils.getStationFromName(busResult[0].stationUp)
        val stationTo1 = GeneralUtils.getStationFromName(busResult[0].stationDown)

        val stationFrom2 = GeneralUtils.getStationFromName(busResult[1].stationUp)
        val stationTo2 = GeneralUtils.getStationFromName(busResult[1].stationDown)

        if (stationFrom1 != null && stationTo1 != null && stationFrom2 != null && stationTo2 != null) {
            val latLngFrom1 = LatLng(stationFrom1.latitude, stationFrom1.longitude)
            val latLngTo1 = LatLng(stationTo1.latitude, stationTo1.longitude)

            val latLngFrom2 = LatLng(stationFrom2.latitude, stationFrom2.longitude)
            val latLngTo2 = LatLng(stationTo2.latitude, stationTo2.longitude)

            routeToClosestStation = MapUtils.findRoutesBetweenTwoPoints(
                busResult[0].positionFrom,
                latLngFrom1,
                routingMapContext,
                routingListenerToClosestStation,
                AbstractRouting.TravelMode.WALKING
            )
            routeToClosestStation?.execute()

            val stations1 = busResult[0].getStationsFromInterval()
            val stations2 = busResult[1].getStationsFromInterval()

            val waypointsStationToStation1 = MapUtils.getWayPointsForStations(stations1)
            val waypointsStationToStation2 = MapUtils.getWayPointsForStations(stations2)

            routeStationToStation1 = MapUtils.findRoutesBetweenMultiplePoints(
                waypointsStationToStation1,
                routingMapContext,
                routingListenerFromStationToStation1,
                AbstractRouting.TravelMode.DRIVING
            )
            routeStationToStation1?.execute()

            routeStationToStation2 = MapUtils.findRoutesBetweenMultiplePoints(
                waypointsStationToStation2,
                routingMapContext,
                routingListenerFromStationToStation2,
                AbstractRouting.TravelMode.DRIVING
            )
            routeStationToStation1?.execute()

            routeFromClosestStation = MapUtils.findRoutesBetweenTwoPoints(
                latLngTo2,
                busResult[1].positionTo,
                routingMapContext,
                routingListenerFromClosestStation,
                AbstractRouting.TravelMode.WALKING
            )
            routeFromClosestStation?.execute()

            MapUtils.moveCameraToLatLngWithZoom(mMap, currentLatLng, 15F)
        }
    }

    private var routingListenerToStation: RouteRecyclerAdapter.OnRouteItemClickListener = object : RouteRecyclerAdapter.OnRouteItemClickListener {
        override fun onItemClick(busResult: List<BusResult>, position: Int) {
            if (busResult.size == 1) {
                Log.i(TAG, "onItemClick: selected bus ${busResult[0].bus.number}")
                drawRouteForOne(busResult[0])
            } else {
                Log.i(TAG, "onItemClick: selected buses ${busResult[0].bus.number} and ${busResult[1].bus.number}")
                drawRouteForTwo(busResult)
            }

            val stationName: String = if (busResult[0].direction == Direction.DIRECTION_1) {
                busResult[0].bus.scheduleRoutes.scheduleRoute1[0]
            } else {
                busResult[0].bus.scheduleRoutes.scheduleRoute2[0]
            }
            val stationFrom = GeneralUtils.getStationFromName(stationName)
            stationFrom?.let {
                Toast.makeText(routingMapContext, "Arrives in approximately ${busResult[0].getDurationToStationForHour(it)} minutes if leaves ${it.name} now", Toast.LENGTH_SHORT).show()
//                Toast.makeText(routingMapContext, "Arrives in approximately ${busResult[0].getDurationToStation(it)} minutes if leaves ${it.name} now", Toast.LENGTH_SHORT).show()
//                Toast.makeText(routingMapContext, "Arrives in approximately ${GeneralUtils.getDurationForRoute(LatLng(it.latitude, it.longitude), currentLatLng, routingMapContext)}", Toast.LENGTH_SHORT).show()
            }

            routeAdapter.notifyItemChanged(position)
        }
    }

    private var routingListenerToClosestStation: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex?.message}")
        }

        override fun onRoutingStart() {
            Log.i(TAG, "onRoutingStart: Finding Route")
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesRouteToClosestStation = if (!polyLinesRouteToClosestStation.isNullOrEmpty()) {
                for (polyline in polyLinesRouteToClosestStation) {
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
                    polyOptions.color(ContextCompat.getColor(requireContext(), R.color.green))
                    polyOptions.width(10F)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyLinesRouteToClosestStation.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
        }
    }

    private var routingListenerFromClosestStation: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex?.message}")
        }

        override fun onRoutingStart() {
            Log.i(TAG, "onRoutingStart: Finding Route")
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesRouteFromClosestStation = if (!polyLinesRouteFromClosestStation.isNullOrEmpty()) {
                for (polyline in polyLinesRouteFromClosestStation) {
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
                    polyOptions.color(ContextCompat.getColor(requireContext(), R.color.green))
                    polyOptions.width(10F)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyLinesRouteFromClosestStation.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
        }
    }

    private var routingListenerFromStationToStation1: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex?.message}")
        }

        override fun onRoutingStart() {
            Log.i(TAG, "onRoutingStart: Finding Route")
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesRouteFromStationToStation1 = if (!polyLinesRouteFromStationToStation1.isNullOrEmpty()) {
                for (polyline in polyLinesRouteFromStationToStation1) {
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
                    polyOptions.width(10F)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyLinesRouteFromStationToStation1.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
        }
    }

    private var routingListenerFromStationToStation2: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex?.message}")
        }

        override fun onRoutingStart() {
            Log.i(TAG, "onRoutingStart: Finding Route")
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLinesRouteFromStationToStation2 = if (!polyLinesRouteFromStationToStation2.isNullOrEmpty()) {
                for (polyline in polyLinesRouteFromStationToStation2) {
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
                    polyOptions.width(10F)
                    polyOptions.addAll(route[shortestRouteIndex].points)
                    val polyline = mMap.addPolyline(polyOptions)
                    polyLinesRouteFromStationToStation2.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
        }
    }
}