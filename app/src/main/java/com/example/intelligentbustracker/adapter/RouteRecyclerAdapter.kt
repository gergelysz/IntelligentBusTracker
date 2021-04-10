package com.example.intelligentbustracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.BusToStation
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.LeavingHours
import com.example.intelligentbustracker.util.GeneralUtils
import java.util.Calendar
import kotlinx.android.synthetic.main.layout_route_list_item.view.route_bus_details
import kotlinx.android.synthetic.main.layout_route_list_item.view.route_bus_number
import kotlinx.android.synthetic.main.layout_route_list_item.view.route_bus_title

class RouteRecyclerAdapter(private val listener: OnRouteItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

//    private lateinit var submittedBuses: Map<Bus, Direction>

    //    private lateinit var items: List<Bus>
    private lateinit var items: List<BusToStation>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RouteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_route_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RouteViewHolder -> {
                holder.bind(items[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class RouteViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val routeBusNumber: TextView = itemView.route_bus_number
        private val routeBusTitle: TextView = itemView.route_bus_title
        private val routeBusDetails: TextView = itemView.route_bus_details

        init {
            itemView.setOnClickListener(this)
        }

        //        fun bind(bus: Bus) {
        fun bind(bus: BusToStation) {
            routeBusNumber.text = bus.busNumber.toString()
//            routeBusTitle.text = getDirection(bus)
            routeBusTitle.text = "${bus.scheduleRoute.first()} - ${bus.scheduleRoute.last()}"
//            routeBusDetails.text = getSchedule(bus, GeneralUtils.getDay())
//            routeBusDetails.text = getSchedule(bus, GeneralUtils.getTodayDayType())
            val hours = GeneralUtils.getEarliestNLeaveTimesForBusTowardsStation(bus.busNumber, bus.stationTo.name, 3)
            routeBusDetails.text = hours.joinToString(", ") { x -> x.hour }
        }

        override fun onClick(v: View?) {
            val position: Int = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(items[position], position)
            }
        }
    }

    interface OnRouteItemClickListener {
        //        fun onItemClick(bus: Bus, position: Int)
        fun onItemClick(bus: BusToStation, position: Int)
    }

    //    fun submitBuses(buses: Map<Bus, Direction>) {
    fun submitBuses(buses: List<BusToStation>) {
//        items = ArrayList(buses.keys)
        items = buses
//        submittedBuses = buses
    }

//    private fun getDirection(bus: Bus): String {
//        val direction = submittedBuses[bus]
//        if (Direction.DIRECTION_1 == direction) {
//            return "${bus.scheduleRoutes.scheduleRoute1.first()} - ${bus.scheduleRoutes.scheduleRoute1.last()}"
//        } else if (Direction.DIRECTION_2 == direction) {
//            return "${bus.scheduleRoutes.scheduleRoute2.first()} - ${bus.scheduleRoutes.scheduleRoute2.last()}"
//        }
//        return "Couldn't determine direction"
//    }

    private fun getSchedule(bus: BusToStation, day: Int): String {
        val schedule = GeneralUtils.getScheduleFromBusNumber(bus.busNumber, BusTrackerApplication.schedules)
        schedule?.let {
            if (Direction.DIRECTION_1 == bus.direction) {
                return processSchedules(it.leavingHours1, day)
            } else if (Direction.DIRECTION_2 == bus.direction) {
                return processSchedules(it.leavingHours2, day)
            }
        }
        return "Couldn't determine direction"
    }

    private fun processSchedules(leavingHours: LeavingHours, day: Int): String {
        val scheduleTimes: List<String> = when (day) {
            Calendar.SATURDAY -> leavingHours.saturdayLeavingHours
            Calendar.SUNDAY -> leavingHours.sundayLeavingHours
            else -> leavingHours.weekdayLeavingHours
        }
        return scheduleTimes.joinToString(" ")
    }
}