package de.simon.dankelmann.bluetoothlespam

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import de.simon.dankelmann.bluetoothlespam.Helpers.LogDirectoryPicker
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val logFileManager = LogFileManager.getInstance(requireContext())
        private lateinit var logDirectoryPicker: LogDirectoryPicker
        private lateinit var directoryPickerLauncher: ActivityResultLauncher<Intent>

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            logDirectoryPicker = LogDirectoryPicker(requireActivity())
            logFileManager.initialize(requireContext())
            directoryPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        logDirectoryPicker.handleResult(uri)
                    }
                }
            }
            logDirectoryPicker.initialize(directoryPickerLauncher)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<SwitchPreferenceCompat>("enable_logging")?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    logDirectoryPicker.pickDirectory { directory ->
                        logFileManager.setCustomLogDirectory(directory, requireContext())
                        logFileManager.initializeLogFile(requireContext())
                    }
                } else {
                    logFileManager.disableLogging(requireContext())
                }
                true
            }
        }
    }
}