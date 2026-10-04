package de.simon.dankelmann.bluetoothlespam.Datastore

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CRITICAL regression test (plan §11 REGRESSION RULE) — existing users' `theme_mode`/
 * `theme_seed_color` SharedPreferences values must survive the move to DataStore unchanged.
 *
 * Uses a plain [Application] (not the manifest's `BleSpamApplication`) — `BleSpamApplication.onCreate`
 * calls `SettingsRepository.warmUp()` before this test gets a chance to seed the legacy
 * SharedPreferences values it's trying to test the migration of.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31], application = Application::class)
class SettingsMigrationTest {

    @Test
    fun `existing theme_mode and theme_seed_color survive migration to DataStore`() {
        val context: Context = ApplicationProvider.getApplicationContext()

        // Seed legacy SharedPreferences exactly as a real pre-upgrade install would have them
        // (written via the old ThemeManager, before this migration existed). .commit() (not
        // .apply()) — Robolectric's .apply() shadow completes asynchronously.
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("theme_mode", "oled")
            .putInt("theme_seed_color", 0xFF112233.toInt())
            .commit()

        val repository = SettingsRepository.getInstance(context)
        repository.warmUp()

        assertEquals("oled", repository.current[SettingsKeys.THEME_MODE])
        assertEquals(0xFF112233.toInt(), repository.current[SettingsKeys.THEME_SEED_COLOR])
    }
}
