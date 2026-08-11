package de.simon.dankelmann.bluetoothlespam.ui.quickstart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A pickable row on the Manage Quick Start screen -- one per premade or custom group. */
data class QuickStartPickerItem(
    val id: Int,
    val title: String,
    val isQuickStart: Boolean,
)

/**
 * Lets the user check/uncheck which groups act as Quick Start shortcuts on the Info screen
 * (plan feedback) -- Quick Start is a manually pinned list, not the old recency-based one.
 */
@Composable
fun ManageQuickStartScreen(
    premadeGroups: List<QuickStartPickerItem>,
    customGroups: List<QuickStartPickerItem>,
    onToggle: (QuickStartPickerItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Choose which groups show up as Quick Start shortcuts",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item { SectionHeader("Premade Groups") }
        items(premadeGroups, key = { "premade-${it.id}" }) { item ->
            PickerRow(item = item, onToggle = { onToggle(item) })
        }

        if (customGroups.isNotEmpty()) {
            item { SectionHeader("Custom Groups") }
            items(customGroups, key = { "custom-${it.id}" }) { item ->
                PickerRow(item = item, onToggle = { onToggle(item) })
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
private fun PickerRow(item: QuickStartPickerItem, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.isQuickStart, onCheckedChange = { onToggle() })
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
