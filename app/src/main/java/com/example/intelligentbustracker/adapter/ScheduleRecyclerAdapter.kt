package com.example.intelligentbustracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Schedule
import com.example.intelligentbustracker.util.GeneralUtils
import java.util.Calendar
import kotlinx.android.synthetic.main.layout_schedule_list_item.view.schedule_bus_details
import kotlinx.android.synthetic.main.layout_schedule_list_item.view.schedule_bus_number
import kotlinx.android.synthetic.main.layout_schedule_list_item.view.schedule_bus_title

class ScheduleRecyclerAdapter(private val listener: OnScheduleItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Schedule> = ArrayList()

    private lateinit var busNumberText: String
    private lateinit var busTitleText: String
    private lateinit var busDetailsText: String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ScheduleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_schedule_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScheduleViewHolder -> {
                holder.bind(items[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ScheduleViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        private val scheduleBusNumber: TextView = itemView.schedule_bus_number
        private val scheduleBusTitle: TextView = itemView.schedule_bus_title
        private val scheduleBusDetails: TextView = itemView.schedule_bus_details

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(schedule: Schedule) {
            initValues(schedule, true)

            scheduleBusNumber.text = busNumberText
            scheduleBusTitle.text = busTitleText
            scheduleBusDetails.text = busDetailsText
        }

        override fun onClick(v: View) {
            val position: Int = bindingAdapterPosition

            val scheduleBusNumberClicked: TextView = v.schedule_bus_number
            val scheduleBusTitleClicked: TextView = v.schedule_bus_title
            val scheduleBusDetailsClicked: TextView = v.schedule_bus_details

            val switchBoolean: Boolean = "${items[position].leavingHours1.fromStation} - ${items[position].leavingHours2.fromStation}" == scheduleBusTitleClicked.text

            initValues(items[position], switchBoolean)

            scheduleBusNumberClicked.text = busNumberText
            scheduleBusTitleClicked.text = busTitleText
            scheduleBusDetailsClicked.text = busDetailsText
        }

        override fun onLongClick(v: View?): Boolean {
            val position: Int = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
            return true
        }
    }

    interface OnScheduleItemClickListener {
        fun onItemClick(position: Int)
    }

    fun submitSchedulesList(schedules: List<Schedule>) {
        items = schedules
    }

    /**
     * Initializes values for the TextViews in the list item.
     */
    private fun initValues(schedule: Schedule, firstScheduleHours: Boolean) {
        busNumberText = "${schedule.busNumber}"

        if (!firstScheduleHours) {
            busTitleText = "${schedule.leavingHours1.fromStation} - ${schedule.leavingHours2.fromStation}"
            busDetailsText = when (GeneralUtils.getTodayDayType()) {
                Calendar.SATURDAY -> {
                    schedule.leavingHours1.saturdayLeavingHours.joinToString(" ")
                }
                Calendar.SUNDAY -> {
                    schedule.leavingHours1.sundayLeavingHours.joinToString(" ")
                }
                else -> {
                    schedule.leavingHours1.weekdayLeavingHours.joinToString(" ")
                }
            }
        } else {
            busTitleText = "${schedule.leavingHours2.fromStation} - ${schedule.leavingHours1.fromStation}"
            busDetailsText = when (GeneralUtils.getTodayDayType()) {
                Calendar.SATURDAY -> {
                    schedule.leavingHours2.saturdayLeavingHours.joinToString(" ")
                }
                Calendar.SUNDAY -> {
                    schedule.leavingHours2.sundayLeavingHours.joinToString(" ")
                }
                else -> {
                    schedule.leavingHours2.weekdayLeavingHours.joinToString(" ")
                }
            }
        }
    }
}