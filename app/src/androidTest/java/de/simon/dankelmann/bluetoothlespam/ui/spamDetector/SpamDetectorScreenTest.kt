package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpamDetectorScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(isDetecting: Boolean, flipperDevices: List<FlipperDeviceScanResult> = emptyList()) {
        composeTestRule.setContent {
            SpecterTheme {
                SpamDetectorScreen(
                    isDetecting = isDetecting,
                    flipperDevices = flipperDevices,
                    spamPackages = emptyList(),
                    onToggleClicked = {},
                )
            }
        }
    }

    @Test
    fun idleState_showsZeroCounts_noIdleOrScanningIndicator() {
        setContent(isDetecting = false)

        composeTestRule.onNodeWithText("Idle").assertDoesNotExist()
        composeTestRule.onNodeWithText("Scanning…").assertDoesNotExist()
        composeTestRule.onNodeWithText("Detected Devices (0)").assertExists()
        composeTestRule.onNodeWithText("Spam Packages (0)").assertExists()
    }

    @Test
    fun foundState_showsCountInSectionHeader() {
        val device = FlipperDeviceScanResult().apply { address = "AA:BB:CC:DD:EE:FF" }
        setContent(isDetecting = true, flipperDevices = listOf(device))

        composeTestRule.onNodeWithText("Detected Devices (1)").assertExists()
    }
}
