package com.example.intelligentbustracker.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.intelligentbustracker.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // The line below is used to add preference
        // fragment from our 'xml' folder.
        addPreferencesFromResource(R.xml.preferences)
    }
}