package de.simon.dankelmann.bluetoothlespam.ui.deviceselector

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Database.Dao.CollectionWithLists
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity

/**
 * Group Editor (plan §8) — pick lists from any existing collection (built-in or custom) to
 * build a new custom group. Selection state is keyed by list ID ([selectedListIds]), so the
 * same list reached through two different collections (e.g. Kitchen Sink reusing Fast Pair's
 * lists) is a single selection, not a duplicate.
 */
@Composable
fun GroupEditorScreen(
    groupName: String,
    onGroupNameChanged: (String) -> Unit,
    collections: List<CollectionWithLists>,
    selectedListIds: Set<Int>,
    onToggleCollection: (CollectionWithLists) -> Unit,
    onToggleList: (AdvertisementSetListEntity) -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = groupName,
            onValueChange = onGroupNameChanged,
            label = { Text("Group name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
        )

        val expandedCollections = remember { mutableStateMapOf<Int, Boolean>() }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(collections, key = { it.collection.id }) { collectionWithLists ->
                CollectionRow(
                    collectionWithLists = collectionWithLists,
                    selectedListIds = selectedListIds,
                    expanded = expandedCollections[collectionWithLists.collection.id] == true,
                    onToggleExpanded = {
                        val id = collectionWithLists.collection.id
                        expandedCollections[id] = expandedCollections[id] != true
                    },
                    onToggleCollection = { onToggleCollection(collectionWithLists) },
                    onToggleList = onToggleList,
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = groupName.isNotBlank() && selectedListIds.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Save Group")
        }
    }
}

@Composable
private fun CollectionRow(
    collectionWithLists: CollectionWithLists,
    selectedListIds: Set<Int>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleCollection: () -> Unit,
    onToggleList: (AdvertisementSetListEntity) -> Unit,
) {
    val listIds = collectionWithLists.lists.map { it.id }
    val selectedCount = listIds.count { it in selectedListIds }
    val state = when {
        selectedCount == 0 -> ToggleableState.Off
        selectedCount == listIds.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val checkedStateDescription = when (state) {
        ToggleableState.On -> "Selected"
        ToggleableState.Indeterminate -> "Partially selected"
        ToggleableState.Off -> "Not selected"
    }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .semantics {
                    contentDescription = checkedStateDescription
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(state = state, onClick = onToggleCollection)
            Text(
                text = collectionWithLists.collection.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }

        if (expanded) {
            collectionWithLists.lists.forEach { list ->
                val checked = list.id in selectedListIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp)
                        .clickable { onToggleList(list) }
                        .semantics { contentDescription = if (checked) "Selected" else "Not selected" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TriStateCheckbox(
                        state = if (checked) ToggleableState.On else ToggleableState.Off,
                        onClick = { onToggleList(list) },
                    )
                    Text(
                        text = list.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
