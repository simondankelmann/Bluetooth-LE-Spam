package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.LogDirectoryPicker
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager.Companion.THEME_MODE_KEY
import de.simon.dankelmann.bluetoothlespam.R

class PreferencesFragment : PreferenceFragmentCompat(), MenuProvider {

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

        // Set up theme mode preference
        val themePreference = findPreference<ListPreference>(THEME_MODE_KEY)
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val themeMode = newValue as String
            ThemeManager.getInstance().setTheme(requireContext(), themeMode)

            // If the system is in dark mode, then when switching between FOLLOW_SYSTEM and DARK,
            // the fragment will not be recreated. Same for light mode.
            // Thus we need to manually update this.
            themePreference.summary = ThemeManager.getInstance().getThemeString(requireContext())

            true
        }
        themePreference?.summary = ThemeManager.getInstance().getThemeString(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        //menuInflater.inflate(R.menu.main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

        val menuItems = listOf<MenuItem?>(
            menu.findItem(R.id.nav_preferences),
            menu.findItem(R.id.nav_set_tx_power)
        )

        val context = requireContext()

        menuItems.forEach { menuItem ->
            val actionSettingsMenuItem = menuItem
            val title = actionSettingsMenuItem?.title.toString()
            val spannable = SpannableString(title)

            var textColor = resources.getColor(R.color.text_color, context.theme)

            if (menuItem?.itemId == R.id.nav_preferences) {
                textColor = resources.getColor(R.color.text_color_light, context.theme)
                menuItem.isEnabled = false
            }

            spannable.setSpan(
                ForegroundColorSpan(textColor),
                0,
                spannable.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionSettingsMenuItem?.title = spannable
        }
    }
}