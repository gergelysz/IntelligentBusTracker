package com.example.intelligentbustracker.activity

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.fragment.SettingsFragment
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (settings_frame_layout != null) {
            if (savedInstanceState != null)
                return
            supportFragmentManager.beginTransaction()
                .add(R.id.settings_frame_layout, SettingsFragment())
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    /**
     * Listens to changes applies in Settings.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "key_location_update_interval" -> {
                MainActivity.updateInterval = sharedPreferences!!.getString(key, "5000") ?: "5000"
            }
            "key_location_update_accuracy" -> {
                MainActivity.updateAccuracy = sharedPreferences!!.getString(key, "100") ?: "100"
            }
            "key_focus_map_center_on_current_location" -> {
                MainActivity.focusOnCenter = sharedPreferences!!.getBoolean(key, false).toString()
            }
            "key_map_theme" -> {
                MainActivity.mapTheme = sharedPreferences!!.getString(key, "map_style_retro") ?: "map_style_retro"
            }
        }
    }
}