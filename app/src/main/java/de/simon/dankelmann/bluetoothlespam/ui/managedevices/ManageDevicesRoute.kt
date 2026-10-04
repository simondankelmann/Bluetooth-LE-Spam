package de.simon.dankelmann.bluetoothlespam.ui.managedevices

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RemovedItem(val id: Int, val set: AdvertisementSet, val isDefault: Boolean)
private data class ModifiedItem(val id: Int, val set: AdvertisementSet, val originalTitle: String, val isDefault: Boolean)

@Composable
fun ManageDevicesRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var removed by remember { mutableStateOf<List<RemovedItem>>(emptyList()) }
    var modified by remember { mutableStateOf<List<ModifiedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        loading = true
        scope.launch(Dispatchers.IO) {
            try {
                val defaults = DeviceCustomizationHelper.defaultSetsByKey()
                val removedList = mutableListOf<RemovedItem>()
                DeviceCustomizationHelper.getDeletedIds().forEach { id ->
                    val set = try { DatabaseHelpers.getAdvertisementSetById(id) } catch (e: Exception) { null }
                    if (set == null) {
                        DeviceCustomizationHelper.setDeleted(id, false)
                    } else {
                        removedList.add(RemovedItem(id, set, DeviceCustomizationHelper.isDefaultSet(set)))
                    }
                }
                val modifiedList = mutableListOf<ModifiedItem>()
                DeviceCustomizationHelper.getModifiedIds().forEach { id ->
                    val backupTitle = DeviceCustomizationHelper.getBackupTitle(id)
                    val set = try { DatabaseHelpers.getAdvertisementSetById(id) } catch (e: Exception) { null }
                    if (set == null) {
                        DeviceCustomizationHelper.clearBackup(id)
                        DeviceCustomizationHelper.setDeleted(id, false)
                    } else if (backupTitle == null) {
                        DeviceCustomizationHelper.clearBackup(id)
                    } else if (DeviceCustomizationHelper.matchesBackup(set)) {
                        DeviceCustomizationHelper.clearBackup(id)
                    } else {
                        val isDefault = defaults.values.any { it.type == set.type && it.title == backupTitle }
                        modifiedList.add(ModifiedItem(id, set, backupTitle, isDefault))
                    }
                }
                removedList.sortBy { it.set.title.lowercase() }
                modifiedList.sortBy { it.set.title.lowercase() }
                withContext(Dispatchers.Main) {
                    removed = removedList
                    modified = modifiedList
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { loading = false }
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    ManageDevicesScreen(
        removed = removed,
        modified = modified,
        loading = loading,
        onRestoreRemoved = { id ->
            scope.launch(Dispatchers.IO) {
                DeviceCustomizationHelper.setDeleted(id, false)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.manage_restored), Toast.LENGTH_SHORT).show()
                    load()
                }
            }
        },
        onRestoreModified = { id ->
            scope.launch(Dispatchers.IO) {
                DeviceCustomizationHelper.applyBackup(id)
                DeviceCustomizationHelper.clearBackup(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.manage_restored), Toast.LENGTH_SHORT).show()
                    load()
                }
            }
        },
        onResetAll = {
            try {
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.manage_reset_title))
                    .setMessage(context.getString(R.string.manage_reset_message))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        scope.launch(Dispatchers.IO) {
                            val result = DeviceCustomizationHelper.resetToDefaults()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${context.getString(R.string.manage_reset_done)} (+${result.readded}/-${result.removedCustoms}/~${result.restored})",
                                    Toast.LENGTH_LONG
                                ).show()
                                load()
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(context, "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
private fun ManageDevicesScreen(
    removed: List<RemovedItem>,
    modified: List<ModifiedItem>,
    loading: Boolean,
    onRestoreRemoved: (Int) -> Unit,
    onRestoreModified: (Int) -> Unit,
    onResetAll: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = SpecterTopAppBarClearance)
        ) {
            if (loading) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = 16.dp + FloatingNavBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionHeader("Removed devices") }
                if (removed.isEmpty()) {
                    item { Text("No removed devices", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(removed, key = { "removed-${it.id}" }) { item ->
                        DeviceRow(
                            title = item.set.title,
                            subtitle = typeLabel(item.set) + if (!item.isDefault) " · Custom" else "",
                            onRestore = { onRestoreRemoved(item.id) },
                        )
                    }
                }
                item { SectionHeader("Modified devices") }
                if (modified.isEmpty()) {
                    item { Text("No modified devices", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(modified, key = { "modified-${it.id}" }) { item ->
                        DeviceRow(
                            title = "${item.originalTitle} → ${item.set.title}",
                            subtitle = typeLabel(item.set) + if (!item.isDefault) " · Custom" else "",
                            onRestore = { onRestoreModified(item.id) },
                        )
                    }
                }
                item {
                    Button(
                        onClick = onResetAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) { Text("Reset all to defaults") }
                }
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
private fun DeviceRow(title: String, subtitle: String, onRestore: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRestore) { Text("Restore") }
        }
    }
}

@Composable
private fun typeLabel(set: AdvertisementSet): String {
    val context = LocalContext.current
    return try {
        context.getString(set.type.stringResId())
    } catch (e: Exception) {
        set.type.name
    }
}
