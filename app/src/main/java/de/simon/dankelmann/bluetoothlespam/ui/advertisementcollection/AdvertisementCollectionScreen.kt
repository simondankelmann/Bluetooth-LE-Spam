package de.simon.dankelmann.bluetoothlespam.ui.advertisementcollection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance

/** One selectable attack profile card on the Advertisement Collection screen. */
data class AdvertisementCollectionItem(
    val title: String,
    val targetLabel: String,
    val distanceLabel: String,
    val iconRes: Int,
    val onClick: () -> Unit,
)

@Composable
fun AdvertisementCollectionScreen(
    premadeItems: List<AdvertisementCollectionItem>,
    customItems: List<AdvertisementCollectionItem>,
    onAddClicked: () -> Unit,
) {
    var customExpanded by remember { mutableStateOf(true) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClicked,
                modifier = Modifier.padding(bottom = FloatingNavBarClearance),
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create custom group")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // Both MainActivity's title bar and the floating nav pill blur on top of content rather
        // than reserving space -- Scaffold's own default window-inset padding would stack an
        // extra gap on top of the explicit SpecterTopAppBarClearance/FloatingNavBarClearance
        // this screen's own contentPadding already adds below.
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Top/bottom padding include SpecterTopAppBarClearance/FloatingNavBarClearance so
            // the first/last card scrolls clear of the floating title bar and nav pill instead
            // of staying stuck underneath either one.
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp + SpecterTopAppBarClearance,
                bottom = 16.dp + FloatingNavBarClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(premadeItems) { item -> AdvertisementCollectionCard(item) }

            if (customItems.isNotEmpty()) {
                item {
                    CategoryHeaderRow(
                        title = "Custom Groups",
                        expanded = customExpanded,
                        onToggle = { customExpanded = !customExpanded },
                    )
                }
                if (customExpanded) {
                    items(customItems) { item -> AdvertisementCollectionCard(item) }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeaderRow(title: String, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun AdvertisementCollectionCard(item: AdvertisementCollectionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(text = item.targetLabel, style = MaterialTheme.typography.bodyMedium)
                Text(text = item.distanceLabel, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}
