package de.simon.dankelmann.bluetoothlespam.ui.deviceselector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Dao.CollectionWithLists
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociatonCollectionListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Route entry point (plan §8) — builds+saves a custom [AdvertisementSetCollectionEntity]. */
@Composable
fun GroupEditorRoute(onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var collections by remember { mutableStateOf<List<CollectionWithLists>>(emptyList()) }
    var groupName by remember { mutableStateOf("") }
    var selectedListIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            collections = AppDatabase.getInstance().advertisementSetCollectionDao().getAllCollectionsWithLists()
        }
    }

    GroupEditorScreen(
        groupName = groupName,
        onGroupNameChanged = { groupName = it },
        collections = collections,
        selectedListIds = selectedListIds,
        onToggleCollection = { collectionWithLists ->
            val listIds = collectionWithLists.lists.map { it.id }.toSet()
            selectedListIds = if (listIds.all { it in selectedListIds }) {
                selectedListIds - listIds
            } else {
                selectedListIds + listIds
            }
        },
        onToggleList = { list: AdvertisementSetListEntity ->
            selectedListIds = if (list.id in selectedListIds) {
                selectedListIds - list.id
            } else {
                selectedListIds + list.id
            }
        },
        onSave = {
            val name = groupName
            val listIds = selectedListIds
            scope.launch(Dispatchers.IO) {
                val database = AppDatabase.getInstance()
                val collectionId = database.advertisementSetCollectionDao().insertItem(
                    AdvertisementSetCollectionEntity(id = 0, title = name, isCustom = true),
                ).toInt()
                listIds.forEachIndexed { index, listId ->
                    database.associationCollectionListDao().insertItem(
                        AssociatonCollectionListEntity(
                            id = 0,
                            advertisementSetCollectionId = collectionId,
                            advertisementSetListId = listId,
                            position = index,
                        ),
                    )
                }
                withContext(Dispatchers.Main) { onSaved() }
            }
        },
    )
}
