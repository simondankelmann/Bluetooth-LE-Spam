package de.simon.dankelmann.bluetoothlespam.Datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Preference keys migrated here MUST keep the exact string values they had as
 * `PreferenceManager.getDefaultSharedPreferences` keys (see [ThemeManager] / `preferences.xml`
 * history) — [SettingsRepository]'s migration matches on these literal names.
 */
object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
    val ALLOW_CUSTOM_SWIFT_PAIR_NAMES = booleanPreferencesKey("allow_custom_swift_pair_names")
}

/**
 * DataStore-backed settings store (plan §2/§11 — theme mode, seed color, dynamic color, blur).
 *
 * `use_legacy_advertising`/advertising-interval/logging settings deliberately stay on
 * `PreferenceManager.getDefaultSharedPreferences` — those are read synchronously on the BLE
 * advertising hot path ([de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeAdvertisementService],
 * [de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers]) and logging already has its own
 * independent SharedPreferences-backed store in `LogFileManager`; moving either onto DataStore's
 * async model was not worth the regression risk for a UI-framework migration.
 *
 * [current] gives synchronous access to the latest snapshot for call sites (e.g. [ThemeManager])
 * that historically read SharedPreferences synchronously — backed by a [StateFlow] that is
 * warmed with a one-time blocking read (which also runs the legacy-value migration, see
 * [migrateFromLegacySharedPreferencesIfNeeded]) in [warmUp], called once from
 * `BleSpamApplication.onCreate` before any UI reads it, so callers never see stale defaults.
 */
class SettingsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(appContext.filesDir, "datastore/specter_settings.preferences_pb") },
    )

    private val initialSnapshot: Preferences by lazy {
        runBlocking { migrateFromLegacySharedPreferencesIfNeeded() }
    }

    val preferencesFlow: StateFlow<Preferences> by lazy {
        dataStore.data.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = initialSnapshot,
        )
    }

    /** Synchronous snapshot for legacy (non-suspend) call sites — see class doc. */
    val current: Preferences get() = preferencesFlow.value

    /** Forces the one-time blocking load (and migration) to happen up front. Call once at process start. */
    fun warmUp() {
        preferencesFlow
    }

    /**
     * Runs once, the first time this DataStore is ever read: if it has no theme_mode/
     * theme_seed_color of its own yet, carries over whatever a pre-upgrade install had in the
     * classic `PreferenceManager.getDefaultSharedPreferences` file (plan §11 REGRESSION RULE —
     * existing users must not lose these on upgrade). No-op forever after the first real write.
     */
    private suspend fun migrateFromLegacySharedPreferencesIfNeeded(): Preferences {
        val existing = dataStore.data.first()
        if (existing.contains(SettingsKeys.THEME_MODE) || existing.contains(SettingsKeys.THEME_SEED_COLOR)) {
            return existing
        }

        val legacyPrefs = appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
        val legacyThemeMode = legacyPrefs.getString(SettingsKeys.THEME_MODE.name, null)
        val legacySeedColor = if (legacyPrefs.contains(SettingsKeys.THEME_SEED_COLOR.name)) {
            legacyPrefs.getInt(SettingsKeys.THEME_SEED_COLOR.name, 0)
        } else {
            null
        }

        if (legacyThemeMode == null && legacySeedColor == null) return existing

        return dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                legacyThemeMode?.let { this[SettingsKeys.THEME_MODE] = it }
                legacySeedColor?.let { this[SettingsKeys.THEME_SEED_COLOR] = it }
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode }
    }

    suspend fun setThemeSeedColor(argb: Int) {
        dataStore.edit { it[SettingsKeys.THEME_SEED_COLOR] = argb }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setBlurEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.BLUR_ENABLED] = enabled }
    }

    suspend fun setAllowCustomSwiftPairNamesEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.ALLOW_CUSTOM_SWIFT_PAIR_NAMES] = enabled }
    }

    /** Fire-and-forget variants for non-suspend call sites (e.g. [ThemeManager]) — launched on
     * this repository's own long-lived [scope] rather than each caller creating its own. */
    fun setThemeModeAsync(mode: String) {
        scope.launch { setThemeMode(mode) }
    }

    fun setThemeSeedColorAsync(argb: Int) {
        scope.launch { setThemeSeedColor(argb) }
    }

    fun setDynamicColorEnabledAsync(enabled: Boolean) {
        scope.launch { setDynamicColorEnabled(enabled) }
    }

    fun setBlurEnabledAsync(enabled: Boolean) {
        scope.launch { setBlurEnabled(enabled) }
    }

    fun setAllowCustomSwiftPairNamesEnabledAsync(enabled: Boolean) {
        scope.launch { setAllowCustomSwiftPairNamesEnabled(enabled) }
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
