package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.LogDirectoryPicker
import de.simon.dankelmann.bluetoothlespam.R
import java.io.File

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
        loggingSwitch?.isChecked = LogFileManager.getInstance().isLoggingEnabledAndValid()
        loggingSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                logDirectoryPicker.pickDirectory { directory ->
                    LogFileManager.getInstance().setCustomLogDirectory(directory, requireContext())
                    LogFileManager.getInstance().initializeLogFile(requireContext())
                    loggingSwitch.isChecked = LogFileManager.getInstance().isLoggingEnabledAndValid()
                }
                false // Don't update switch until directory is selected
            } else {
                LogFileManager.getInstance().disableLogging(requireContext())
                true
            }
        }

        findPreference<Preference>(getString(R.string.preference_key_open_log_folder))?.setOnPreferenceClickListener {
            // Use LogFileManager to get the log directory
            val logDir = LogFileManager.getInstance().getLogDirectory(requireContext())
            logDir?.let { directory ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        directory
                    )
                    intent.setDataAndType(uri, "*/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Could not open log directory", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Log directory not available", Toast.LENGTH_SHORT).show()
            }
            true
        }
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