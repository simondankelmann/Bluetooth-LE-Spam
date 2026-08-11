package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.Helpers.LogDirectoryPicker
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModeOption

/**
 * Route entry point (plan §6 step 4) — owns the business logic `PreferencesFragment` used to
 * own. SAF folder picker kept via `rememberLauncherForActivityResult` (plan §5).
 */
@Composable
fun PreferencesRoute(onTxPowerClicked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    val logDirectoryPicker = remember { LogDirectoryPicker(activity) }
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> logDirectoryPicker.handleResult(uri) }
        }
    }
    remember(directoryPickerLauncher) {
        logDirectoryPicker.initialize(directoryPickerLauncher)
    }

    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepository.preferencesFlow.collectAsState()

    val themeMode = ThemeModeOption.entries.find { it.prefValue == (settings[SettingsKeys.THEME_MODE] ?: "default") }
        ?: ThemeModeOption.DEVICE
    val seedColorArgb = settings[SettingsKeys.THEME_SEED_COLOR] ?: ThemeManager.THEME_SEED_COLOR_DEVICE
    val dynamicColorEnabled = settings[SettingsKeys.DYNAMIC_COLOR_ENABLED] ?: true
    val blurEnabled = settings[SettingsKeys.BLUR_ENABLED] ?: true

    val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val legacyAdvertisingKey = context.getString(R.string.preference_key_use_legacy_advertising)
    val intervalKey = context.getString(R.string.preference_key_interval_advertising_queue_handler)
    var useLegacyAdvertising by remember { mutableStateOf(defaultPrefs.getBoolean(legacyAdvertisingKey, true)) }
    var advertisingIntervalMs by remember { mutableStateOf(defaultPrefs.getString(intervalKey, "1000") ?: "1000") }
    var loggingEnabled by remember { mutableStateOf(LogFileManager.getInstance(context).isLoggingEnabledAndValid()) }

    PreferencesScreen(
        themeMode = themeMode,
        onThemeModeSelected = { option -> ThemeManager.getInstance().setTheme(context, option.prefValue) },
        seedColorArgb = seedColorArgb,
        onSeedColorSelected = { argb -> ThemeManager.getInstance().setSeedColor(context, argb) },
        dynamicColorEnabled = dynamicColorEnabled,
        onDynamicColorEnabledChanged = { enabled -> ThemeManager.getInstance().setDynamicColorEnabled(context, enabled) },
        blurEnabled = blurEnabled,
        onBlurEnabledChanged = { enabled -> ThemeManager.getInstance().setBlurEnabled(context, enabled) },
        useLegacyAdvertising = useLegacyAdvertising,
        onUseLegacyAdvertisingChanged = { enabled ->
            useLegacyAdvertising = enabled
            defaultPrefs.edit().putBoolean(legacyAdvertisingKey, enabled).apply()
        },
        advertisingIntervalMs = advertisingIntervalMs,
        onAdvertisingIntervalChanged = { value ->
            advertisingIntervalMs = value
            defaultPrefs.edit().putString(intervalKey, value).apply()
        },
        onTxPowerClicked = onTxPowerClicked,
        loggingEnabled = loggingEnabled,
        onLoggingEnabledChanged = { enabled ->
            if (enabled) {
                logDirectoryPicker.pickDirectory { directory ->
                    LogFileManager.getInstance(context).setCustomLogDirectory(directory, context)
                    LogFileManager.getInstance(context).initializeLogFile(context)
                    loggingEnabled = LogFileManager.getInstance(context).isLoggingEnabledAndValid()
                }
            } else {
                LogFileManager.getInstance(context).disableLogging(context)
                loggingEnabled = false
            }
        },
    )
}
