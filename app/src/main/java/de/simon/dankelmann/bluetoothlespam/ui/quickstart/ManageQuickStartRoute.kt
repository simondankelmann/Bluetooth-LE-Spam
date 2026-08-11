package de.simon.dankelmann.bluetoothlespam.ui.quickstart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Route entry point -- loads premade/custom groups and persists Quick Start checkbox toggles. */
@Composable
fun ManageQuickStartRoute() {
    val scope = rememberCoroutineScope()
    var premadeGroups by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }
    var customGroups by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance().advertisementSetCollectionDao()
            premadeGroups = dao.getBuiltInCollections()
            customGroups = dao.getCustomCollections()
        }
    }

    fun toggle(item: QuickStartPickerItem) {
        val newChecked = !item.isQuickStart
        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance().advertisementSetCollectionDao()
            // Manual order (plan feedback), not recency: next free slot when checked, cleared when unchecked.
            val newOrder = if (newChecked) (dao.getMaxQuickStartOrder() ?: -1) + 1 else null
            dao.setQuickStart(item.id, newChecked, newOrder)

            withContext(Dispatchers.Main) {
                fun updated(groups: List<AdvertisementSetCollectionEntity>) = groups.map {
                    if (it.id == item.id) it.copy(isQuickStart = newChecked, quickStartOrder = newOrder) else it
                }
                premadeGroups = updated(premadeGroups)
                customGroups = updated(customGroups)
            }
        }
    }

    ManageQuickStartScreen(
        premadeGroups = premadeGroups.map { QuickStartPickerItem(it.id, it.title, it.isQuickStart) },
        customGroups = customGroups.map { QuickStartPickerItem(it.id, it.title, it.isQuickStart) },
        onToggle = ::toggle,
    )
}
