package de.simon.dankelmann.bluetoothlespam.ui.start

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(viewModel: StartViewModel) {
        composeTestRule.setContent {
            SpecterTheme {
                StartScreen(
                    viewModel = viewModel,
                    onGrantPermissions = {},
                    onRecheckPermissions = {},
                    onRecheckBluetooth = {},
                    onRecheckDatabase = {},
                    quickStartCollections = emptyList(),
                    onQuickStartItemClicked = {},
                    onEditQuickStartClicked = {},
                )
            }
        }
    }

    @Test
    fun permissionRationaleCard_shown_whenPermissionsNotGranted() {
        val viewModel = StartViewModel().apply { allPermissionsGranted.value = false }
        setContent(viewModel)

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.missing_permissions_grant))
            .assertExists()
    }

    @Test
    fun permissionRationaleCard_hidden_whenPermissionsGranted() {
        val viewModel = StartViewModel().apply { allPermissionsGranted.value = true }
        setContent(viewModel)

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.missing_permissions_grant))
            .assertDoesNotExist()
    }

    @Test
    fun quickStart_showsEmptyState_whenNoQuickStartCollections() {
        setContent(StartViewModel())

        composeTestRule.onNodeWithText("No Quick Start groups yet — tap the pencil to add some").assertExists()
    }
}
