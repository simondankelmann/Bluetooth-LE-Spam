package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
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
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
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

    // Android denies ACCESS_BACKGROUND_LOCATION without ever showing a dialog if the app doesn't
    // already hold foreground location — so on Q/R we have to request foreground location first,
    // wait for that result, and only then follow up with the background request. Using the
    // Activity Result API (rather than raw ActivityCompat.requestPermissions, which has no
    // completion callback in this codebase) is what makes that chaining possible.
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Preferences doesn't show inline permission state; next Start-screen resume reflects it. */ }
    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepository.preferencesFlow.collectAsState()

    val themeMode = ThemeModeOption.entries.find { it.prefValue == (settings[SettingsKeys.THEME_MODE] ?: "default") }
        ?: ThemeModeOption.DEVICE
    val seedColorArgb = settings[SettingsKeys.THEME_SEED_COLOR] ?: ThemeManager.THEME_SEED_COLOR_DEVICE
    val dynamicColorEnabled = settings[SettingsKeys.DYNAMIC_COLOR_ENABLED] ?: true
    val blurEnabled = settings[SettingsKeys.BLUR_ENABLED] ?: true
    val allowCustomSwiftPairNames = settings[SettingsKeys.ALLOW_CUSTOM_SWIFT_PAIR_NAMES] ?: false
    val spamDetectionBackgroundEnabled = settings[SettingsKeys.SPAM_DETECTION_BACKGROUND_ENABLED] ?: false

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
        allowCustomSwiftPairNames = allowCustomSwiftPairNames,
        onAllowCustomSwiftPairNamesChanged = { enabled ->
            settingsRepository.setAllowCustomSwiftPairNamesEnabledAsync(enabled)
        },
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
        spamDetectionBackgroundEnabled = spamDetectionBackgroundEnabled,
        onSpamDetectionBackgroundEnabledChanged = { enabled ->
            settingsRepository.setSpamDetectionBackgroundEnabledAsync(enabled)
            if (enabled) requestBackgroundLocationPermissionIfNeeded(context, foregroundLocationLauncher, backgroundLocationLauncher)
        },
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

/**
 * ACCESS_BACKGROUND_LOCATION only exists as a concept on Q/R (S+ doesn't need it at all, see
 * [PermissionCheck.getAllRelevantPermissions]) — asked for immediately when a background switch
 * is flipped on, rather than upfront on the Start screen. On Q/R, Android silently denies a
 * background-location request with no dialog at all unless the app already holds foreground
 * location, so that has to be requested (and granted) first.
 */
private fun requestBackgroundLocationPermissionIfNeeded(
    context: Context,
    foregroundLocationLauncher: ManagedActivityResultLauncher<String, Boolean>,
    backgroundLocationLauncher: ManagedActivityResultLauncher<String, Boolean>,
) {
    if (Build.VERSION.SDK_INT !in Build.VERSION_CODES.Q until Build.VERSION_CODES.S) return

    val hasForegroundLocation = PermissionCheck.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, context) ||
        PermissionCheck.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, context)

    if (hasForegroundLocation) {
        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        foregroundLocationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
