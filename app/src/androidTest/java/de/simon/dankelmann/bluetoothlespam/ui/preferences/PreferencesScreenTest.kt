package de.simon.dankelmann.bluetoothlespam.ui.preferences

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModeOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun togglingDynamicColorSwitch_invokesCallbackWithFlippedValue() {
        var dynamicColorEnabled = true
        var callbackValue: Boolean? = null

        composeTestRule.setContent {
            SpecterTheme {
                PreferencesScreen(
                    themeMode = ThemeModeOption.DEVICE,
                    onThemeModeSelected = {},
                    seedColorArgb = 0,
                    onSeedColorSelected = {},
                    dynamicColorEnabled = dynamicColorEnabled,
                    onDynamicColorEnabledChanged = { callbackValue = it },
                    blurEnabled = true,
                    onBlurEnabledChanged = {},
                    useLegacyAdvertising = true,
                    onUseLegacyAdvertisingChanged = {},
                    advertisingIntervalMs = "1000",
                    onAdvertisingIntervalChanged = {},
                    onTxPowerClicked = {},
                    advertisingBackgroundEnabled = false,
                    onAdvertisingBackgroundEnabledChanged = {},
                    spamDetectionBackgroundEnabled = false,
                    onSpamDetectionBackgroundEnabledChanged = {},
                    loggingEnabled = false,
                    onLoggingEnabledChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Dynamic Color").performClick()

        assert(callbackValue == false) { "expected toggling ON->OFF, got $callbackValue" }
    }

    @Test
    fun txPowerRow_invokesCallback_onClick() {
        var clicked = false

        composeTestRule.setContent {
            SpecterTheme {
                PreferencesScreen(
                    themeMode = ThemeModeOption.DEVICE,
                    onThemeModeSelected = {},
                    seedColorArgb = 0,
                    onSeedColorSelected = {},
                    dynamicColorEnabled = true,
                    onDynamicColorEnabledChanged = {},
                    blurEnabled = true,
                    onBlurEnabledChanged = {},
                    useLegacyAdvertising = true,
                    onUseLegacyAdvertisingChanged = {},
                    advertisingIntervalMs = "1000",
                    onAdvertisingIntervalChanged = {},
                    onTxPowerClicked = { clicked = true },
                    advertisingBackgroundEnabled = false,
                    onAdvertisingBackgroundEnabledChanged = {},
                    spamDetectionBackgroundEnabled = false,
                    onSpamDetectionBackgroundEnabledChanged = {},
                    loggingEnabled = false,
                    onLoggingEnabledChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Set TX Power").performClick()

        assert(clicked)
    }
}
