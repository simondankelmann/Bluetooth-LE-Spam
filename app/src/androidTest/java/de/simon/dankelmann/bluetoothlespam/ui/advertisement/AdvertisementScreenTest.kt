package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvertisementScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun buildLists(): List<AdvertisementSetList> {
        val set = AdvertisementSet().apply { id = 1; title = "Set One" }
        val list = AdvertisementSetList().apply { title = "Group One"; advertisementSets = mutableListOf(set) }
        return listOf(list)
    }

    private fun setContent(lists: List<AdvertisementSetList>) {
        composeTestRule.setContent {
            SpecterTheme {
                AdvertisementScreen(
                    isAdvertising = false,
                    target = AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED,
                    collectionTitle = "Collection",
                    collectionSubtitle = "Subtitle",
                    collectionHint = "-",
                    currentSetTitle = "-",
                    currentSetSubtitle = "-",
                    queueMode = AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM,
                    isLoadingSets = false,
                    advertisementSetLists = lists,
                    revision = 0,
                    onPlayClicked = {},
                    onQueueModeSelected = {},
                    onSetRowClicked = { _, _, _ -> },
                    onSetCheckedChanged = { _, _ -> },
                    onGroupCheckedChanged = { _, _ -> },
                    allowCustomSwiftPairNames = false,
                    onRenameSwiftPairDevice = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun groupHeader_expandsToRevealChildSet_onClick() {
        setContent(buildLists())

        composeTestRule.onNodeWithText("Set One").assertDoesNotExist()

        composeTestRule.onNodeWithText("Group One").performClick()

        composeTestRule.onNodeWithText("Set One").assertExists()
    }

    @Test
    fun groupHeader_hasStateDescription_reflectingExpandedState() {
        setContent(buildLists())

        val collapsedMatcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed")
        composeTestRule.onNode(collapsedMatcher).assertExists()

        composeTestRule.onNodeWithText("Group One").performClick()

        val expandedMatcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded")
        composeTestRule.onNode(expandedMatcher).assertExists()
    }
}
