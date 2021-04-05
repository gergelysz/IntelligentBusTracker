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
import com.example.intelligentbustracker.model.Bus
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.LeavingHour
import com.example.intelligentbustracker.model.Station
import com.example.intelligentbustracker.util.GeneralUtils
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.ArrayList
import kotlinx.android.synthetic.main.fragment_route_dialog_layout.view.route_dialog_recycler_view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class RoutingMapFragment : DialogFragment() {

    private val routingMapContext: Context = BusTrackerApplication.getInstance()
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var mMap: GoogleMap

    private lateinit var busNumbers: IntArray
    private lateinit var selectedStation: String
    private lateinit var currentLatLng: LatLng

    private lateinit var routeAdapter: RouteRecyclerAdapter
    private lateinit var buses: List<Bus>
    private lateinit var busesWithDirection: Map<Bus, Direction>
    private lateinit var jobProcessBusesWithDirection: Deferred<Map<Bus, Direction>>

    private var routeToClosestStation: Routing? = null
    private var polyLines = arrayListOf<Polyline>()

    companion object {
        private const val TAG = "RoutingMapFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        busNumbers = arguments?.getIntArray("buses_with_number") ?: IntArray(0)
        selectedStation = arguments?.getString("station_name") ?: ""
        currentLatLng = arguments?.getParcelable("current_latlng") ?: LatLng(46.539892, 24.558334)

        buses = GeneralUtils.generateBusListFromBusNumbers(BusTrackerApplication.buses, busNumbers)
        jobProcessBusesWithDirection = scope.async { GeneralUtils.getBusesWithDirectionForStation(selectedStation, buses) }

        activity?.let {
            return GeneralUtils.buildFullscreenDialog(it, R.color.darker_orange)
        } ?: return GeneralUtils.buildFullscreenDialog(requireActivity(), R.color.darker_orange)
    }

    private var routingListenerToStation: RouteRecyclerAdapter.OnRouteItemClickListener = object : RouteRecyclerAdapter.OnRouteItemClickListener {
        override fun onItemClick(bus: Bus, position: Int) {
            Log.i(TAG, "onItemClick: selected bus ${bus.number}")

            val leavingHour: LeavingHour? = GeneralUtils.getEarliestLeaveTimeForBusTowardsStation(bus, selectedStation)
            leavingHour?.let { leaving ->
                Toast.makeText(routingMapContext, "Closest arrival time for ${bus.number} is at ${leaving.hour} from ${leaving.fromStation}", Toast.LENGTH_SHORT).show()
                var stations: List<Station> = ArrayList()
                if (bus.scheduleRoutes.scheduleRoute1[0] == leaving.fromStation) {
                    stations = GeneralUtils.generateStationListFromStationNames(BusTrackerApplication.stations, bus.scheduleRoutes.scheduleRoute1)
                } else if (bus.scheduleRoutes.scheduleRoute2[0] == leaving.fromStation) {
                    stations = GeneralUtils.generateStationListFromStationNames(BusTrackerApplication.stations, bus.scheduleRoutes.scheduleRoute2)
                }
                val closestStation: Station? = GeneralUtils.getClosestStationFromListOfStations(currentLatLng, stations)
                closestStation?.let { station ->
                    routeToClosestStation = MapUtils.findRoutesBetweenTwoPoints(
                        currentLatLng,
                        LatLng(station.latitude, station.longitude),
                        routingMapContext,
                        routingListenerToClosestStation,
                        AbstractRouting.TravelMode.WALKING
                    )
                    routeToClosestStation?.execute()
                    MapUtils.moveCameraToLatLngWithZoom(mMap, currentLatLng, 18F)
                }
            }

            routeAdapter.notifyItemChanged(position)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_route_dialog_layout, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_route_dialog_map_container) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            MapUtils.setupMap(mMap, routingMapContext, BusTrackerApplication.mapTheme.value!!)

            mMap.setOnMapClickListener { position ->
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 10F))
            }
        }

        rootView.route_dialog_recycler_view.apply {
            layoutManager = LinearLayoutManager(routingMapContext)
            routeAdapter = RouteRecyclerAdapter(routingListenerToStation)
            adapter = routeAdapter
            runBlocking {
                busesWithDirection = jobProcessBusesWithDirection.await()
            }
            routeAdapter.submitBuses(busesWithDirection)
        }

        return rootView
    }

    private var routingListenerToClosestStation: RoutingListener = object : RoutingListener {

        override fun onRoutingFailure(ex: RouteException?) {
            Log.e(TAG, "onRoutingFailure: Routing failed. More details: ${ex?.message}")
        }

        override fun onRoutingStart() {
            Log.i(TAG, "onRoutingStart: Finding Route")
        }

        override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
            polyLines = if (!polyLines.isNullOrEmpty()) {
                for (polyline in polyLines) {
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
                    polyLines.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
        }
    }
}