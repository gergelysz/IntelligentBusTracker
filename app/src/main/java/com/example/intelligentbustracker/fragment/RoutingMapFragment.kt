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
import com.example.intelligentbustracker.util.RoutingUtil
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.ArrayList
import kotlinx.android.synthetic.main.fragment_route_dialog_layout.view.route_dialog_recycler_view

class RoutingMapFragment : DialogFragment() {

    private val routingMapContext: Context = BusTrackerApplication.getInstance()
//    private val scope = CoroutineScope(Dispatchers.Default)

//    private lateinit var mMap: GoogleMap

    private lateinit var busNumbers: IntArray
    private lateinit var selectedStation: String

    private lateinit var buses: List<List<BusResult>>
//    private lateinit var buses: List<BusToStation>

    private lateinit var currentLatLng: LatLng
    private lateinit var destinationLatLng: LatLng

    private lateinit var routeAdapter: RouteRecyclerAdapter
//    private lateinit var buses: List<Bus>
//    private lateinit var busesWithDirection: Map<Bus, Direction>
//    private lateinit var jobProcessBusesWithDirection: Deferred<Map<Bus, Direction>>

    private var routeToClosestStation: Routing? = null
    private var polyLines = arrayListOf<Polyline>()

    companion object {
        private const val TAG = "RoutingMapFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireActivity())
        dialog.setContentView(R.layout.fragment_route_dialog_layout)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)


        busNumbers = arguments?.getIntArray("buses_with_number") ?: IntArray(0)
        selectedStation = arguments?.getString("station_name") ?: ""

        currentLatLng = arguments?.getParcelable("current_latlng") ?: LatLng(46.539892, 24.558334)
        destinationLatLng = arguments?.getParcelable("destination_latlng") ?: LatLng(46.539892, 24.558334)

        buses = RoutingUtil.initializeManualRouting(currentLatLng, destinationLatLng)

        return dialog
    }

    private var routingListenerToStation: RouteRecyclerAdapter.OnRouteItemClickListener = object : RouteRecyclerAdapter.OnRouteItemClickListener {
        override fun onItemClick(busResult: List<BusResult>, position: Int) {
            Log.i(TAG, "onItemClick: selected bus ${busResult}")

            val stationName: String = if (busResult[0].direction == Direction.DIRECTION_1) {
                busResult[0].bus.scheduleRoutes.scheduleRoute1[0]
            } else {
                busResult[0].bus.scheduleRoutes.scheduleRoute2[0]
            }
            val stationFrom = GeneralUtils.getStationFromName(stationName)
            stationFrom?.let {
                Toast.makeText(routingMapContext, "Arrives in approximately ${GeneralUtils.getDurationForRoute(LatLng(it.latitude, it.longitude), currentLatLng, routingMapContext)}", Toast.LENGTH_SHORT).show()
            }


//            val leavingHour: LeavingHour? = GeneralUtils.getEarliestLeaveTimeForBusTowardsStation(bus.busNumber, bus.stationTo.name)
//            leavingHour?.let { leaving ->
//                Toast.makeText(routingMapContext, "Closest arrival time for ${bus.busNumber} is at ${leaving.hour} from ${leaving.fromStation}", Toast.LENGTH_SHORT).show()
//                var stations: List<Station> = ArrayList()
//                if (bus.scheduleRoutes.scheduleRoute1[0] == leaving.fromStation) {
//                    stations = GeneralUtils.generateStationListFromStationNames(BusTrackerApplication.stations, bus.scheduleRoutes.scheduleRoute1)
//                } else if (bus.scheduleRoutes.scheduleRoute2[0] == leaving.fromStation) {
//                    stations = GeneralUtils.generateStationListFromStationNames(BusTrackerApplication.stations, bus.scheduleRoutes.scheduleRoute2)
//                }
//                val closestStation: Station? = GeneralUtils.getClosestStationFromListOfStations(currentLatLng, stations)
//                closestStation?.let { station ->
//                routeToClosestStation = MapUtils.findRoutesBetweenTwoPoints(
//                    currentLatLng,
////                    LatLng(station.latitude, station.longitude),
//                    LatLng(bus.stationFrom.latitude, bus.stationFrom.longitude),
//                    routingMapContext,
//                    routingListenerToClosestStation,
//                    AbstractRouting.TravelMode.WALKING
//                )
//                routeToClosestStation?.execute()
//                MapUtils.moveCameraToLatLngWithZoom(mMap, currentLatLng, 18F)
//                }
//            }

            routeAdapter.notifyItemChanged(position)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_route_dialog_layout, container, false)

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
//                    val polyline = mMap.addPolyline(polyOptions)
//                    polyLines.add(polyline)
                } else {
                }
            }
        }

        override fun onRoutingCancelled() {
            Log.w(TAG, "onRoutingCancelled: Routing cancelled")
        }
    }
}