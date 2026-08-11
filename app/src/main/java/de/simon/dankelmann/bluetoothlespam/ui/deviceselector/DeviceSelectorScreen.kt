package de.simon.dankelmann.bluetoothlespam.ui.deviceselector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity

/**
 * Device Selector (plan §8) — 3 ways to pick what to advertise: an existing device list, a
 * built-in/custom group, or build a new custom group (-> Group Editor).
 */
@Composable
fun DeviceSelectorScreen(
    lists: List<AdvertisementSetListEntity>,
    builtInGroups: List<AdvertisementSetCollectionEntity>,
    customGroups: List<AdvertisementSetCollectionEntity>,
    onListSelected: (AdvertisementSetListEntity) -> Unit,
    onGroupSelected: (AdvertisementSetCollectionEntity) -> Unit,
    onCreateCustomGroupClicked: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredLists = if (query.isBlank()) lists else lists.filter { it.title.contains(query, ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader("Groups") }
        items(builtInGroups, key = { "built-in-${it.id}" }) { group ->
            SelectableRow(title = group.title, onClick = { onGroupSelected(group) })
        }
        if (customGroups.isNotEmpty()) {
            item { SectionHeader("Your Groups") }
            items(customGroups, key = { "custom-${it.id}" }) { group ->
                SelectableRow(title = group.title, onClick = { onGroupSelected(group) })
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateCustomGroupClicked),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Text(
                        text = "Create custom group",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }

        item { SectionHeader("Device Lists") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search lists") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (filteredLists.isEmpty()) {
            item {
                Text(
                    "No matching lists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            items(filteredLists, key = { "list-${it.id}" }) { list ->
                SelectableRow(title = list.title, onClick = { onListSelected(list) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SelectableRow(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
