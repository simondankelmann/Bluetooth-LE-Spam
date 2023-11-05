package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.simon.dankelmann.bluetoothlespam.R

class PreferencesFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}