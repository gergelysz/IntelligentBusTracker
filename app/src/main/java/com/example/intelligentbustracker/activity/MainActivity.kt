package com.example.intelligentbustracker.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.intelligentbustracker.R
import kotlinx.android.synthetic.main.activity_main.load_map_button
import kotlinx.android.synthetic.main.activity_main.schedules_button
import kotlinx.android.synthetic.main.activity_main.settings_button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        load_map_button.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        settings_button.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        schedules_button.setOnClickListener {
            startActivity(Intent(this, SchedulesActivity::class.java))
        }
    }
}