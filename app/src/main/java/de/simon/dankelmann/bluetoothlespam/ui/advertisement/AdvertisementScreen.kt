package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.getDrawableId
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.LocalExtendedColors
import de.simon.dankelmann.bluetoothlespam.ui.theme.StatusBadge
import de.simon.dankelmann.bluetoothlespam.ui.theme.StatusTone

/**
 * Core migration of `AdvertisementFragment`'s content (plan §5/§5a) — flattened `LazyColumn`
 * replacing `ExpandableListView` (group headers toggle expanded state, `stateDescription`
 * semantics preserve the TalkBack expand/collapse announcement the old widget gave for free).
 *
 * [revision] has no semantic meaning — [advertisementSetLists] contains plain mutable model
 * objects (`AdvertisementSet.isChecked`/`.currentlyAdvertising`/`.advertisementState`) that the
 * Fragment mutates in place from service callbacks, mirroring the previous
 * `notifyDataSetChanged()` pattern. Bumping [revision] is the only way Compose learns to re-read
 * them.
 */
@Composable
fun AdvertisementScreen(
    isAdvertising: Boolean,
    target: AdvertisementTarget,
    collectionTitle: String,
    collectionSubtitle: String,
    collectionHint: String,
    currentSetTitle: String,
    currentSetSubtitle: String,
    queueMode: AdvertisementQueueMode,
    isLoadingSets: Boolean,
    advertisementSetLists: List<AdvertisementSetList>,
    revision: Int,
    onPlayClicked: () -> Unit,
    onQueueModeSelected: (AdvertisementQueueMode) -> Unit,
    onSetRowClicked: (groupIndex: Int, childIndex: Int, AdvertisementSet) -> Unit,
    onSetCheckedChanged: (AdvertisementSet, Boolean) -> Unit,
    onGroupCheckedChanged: (AdvertisementSetList, Boolean) -> Unit,
    allowCustomSwiftPairNames: Boolean,
    onRenameSwiftPairDevice: (AdvertisementSet, String) -> Unit,
) {
    val expandedGroups = remember(advertisementSetLists) {
        mutableStateMapOf<Int, Boolean>().apply {
            if (advertisementSetLists.size == 1) put(0, true)
        }
    }
    var editingSwiftPairSet by remember { mutableStateOf<AdvertisementSet?>(null) }

    // Surface (not a plain Column) so every Text/Icon below that doesn't set an explicit color
    // gets a real LocalContentColor instead of falling back to black -- MainActivity's root is a
    // bare Box/Column with no Surface of its own, and this screen never established one either,
    // so several rows read as black-on-black in dark mode (same root cause as the Group Editor
    // checkbox-label bug from earlier in this session).
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderSection(
            isAdvertising = isAdvertising,
            target = target,
            collectionTitle = collectionTitle,
            collectionSubtitle = collectionSubtitle,
            collectionHint = collectionHint,
            currentSetTitle = currentSetTitle,
            currentSetSubtitle = currentSetSubtitle,
            queueMode = queueMode,
            onPlayClicked = onPlayClicked,
            onQueueModeSelected = onQueueModeSelected,
        )

        if (isLoadingSets) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                advertisementSetLists.forEachIndexed { groupIndex, list ->
                    // Keys include `revision`: `set`/`list` are plain mutated objects (not Compose
                    // State), and LazyColumn gives each item its own recomposition scope that a
                    // `revision++` bump alone doesn't invalidate -- only a key change forces
                    // Compose to dispose+recreate the row and re-read the mutated field. Without
                    // this, toggling a checkbox silently updates the model but the checkbox glyph
                    // stays stale until the group is collapsed/expanded.
                    item(key = "group-$groupIndex-$revision") {
                        GroupHeaderRow(
                            list = list,
                            expanded = expandedGroups[groupIndex] == true,
                            onToggleExpanded = {
                                expandedGroups[groupIndex] = expandedGroups[groupIndex] != true
                            },
                            onGroupCheckedChanged = { checked -> onGroupCheckedChanged(list, checked) },
                        )
                    }

                    if (expandedGroups[groupIndex] == true) {
                        items(list.advertisementSets, key = { "set-$groupIndex-${it.id}-$revision" }) { set ->
                            val childIndex = list.advertisementSets.indexOf(set)
                            SetRow(
                                set = set,
                                onClick = { onSetRowClicked(groupIndex, childIndex, set) },
                                onCheckedChanged = { checked -> onSetCheckedChanged(set, checked) },
                                showEditButton = allowCustomSwiftPairNames &&
                                    set.type == AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING,
                                onEditClicked = { editingSwiftPairSet = set },
                            )
                        }
                    }
                }
            }
        }
    }
    }

    editingSwiftPairSet?.let { set ->
        RenameSwiftPairDeviceDialog(
            set = set,
            onConfirm = { newName ->
                onRenameSwiftPairDevice(set, newName)
                editingSwiftPairSet = null
            },
            onDismiss = { editingSwiftPairSet = null },
        )
    }
}

@Composable
private fun HeaderSection(
    isAdvertising: Boolean,
    target: AdvertisementTarget,
    collectionTitle: String,
    collectionSubtitle: String,
    collectionHint: String,
    currentSetTitle: String,
    currentSetSubtitle: String,
    queueMode: AdvertisementQueueMode,
    onPlayClicked: () -> Unit,
    onQueueModeSelected: (AdvertisementQueueMode) -> Unit,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.advertisingdevices))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isAdvertising,
    )

    Column(modifier = Modifier.padding(16.dp)) {
        if (isAdvertising) {
            StatusBadge(
                label = "Advertising",
                tone = StatusTone.WARNING,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        )

        Text(text = collectionTitle, style = MaterialTheme.typography.titleLarge)
        Text(text = collectionSubtitle, style = MaterialTheme.typography.bodyMedium)
        if (collectionHint == "-") {
            PlaceholderBox(modifier = Modifier.padding(top = 4.dp))
        } else {
            Text(text = collectionHint, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                IconButton(onClick = { onQueueModeSelected(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR) }) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Linear queue mode",
                        tint = if (queueMode == AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { onQueueModeSelected(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM) }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Random queue mode",
                        tint = if (queueMode == AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            FilledIconButton(onClick = onPlayClicked) {
                Icon(
                    imageVector = if (isAdvertising) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isAdvertising) "Stop advertising" else "Start advertising",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(target.getDrawableId()),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                if (currentSetTitle == "-") {
                    PlaceholderBox()
                } else {
                    Text(text = currentSetTitle, style = MaterialTheme.typography.titleSmall)
                }
                if (currentSetSubtitle == "-") {
                    PlaceholderBox(modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text(text = currentSetSubtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Small muted placeholder for a not-yet-available value, standing in for a bare "-" (plan feedback). */
@Composable
private fun PlaceholderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun GroupHeaderRow(
    list: AdvertisementSetList,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onGroupCheckedChanged: (Boolean) -> Unit,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")
    val checkedCount = list.advertisementSets.count { it.isChecked }
    val checkedState = when {
        checkedCount == 0 -> ToggleableState.Off
        checkedCount == list.advertisementSets.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val titleColor = if (list.currentlyAdvertising) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = checkedState,
            onClick = { onGroupCheckedChanged(checkedState != ToggleableState.On) },
        )
        Text(
            text = list.title,
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun SetRow(
    set: AdvertisementSet,
    onClick: () -> Unit,
    onCheckedChanged: (Boolean) -> Unit,
    showEditButton: Boolean,
    onEditClicked: () -> Unit,
) {
    val extendedColors = LocalExtendedColors.current
    val titleColor = when {
        set.currentlyAdvertising && set.advertisementState == AdvertisementState.ADVERTISEMENT_STATE_SUCCEEDED -> extendedColors.success
        set.currentlyAdvertising && set.advertisementState == AdvertisementState.ADVERTISEMENT_STATE_FAILED -> extendedColors.warning
        set.currentlyAdvertising -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = set.isChecked, onCheckedChange = onCheckedChanged)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(text = set.title, style = MaterialTheme.typography.bodyMedium, color = titleColor)
            Text(text = advertisementSetSubtitle(set), style = MaterialTheme.typography.bodySmall)
        }
        if (showEditButton) {
            IconButton(onClick = onEditClicked) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit device name")
            }
        }
    }
}

/** Lets the user set the advertised Swift Pair device name (Allow Custom Swift Pair Names setting). */
@Composable
private fun RenameSwiftPairDeviceDialog(
    set: AdvertisementSet,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // BLE legacy advertising packets top out at 31 bytes and Swift Pair's fixed framing already
    // uses some of that budget, so the name is capped well under the raw limit.
    val maxNameLength = 20
    var name by remember(set) { mutableStateOf(set.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= maxNameLength) name = it },
                label = { Text("Device name") },
                supportingText = { Text("${name.length}/$maxNameLength") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

internal fun advertisementSetSubtitle(advertisementSet: AdvertisementSet): String {
    val type = when (advertisementSet.type) {
        AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined"
        AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> "Fast Pairing Device"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> "Fast Pairing Phone Setup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> "Fast Pairing Non Production"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing Debug"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> "New Device Popup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> "Not your Device Popup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> "New Airtag Popup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "iOS Action Modal"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> "iOS 17 Crash"
        AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> "Easy Setup Watch"
        AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> "Easy Setup Buds"
        AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY -> "Lovespouse Play"
        AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP -> "Lovespouse Stop"
    }

    val range = when (advertisementSet.range) {
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE -> "Close"
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_MEDIUM -> "Medium"
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_FAR -> "Far"
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN -> "Unknown"
    }

    return "Type: $type, Range: $range"
}
