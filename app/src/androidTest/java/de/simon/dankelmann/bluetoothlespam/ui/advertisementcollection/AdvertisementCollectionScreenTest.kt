package de.simon.dankelmann.bluetoothlespam.ui.advertisementcollection

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvertisementCollectionScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun populatedState_rendersEachCollectionCard() {
        composeTestRule.setContent {
            SpecterTheme {
                AdvertisementCollectionScreen(
                    description = "Select a collection and start BLE advertising",
                    premadeItems = listOf(
                        AdvertisementCollectionItem(
                            title = "Fast Pair",
                            targetLabel = "Target: Android",
                            distanceLabel = "Distance: Close",
                            iconRes = R.drawable.ic_android,
                            onClick = {},
                        ),
                    ),
                    customItems = emptyList(),
                    onAddClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Fast Pair").assertExists()
        composeTestRule.onNodeWithText("Target: Android").assertExists()
    }

    @Test
    fun emptyState_showsOnlyDescription_noCollectionCards() {
        composeTestRule.setContent {
            SpecterTheme {
                AdvertisementCollectionScreen(
                    description = "Select a collection and start BLE advertising",
                    premadeItems = emptyList(),
                    customItems = emptyList(),
                    onAddClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Select a collection and start BLE advertising").assertExists()
        composeTestRule.onNodeWithText("Fast Pair").assertDoesNotExist()
    }

    @Test
    fun customGroups_hidden_whenNoneExist() {
        composeTestRule.setContent {
            SpecterTheme {
                AdvertisementCollectionScreen(
                    description = "Select a collection and start BLE advertising",
                    premadeItems = emptyList(),
                    customItems = emptyList(),
                    onAddClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Custom Groups").assertDoesNotExist()
    }

    @Test
    fun customGroups_shown_whenPresent() {
        composeTestRule.setContent {
            SpecterTheme {
                AdvertisementCollectionScreen(
                    description = "Select a collection and start BLE advertising",
                    premadeItems = emptyList(),
                    customItems = listOf(
                        AdvertisementCollectionItem(
                            title = "My Group",
                            targetLabel = "Target: Mixed",
                            distanceLabel = "Distance: Mixed",
                            iconRes = R.drawable.ic_android,
                            onClick = {},
                        ),
                    ),
                    onAddClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Custom Groups").assertExists()
        composeTestRule.onNodeWithText("My Group").assertExists()
    }

    @Test
    fun addFab_invokesCallback_onClick() {
        var clicked = false
        composeTestRule.setContent {
            SpecterTheme {
                AdvertisementCollectionScreen(
                    description = "Select a collection and start BLE advertising",
                    premadeItems = emptyList(),
                    customItems = emptyList(),
                    onAddClicked = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Choose what to advertise").performClick()

        org.junit.Assert.assertTrue(clicked)
    }
}
