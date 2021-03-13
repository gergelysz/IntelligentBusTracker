package com.example.intelligentbustracker.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.ScheduleRecyclerAdapter
import com.example.intelligentbustracker.fragment.ScheduleMapFragment
import com.example.intelligentbustracker.model.Schedule
import kotlinx.android.synthetic.main.activity_schedules.schedules_recycler_view

class SchedulesActivity : AppCompatActivity(), ScheduleRecyclerAdapter.OnScheduleItemClickListener {

    private lateinit var scheduleAdapter: ScheduleRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedules)
        initRecyclerView()
        addDataSet()
    }

    private fun initRecyclerView() {
        schedules_recycler_view.apply {
            layoutManager = LinearLayoutManager(this@SchedulesActivity)
            scheduleAdapter = ScheduleRecyclerAdapter(this@SchedulesActivity)
            adapter = scheduleAdapter
        }
    }

    private fun addDataSet() {
        scheduleAdapter.submitSchedulesList(BusTrackerApplication.schedules)
    }

    override fun onItemClick(position: Int) {
        val clickedItem: Schedule = BusTrackerApplication.schedules[position]
        val scheduleMapFragment = ScheduleMapFragment()
        val bundle = Bundle()
        bundle.putString("bus_number", clickedItem.busNumber)
        scheduleMapFragment.arguments = bundle
        scheduleMapFragment.show(supportFragmentManager, "Schedule Map")
        scheduleAdapter.notifyItemChanged(position)
    }
}