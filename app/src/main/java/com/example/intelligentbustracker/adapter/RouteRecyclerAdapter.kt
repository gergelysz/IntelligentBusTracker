package com.example.intelligentbustracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.BusResult
import com.example.intelligentbustracker.util.GeneralUtils
import kotlinx.android.synthetic.main.layout_route_list_item.view.route_bus_details
import kotlinx.android.synthetic.main.layout_route_list_item.view.route_bus_number
import kotlinx.android.synthetic.main.layout_route_list_item.view.route_bus_title

class RouteRecyclerAdapter(private val listener: OnRouteItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var items: List<List<BusResult>>

    private lateinit var busNumberText: String
    private lateinit var busTitleText: String
    private lateinit var busDetailsText: String

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

        fun bind(busResult: List<BusResult>) {
            initValues(busResult)

            routeBusNumber.text = busNumberText
            routeBusTitle.text = busTitleText
            routeBusDetails.text = busDetailsText
        }

        override fun onClick(v: View?) {
            val position: Int = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(items[position], position)
            }
        }
    }

    interface OnRouteItemClickListener {
        fun onItemClick(busResult: List<BusResult>, position: Int)
    }

    fun submitBuses(buses: List<List<BusResult>>) {
        items = buses
    }

    /**
     * Initializes values for the TextViews in the list item.
     */
    private fun initValues(busResult: List<BusResult>) {
        if (busResult.size == 1) {
            busNumberText = busResult[0].bus.number
            busTitleText = "${busResult[0].stationUp} - ${busResult[0].stationDown}"
            val hours = GeneralUtils.getEarliestNLeaveTimesForBusTowardsStation(busResult[0].bus.number, busResult[0].stationDown, 3)
            busDetailsText = hours.joinToString(", ") { x -> x.hour }
        } else if (busResult.size > 1) {
            busNumberText = busResult[0].bus.number + "\n➜\n" + busResult[1].bus.number
            busTitleText = "${busResult[0].stationUp} - ${busResult[0].stationDown} ➜ ${busResult[1].stationUp} - ${busResult[1].stationDown}"
            val hoursFirst = GeneralUtils.getEarliestNLeaveTimesForBusTowardsStation(busResult[0].bus.number, busResult[0].stationDown, 3)
            val hoursSecond = GeneralUtils.getEarliestNLeaveTimesForBusTowardsStation(busResult[1].bus.number, busResult[1].stationDown, 3)
            busDetailsText = hoursFirst.joinToString(", ") { x -> x.hour } + "\n" + hoursSecond.joinToString(", ") { x -> x.hour }
        }
    }
}