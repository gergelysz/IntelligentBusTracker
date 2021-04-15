package com.example.intelligentbustracker.fragment

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // The line below is used to add preference
        // fragment from our 'xml' folder.
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this@SettingsFragment)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this@SettingsFragment)
    }

    /**
     * Listens to changes applies in Settings.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "key_location_update_interval" -> {
                BusTrackerApplication.updateInterval = sharedPreferences!!.getString(key, "5000") ?: "5000"
            }
            "key_location_update_accuracy" -> {
                BusTrackerApplication.updateAccuracy = sharedPreferences!!.getString(key, "100") ?: "100"
            }
            "key_focus_map_center_on_current_location" -> {
                BusTrackerApplication.focusOnCenter = sharedPreferences!!.getBoolean(key, false).toString()
            }
            "key_map_theme" -> {
                BusTrackerApplication.mapTheme.value = sharedPreferences!!.getString(key, "map_style_retro") ?: "map_style_retro"
            }
            "key_intelligent_bus_track" -> {
                BusTrackerApplication.intelligentTracker.value = sharedPreferences!!.getBoolean(key, true).toString()
            }
            "key_ask_location_change" -> {
                BusTrackerApplication.askLocationChange = sharedPreferences!!.getBoolean(key, true).toString()
            }
        }
    }
}