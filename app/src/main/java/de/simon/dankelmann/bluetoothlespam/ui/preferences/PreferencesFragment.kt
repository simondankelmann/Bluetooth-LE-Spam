package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceFragmentCompat
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.LogDirectoryPicker
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.R

class PreferencesFragment : PreferenceFragmentCompat() {

    private val _logTag = "PreferencesFragment"
    private lateinit var directoryPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var logDirectoryPicker: LogDirectoryPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logDirectoryPicker = LogDirectoryPicker(requireActivity())
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
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Set up logging switch listener
        val loggingSwitch = findPreference<androidx.preference.SwitchPreferenceCompat>(getString(R.string.preference_key_enable_logging))
        loggingSwitch?.isChecked = LogFileManager.getInstance(requireContext()).isLoggingEnabledAndValid()
        loggingSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                // Check if we have any accessible directories first
                val accessibleDirs = LogFileManager.getInstance(requireContext()).listAccessibleDirectories(requireContext())
                Log.d(_logTag, "Available accessible directories: ${accessibleDirs.size}")

                logDirectoryPicker.pickDirectory { directory ->
                    LogFileManager.getInstance(requireContext()).setCustomLogDirectory(directory, requireContext())
                    LogFileManager.getInstance(requireContext()).initializeLogFile(requireContext())
                    loggingSwitch.isChecked = LogFileManager.getInstance(requireContext()).isLoggingEnabledAndValid()
                }
                false // Don't update switch until directory is selected
            } else {
                LogFileManager.getInstance(requireContext()).disableLogging(requireContext())
                true
            }
        }

        // Theme mode is now the segmented ThemeModePickerPreference, which manages
        // ThemeManager.setTheme() itself in onBindViewHolder — no listener needed here.

        // TX power dialog moved here from the (now-removed) toolbar overflow menu.
        findPreference<androidx.preference.Preference>("tx_power")?.setOnPreferenceClickListener {
            (activity as? MainActivity)?.showSetTxPowerDialog()
            true
        }
    }
}
