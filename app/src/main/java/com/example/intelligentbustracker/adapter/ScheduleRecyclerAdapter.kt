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

    inner class ScheduleViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val scheduleBusNumber: TextView = itemView.schedule_bus_number
        private val scheduleBusTitle: TextView = itemView.schedule_bus_title
        private val scheduleBusDetails: TextView = itemView.schedule_bus_details

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(schedule: Schedule) {
            scheduleBusNumber.text = "${schedule.busNumber}"
            scheduleBusTitle.text = "${schedule.leavingHours1.fromStation} - ${schedule.leavingHours2.fromStation}"
            when (GeneralUtils.getTodayDayType()) {
                Calendar.SATURDAY -> {
                    scheduleBusDetails.text = schedule.leavingHours1.saturdayLeavingHours.joinToString(" ")
                }
                Calendar.SUNDAY -> {
                    scheduleBusDetails.text = schedule.leavingHours1.sundayLeavingHours.joinToString(" ")
                }
                else -> {
                    scheduleBusDetails.text = schedule.leavingHours1.weekdayLeavingHours.joinToString(" ")
                }
            }
        }

        override fun onClick(v: View?) {
            val position: Int = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }

    interface OnScheduleItemClickListener {
        fun onItemClick(position: Int)
    }

    fun submitSchedulesList(schedules: List<Schedule>) {
        items = schedules
    }
}