package de.simon.dankelmann.bluetoothlespam.Helpers

import android.os.ParcelUuid
import androidx.sqlite.db.SupportSQLiteDatabase
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.builtInCollectionDefinitions
import de.simon.dankelmann.bluetoothlespam.Database.Dao.CollectionWithLists
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataServiceDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociationListSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociatonCollectionListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.PeriodicAdvertisingParametersEntity
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseData
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseSettings
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisingSetParameters
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import de.simon.dankelmann.bluetoothlespam.Models.PeriodicAdvertisingParameters
import de.simon.dankelmann.bluetoothlespam.Models.ServiceData
import java.util.UUID

class DatabaseHelpers {
    companion object{
        private const val _logTag = "DatabaseHelpers"

        fun saveAdvertisementSet(advertisementSet: AdvertisementSet,):Int{


            val database = AppDatabase.getInstance()

            var advertisementSetEntity:AdvertisementSetEntity = AdvertisementSetEntity(
                advertisementSet.id,
                advertisementSet.title,
                advertisementSet.target,
                advertisementSet.type,
                advertisementSet.duration,
                advertisementSet.maxExtendedAdvertisingEvents,
                advertisementSet.range,
                0 ,
                0 ,
                0,
                0,
                0,
                0
            )

            var advertiseSettingsEntity = AdvertiseSettingsEntity(
                advertisementSet.advertiseSettings.id,
                advertisementSet.advertiseSettings.advertiseMode,
                advertisementSet.advertiseSettings.txPowerLevel,
                advertisementSet.advertiseSettings.connectable,
                advertisementSet.advertiseSettings.timeout
            )

            var advertiseSettingsId = database.advertiseSettingsDao().insertItem(advertiseSettingsEntity).toInt()
            advertisementSetEntity.advertiseSettingsId = advertiseSettingsId

            var advertisingSetParametersEntity = AdvertisingSetParametersEntity(
                advertisementSet.advertisingSetParameters.id,
                advertisementSet.advertisingSetParameters.legacyMode,
                advertisementSet.advertisingSetParameters.interval,
                advertisementSet.advertisingSetParameters.txPowerLevel,
                advertisementSet.advertisingSetParameters.includeTxPowerLevel,
                advertisementSet.advertisingSetParameters.primaryPhy,
                advertisementSet.advertisingSetParameters.secondaryPhy,
                advertisementSet.advertisingSetParameters.scanable,
                advertisementSet.advertisingSetParameters.connectable,
                advertisementSet.advertisingSetParameters.anonymous)
            var advertisingSetParametersId = database.advertisingSetParametersDao().insertItem(advertisingSetParametersEntity).toInt()
            advertisementSetEntity.advertisingSetParametersId = advertisingSetParametersId

            var advertiseDataEntityId = saveAdvertiseData(advertisementSet.advertiseData, database)
            advertisementSetEntity.advertiseDataId = advertiseDataEntityId

            var scanResponseId = 0
            if(advertisementSet.scanResponse != null){
                scanResponseId = saveAdvertiseData(advertisementSet.scanResponse!!, database)
            }
            advertisementSetEntity.scanResponseId = scanResponseId

            var periodicAdvertiseDataId = 0
            if(advertisementSet.periodicAdvertiseData != null){
                periodicAdvertiseDataId = saveAdvertiseData(advertisementSet.periodicAdvertiseData!!, database)
            }
            advertisementSetEntity.periodicAdvertiseDataId = periodicAdvertiseDataId


            var periodicAdvertisingParametersEntity:PeriodicAdvertisingParametersEntity? = null
            if(advertisementSet.periodicAdvertisingParameters != null){
                periodicAdvertisingParametersEntity = PeriodicAdvertisingParametersEntity(
                    advertisementSet.periodicAdvertisingParameters!!.id,
                    advertisementSet.periodicAdvertisingParameters!!.includeTxPowerLevel,
                    advertisementSet.periodicAdvertisingParameters!!.interval
                )
                var periodicAdvertisingParametersId = database.periodicAdvertisingParametersDao().insertItem(periodicAdvertisingParametersEntity).toInt()
                advertisementSetEntity.periodicAdvertisingParametersId = periodicAdvertisingParametersId
            }

            var advertisementSetId = database.advertisementSetDao().insertItem(advertisementSetEntity).toInt()

            return advertisementSetId
        }
        fun saveAdvertiseData(advertiseData: AdvertiseData, database: AppDatabase): Int{
            var advertiseDataEntity = AdvertiseDataEntity(
                advertiseData.id,
                advertiseData.includeDeviceName,
                advertiseData.includeTxPower
            )

            // SAVE
            var advertiseDataId = database.advertiseDataDao().insertItem(advertiseDataEntity).toInt()

            // SAVE SERVICE DATA
            advertiseData.services.forEach {service ->
                var advertiseDataServiceDataEntity = AdvertiseDataServiceDataEntity(
                    service.id,
                    advertiseDataId,
                    UUID.fromString(service.serviceUuid.toString()),
                    service.serviceData?.toHexString()
                )

                var serviceDataId = database.advertiseDataServiceDataDao().insertItem(advertiseDataServiceDataEntity)
            }

            // SAVE MANUFACTURER DATA
            advertiseData.manufacturerData.forEach { manufacturerSpecificData ->
                var manufacturerSpecificDataEntity = AdvertiseDataManufacturerSpecificDataEntity(
                    manufacturerSpecificData.id,
                    advertiseDataId,
                    manufacturerSpecificData.manufacturerId,
                    manufacturerSpecificData.manufacturerSpecificData.toHexString()
                )

                var manufacturerDataId = database.advertiseDataManufacturerSpecificDataDao().insertItem(manufacturerSpecificDataEntity)
            }

            // RETURN ID
            return advertiseDataId
        }

        /**
         * Renames a Swift Pair entry (Allow Custom Swift Pair Names setting) — persists the new
         * title and rebuilds the manufacturer-specific-data bytes with the same Swift Pair
         * framing ([SwiftPairAdvertisementSetGenerator.PREPENDED_BYTES]) so the new name is what
         * actually gets advertised, then mutates [advertisementSet] in place so an
         * already-loaded/queued set picks up the change immediately. Must be called off the main
         * thread (Room has no allowMainThreadQueries()).
         */
        fun updateSwiftPairDeviceName(advertisementSet: AdvertisementSet, newName: String) {
            val database = AppDatabase.getInstance()
            database.advertisementSetDao().updateTitle(advertisementSet.id, newName)

            val manufacturerSpecificData = advertisementSet.advertiseData.manufacturerData.firstOrNull() ?: return
            val newBytes = SwiftPairAdvertisementSetGenerator.PREPENDED_BYTES.plus(newName.toByteArray())
            database.advertiseDataManufacturerSpecificDataDao()
                .updateManufacturerSpecificData(manufacturerSpecificData.id, newBytes.toHexString())

            advertisementSet.title = newName
            manufacturerSpecificData.manufacturerSpecificData = newBytes
        }

        fun getAdvertisementSetFromEntity(advertisementSetEntity: AdvertisementSetEntity):AdvertisementSet{
            var advertisementSet = AdvertisementSet()

            // Data
            advertisementSet.id = advertisementSetEntity.id
            advertisementSet.title = advertisementSetEntity.title
            advertisementSet.target = advertisementSetEntity.target
            advertisementSet.type = advertisementSetEntity.type
            advertisementSet.duration = advertisementSetEntity.duration
            advertisementSet.maxExtendedAdvertisingEvents = advertisementSetEntity.maxExtendedAdvertisingEvents
            advertisementSet.range = advertisementSetEntity.range

            var database = AppDatabase.getInstance()

            // Advertise Settings
            database.advertiseSettingsDao().findById(advertisementSetEntity.advertiseSettingsId).let { entity ->
                advertisementSet.advertiseSettings = AdvertiseSettings().apply {
                    id = advertisementSetEntity.id
                    advertiseMode = entity.advertiseMode
                    txPowerLevel = entity.txPowerLevel
                    connectable = entity.connectable
                    timeout = entity.timeout
                }
            }

            // AdvertisingSetParameters
            database.advertisingSetParametersDao().findById(advertisementSetEntity.advertisingSetParametersId).let { entity ->
                advertisementSet.advertisingSetParameters = AdvertisingSetParameters().apply {
                    id = entity.id
                    legacyMode = entity.legacyMode
                    interval = entity.interval
                    txPowerLevel = entity.txPowerLevel
                    includeTxPowerLevel = entity.includeTxPowerLevel
                    primaryPhy = entity.primaryPhy
                    secondaryPhy = entity.secondaryPhy
                    scanable = entity.scanable
                    connectable = entity.connectable
                    anonymous = entity.anonymous
                }
            }

            advertisementSetEntity.advertiseDataId.let { id ->
                database.advertiseDataDao().findById(id)?.let { entity ->
                    advertisementSet.advertiseData = getAdvertiseDataFromEntity(entity, database)
                }
            }

            advertisementSetEntity.scanResponseId?.let { id ->
                database.advertiseDataDao().findById(id)?.let { entity ->
                    advertisementSet.scanResponse = getAdvertiseDataFromEntity(entity, database)
                }
            }

            advertisementSetEntity.periodicAdvertiseDataId?.let { id ->
                database.advertiseDataDao().findById(id)?.let { entity ->
                    advertisementSet.periodicAdvertiseData = getAdvertiseDataFromEntity(entity, database)
                }
            }

            advertisementSetEntity.periodicAdvertisingParametersId?.let { _ ->
                database.advertisingSetParametersDao().findById(advertisementSetEntity.advertisingSetParametersId).let { entity ->
                    advertisementSet.periodicAdvertisingParameters = PeriodicAdvertisingParameters().apply {
                        id = entity.id
                        includeTxPowerLevel = entity.includeTxPowerLevel
                        interval = entity.interval
                    }
                }
            }

            return advertisementSet
        }

        fun getAdvertiseDataFromEntity(advertiseDataEntity: AdvertiseDataEntity, database: AppDatabase):AdvertiseData{
            var advertiseData = AdvertiseData()

            advertiseData.id = advertiseDataEntity.id
            advertiseData.includeDeviceName = advertiseDataEntity.includeDeviceName
            advertiseData.includeTxPower = advertiseDataEntity.includeTxPower

            var manufacturerSpecificDataEntities = database.advertiseDataManufacturerSpecificDataDao().findByAdvertiseDataId(advertiseDataEntity.id)
            manufacturerSpecificDataEntities.forEach{ manufacturerSpecificDataEntity ->
                var manufacturerSpecificData = ManufacturerSpecificData()
                manufacturerSpecificData.id = manufacturerSpecificDataEntity.id
                manufacturerSpecificData.manufacturerId = manufacturerSpecificDataEntity.manufacturerId
                manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(manufacturerSpecificDataEntity.manufacturerSpecificData)

                advertiseData.manufacturerData.add(manufacturerSpecificData)
            }

            var advertiseDataServiceDataEntities = database.advertiseDataServiceDataDao().findByAdvertiseDataId(advertiseDataEntity.id)
            advertiseDataServiceDataEntities.forEach{ advertiseDataServiceDataEntity ->
                var serviceData = ServiceData()

                serviceData.id = advertiseDataServiceDataEntity.id
                serviceData.serviceUuid = ParcelUuid.fromString(advertiseDataServiceDataEntity.serviceUuid.toString())
                if(advertiseDataServiceDataEntity.serviceData != null){
                    serviceData.serviceData = StringHelpers.decodeHex(advertiseDataServiceDataEntity.serviceData!!)
                }

                advertiseData.services.add(serviceData)
            }

            return advertiseData
        }

        fun getAllAdvertisementSetsForTarget(advertisementTarget: AdvertisementTarget):List<AdvertisementSet>{
            var database = AppDatabase.getInstance()
            var advertisementSetEntities = database.advertisementSetDao().findByTarget(advertisementTarget)
            return getAdvertisementSetListFromEntities(advertisementSetEntities)
        }

        fun getAllAdvertisementSetsForType(advertisementSetType: AdvertisementSetType):List<AdvertisementSet>{
            var database = AppDatabase.getInstance()
            var advertisementSetEntities = database.advertisementSetDao().findByType(advertisementSetType)
            return getAdvertisementSetListFromEntities(advertisementSetEntities)
        }

        fun getAdvertisementSetListFromEntities(entities: List<AdvertisementSetEntity>):List<AdvertisementSet>{
            if (entities.isEmpty()) return emptyList()
            // Batched assembly: the old per-entity path issued ~5 queries per set
            // (~5000 for a full Fast Pair collection). Same mapping, same quirks
            // (see notes inside), but a constant handful of IN queries.
            return try {
                assembleAdvertisementSetsBatched(entities)
            } catch (e: Exception) {
                // Fallback to the old row-by-row path so a partial batch failure
                // can never load less than before; per-row skips as in getAllAdvertisementSets.
                val out = mutableListOf<AdvertisementSet>()
                entities.forEach { entity ->
                    try {
                        out.add(getAdvertisementSetFromEntity(entity))
                    } catch (ignored: Exception) {
                    }
                }
                out.toList()
            }
        }

        private fun assembleAdvertisementSetsBatched(entities: List<AdvertisementSetEntity>): List<AdvertisementSet> {
            val database = AppDatabase.getInstance()

            val settingsById = database.advertiseSettingsDao()
                .loadAllByIds(entities.map { it.advertiseSettingsId }.distinct().toIntArray())
                .associateBy { it.id }
            val paramsById = database.advertisingSetParametersDao()
                .loadAllByIds(entities.map { it.advertisingSetParametersId }.distinct().toIntArray())
                .associateBy { it.id }

            val dataIds = entities.flatMap {
                listOfNotNull(
                    it.advertiseDataId.takeIf { id -> id > 0 },
                    it.scanResponseId?.takeIf { id -> id > 0 },
                    it.periodicAdvertiseDataId?.takeIf { id -> id > 0 },
                )
            }.distinct().toIntArray()
            val dataById = if (dataIds.isEmpty()) emptyMap() else database.advertiseDataDao()
                .loadAllByIds(dataIds).associateBy { it.id }
            val mfrByDataId = if (dataIds.isEmpty()) emptyMap() else database
                .advertiseDataManufacturerSpecificDataDao().findByAdvertiseDataIds(dataIds)
                .groupBy { it.advertiseDataId }
            val svcByDataId = if (dataIds.isEmpty()) emptyMap() else database
                .advertiseDataServiceDataDao().findByAdvertiseDataIds(dataIds)
                .groupBy { it.advertiseDataId }

            fun buildAdvertiseData(dataId: Int?): AdvertiseData? {
                if (dataId == null || dataId <= 0) return null
                val entity = dataById[dataId] ?: return null
                val out = AdvertiseData()
                out.id = entity.id
                out.includeDeviceName = entity.includeDeviceName
                out.includeTxPower = entity.includeTxPower
                mfrByDataId[entity.id]?.forEach { mfr ->
                    val item = ManufacturerSpecificData()
                    item.id = mfr.id
                    item.manufacturerId = mfr.manufacturerId
                    item.manufacturerSpecificData = StringHelpers.decodeHex(mfr.manufacturerSpecificData)
                    out.manufacturerData.add(item)
                }
                svcByDataId[entity.id]?.forEach { svc ->
                    val item = ServiceData()
                    item.id = svc.id
                    item.serviceUuid = ParcelUuid.fromString(svc.serviceUuid.toString())
                    if (svc.serviceData != null) {
                        item.serviceData = StringHelpers.decodeHex(svc.serviceData!!)
                    }
                    out.services.add(item)
                }
                return out
            }

            val out = mutableListOf<AdvertisementSet>()
            entities.forEach { entity ->
                try {
                    val set = AdvertisementSet()
                    set.id = entity.id
                    set.title = entity.title
                    set.target = entity.target
                    set.type = entity.type
                    set.duration = entity.duration
                    set.maxExtendedAdvertisingEvents = entity.maxExtendedAdvertisingEvents
                    set.range = entity.range

                    val settings = settingsById[entity.advertiseSettingsId]
                        ?: throw IllegalStateException("Missing AdvertiseSettings ${entity.advertiseSettingsId}")
                    set.advertiseSettings = AdvertiseSettings().apply {
                        // Quirk preserved from getAdvertisementSetFromEntity: model id mirrors
                        // the set row id, not the settings row id.
                        id = entity.id
                        advertiseMode = settings.advertiseMode
                        txPowerLevel = settings.txPowerLevel
                        connectable = settings.connectable
                        timeout = settings.timeout
                    }

                    val params = paramsById[entity.advertisingSetParametersId]
                        ?: throw IllegalStateException("Missing AdvertisingSetParameters ${entity.advertisingSetParametersId}")
                    set.advertisingSetParameters = AdvertisingSetParameters().apply {
                        id = params.id
                        legacyMode = params.legacyMode
                        interval = params.interval
                        txPowerLevel = params.txPowerLevel
                        includeTxPowerLevel = params.includeTxPowerLevel
                        primaryPhy = params.primaryPhy
                        secondaryPhy = params.secondaryPhy
                        scanable = params.scanable
                        connectable = params.connectable
                        anonymous = params.anonymous
                    }

                    buildAdvertiseData(entity.advertiseDataId)?.let { set.advertiseData = it }
                    buildAdvertiseData(entity.scanResponseId)?.let { set.scanResponse = it }
                    buildAdvertiseData(entity.periodicAdvertiseDataId)?.let { set.periodicAdvertiseData = it }

                    // Quirk preserved: the old path (probably by copy-paste) builds the
                    // periodic *parameters* from the REGULAR parameters row, not the periodic
                    // table. Replicate exactly so periodic-capable sets behave as before.
                    if (entity.periodicAdvertisingParametersId != null) {
                        set.periodicAdvertisingParameters = PeriodicAdvertisingParameters().apply {
                            id = params.id
                            includeTxPowerLevel = params.includeTxPowerLevel
                            interval = params.interval
                        }
                    }

                    out.add(set)
                } catch (ignored: Exception) {
                    // Skip single unreadable rows, like getAllAdvertisementSets does.
                }
            }
            return out.toList()
        }

        /**
         * Activates the dormant AdvertisementSetList/AdvertisementSetCollection schema (plan §8):
         * one real [AdvertisementSetListEntity] per [AdvertisementSetType] (mirroring the 14
         * seeding generators), joined to the sets already saved for that type, then the 6
         * built-in collections ([builtInCollectionDefinitions]) joined to their member lists.
         * Called once from fresh-install seeding and once from `Migration_2_3` for existing
         * installs upgrading — both cases run against a DB that already has its
         * [AdvertisementSetEntity] rows saved.
         */
        fun seedBuiltInListsAndCollections() {
            val database = AppDatabase.getInstance()
            val context = AppContext.getContext()
            val typeToListId = mutableMapOf<AdvertisementSetType, Int>()

            AdvertisementSetType.entries
                .filter { it != AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED }
                .forEach { type ->
                    val setsForType = database.advertisementSetDao().findByType(type)
                    if (setsForType.isNotEmpty()) {
                        val listId = database.advertisementSetListDao().insertItem(
                            AdvertisementSetListEntity(id = 0, title = "${context.getString(type.stringResId())} List"),
                        ).toInt()
                        typeToListId[type] = listId

                        val associations = setsForType.mapIndexed { index, setEntity ->
                            AssociationListSetEntity(
                                id = 0,
                                advertisementSetId = setEntity.id,
                                advertisementSetListId = listId,
                                position = index,
                            )
                        }
                        database.associationListSetDao().insertAll(*associations.toTypedArray())
                    }
                }

            builtInCollectionDefinitions.forEach { definition ->
                val collectionId = database.advertisementSetCollectionDao().insertItem(
                    AdvertisementSetCollectionEntity(id = 0, title = definition.title, isCustom = false),
                ).toInt()

                val associations = definition.types.mapIndexedNotNull { index, type ->
                    val listId = typeToListId[type] ?: return@mapIndexedNotNull null
                    AssociatonCollectionListEntity(
                        id = 0,
                        advertisementSetCollectionId = collectionId,
                        advertisementSetListId = listId,
                        position = index,
                    )
                }
                if (associations.isNotEmpty()) {
                    database.associationCollectionListDao().insertAll(*associations.toTypedArray())
                }
            }
        }

        fun getAllAdvertisementSetsForList(listId: Int): List<AdvertisementSet> {
            val database = AppDatabase.getInstance()
            // findByListId is already ORDER BY position; loadAllByIds's WHERE IN doesn't
            // preserve that order, so re-sort the batched result to match it.
            val orderedSetIds = database.associationListSetDao().findByListId(listId).map { it.advertisementSetId }
            val entitiesById = database.advertisementSetDao().loadAllByIds(orderedSetIds.toIntArray()).associateBy { it.id }
            val setEntities = orderedSetIds.mapNotNull { entitiesById[it] }
            return getAdvertisementSetListFromEntities(setEntities)
        }

        fun getAdvertisementSetById(id: Int): AdvertisementSet? {
            return try {
                val database = AppDatabase.getInstance()
                val entity = database.advertisementSetDao().findByIdOrNull(id) ?: return null
                getAdvertisementSetFromEntity(entity)
            } catch (e: Exception) {
                null
            }
        }

        fun getAllAdvertisementSets(): List<AdvertisementSet> {
            // Batched path already skips single unreadable rows internally.
            return getAdvertisementSetListFromEntities(AppDatabase.getInstance().advertisementSetDao().getAll())
        }

        /**
         * Persists title + advertiseData content (flags, manufacturer entries, service entries)
         * of an already stored AdvertisementSet, matched by row ids carried by the model.
         */
        fun updateAdvertisementSetContent(advertisementSet: AdvertisementSet) {
            val database = AppDatabase.getInstance()

            database.advertisementSetDao().updateTitle(advertisementSet.id, advertisementSet.title)

            val advertiseData = advertisementSet.advertiseData
            database.advertiseDataDao().updateFlags(
                advertiseData.id,
                advertiseData.includeDeviceName,
                advertiseData.includeTxPower
            )
            advertiseData.manufacturerData.forEach { manufacturerSpecificData ->
                database.advertiseDataManufacturerSpecificDataDao().updateEntry(
                    manufacturerSpecificData.id,
                    manufacturerSpecificData.manufacturerId,
                    manufacturerSpecificData.manufacturerSpecificData.toHexString()
                )
            }
            advertiseData.services.forEach { serviceData ->
                val uuid = serviceData.serviceUuid
                if (uuid != null) {
                    database.advertiseDataServiceDataDao().updateEntry(
                        serviceData.id,
                        UUID.fromString(uuid.toString()),
                        serviceData.serviceData?.toHexString()
                    )
                }
            }
        }

        /**
         * Physically removes an AdvertisementSet and all its related rows.
         * Used for custom (non-default) sets on "reset to defaults".
         * Also cleans list associations so no orphan rows remain.
         */
        fun deleteAdvertisementSetTree(advertisementSetId: Int) {
            val database = AppDatabase.getInstance()
            val entity = try {
                database.advertisementSetDao().findByIdOrNull(advertisementSetId)
            } catch (e: Exception) {
                null
            } ?: return

            try {
                database.associationListSetDao().deleteBySetId(advertisementSetId)
            } catch (e: Exception) {
                // Older DBs without the query or missing rows: ignore
            }

            fun deleteAdvertiseDataTree(advertiseDataId: Int?) {
                if (advertiseDataId == null || advertiseDataId <= 0) return
                database.advertiseDataServiceDataDao().deleteByAdvertiseDataId(advertiseDataId)
                database.advertiseDataManufacturerSpecificDataDao().deleteByAdvertiseDataId(advertiseDataId)
                database.advertiseDataDao().deleteById(advertiseDataId)
            }

            deleteAdvertiseDataTree(entity.advertiseDataId)
            deleteAdvertiseDataTree(entity.scanResponseId)
            deleteAdvertiseDataTree(entity.periodicAdvertiseDataId)

            if (entity.advertiseSettingsId > 0) {
                database.advertiseSettingsDao().deleteById(entity.advertiseSettingsId)
            }
            if (entity.advertisingSetParametersId > 0) {
                database.advertisingSetParametersDao().deleteById(entity.advertisingSetParametersId)
            }
            if ((entity.periodicAdvertisingParametersId ?: 0) > 0) {
                database.periodicAdvertisingParametersDao().deleteById(entity.periodicAdvertisingParametersId!!)
            }

            database.advertisementSetDao().deleteById(entity.id)
        }

        /**
         * Saves a set and ensures it is linked to its type's built-in list
         * (used by syncMissingDefaults so new Model IDs actually show up in the
         * Compose UI, which loads via lists, not via raw entities).
         * @return new row id, or 0 on failure
         */
        fun saveAdvertisementSetAndAssociate(advertisementSet: AdvertisementSet): Int {
            val newId = try {
                saveAdvertisementSet(advertisementSet)
            } catch (e: Exception) {
                return 0
            }
            if (newId <= 0) return 0
            try {
                val database = AppDatabase.getInstance()
                val context = AppContext.getContext()
                val expectedTitle = try {
                    "${context.getString(advertisementSet.type.stringResId())} List"
                } catch (e: Exception) {
                    null
                }
                var listId: Int? = null
                if (expectedTitle != null) {
                    listId = database.advertisementSetListDao().getAll()
                        .firstOrNull { it.title == expectedTitle }?.id
                }
                if (listId == null) {
                    // Fallback: pick the first list whose sets share the same type
                    val allLists = database.advertisementSetListDao().getAll()
                    for (list in allLists) {
                        val firstSetId = database.associationListSetDao().findByListId(list.id).firstOrNull()?.advertisementSetId
                        if (firstSetId != null) {
                            val firstEntity = try {
                                database.advertisementSetDao().findByIdOrNull(firstSetId)
                            } catch (e: Exception) { null }
                            if (firstEntity != null && firstEntity.type == advertisementSet.type) {
                                listId = list.id
                                break
                            }
                        }
                    }
                }
                if (listId == null) {
                    // No list for this type yet: create one
                    listId = database.advertisementSetListDao().insertItem(
                        AdvertisementSetListEntity(id = 0, title = expectedTitle ?: "${advertisementSet.type.name} List")
                    ).toInt()
                }
                val maxPos = try {
                    database.associationListSetDao().getMaxPositionForList(listId!!) ?: -1
                } catch (e: Exception) { -1 }
                database.associationListSetDao().insertItem(
                    AssociationListSetEntity(
                        id = 0,
                        advertisementSetId = newId,
                        advertisementSetListId = listId!!,
                        position = maxPos + 1
                    )
                )
            } catch (e: Exception) {
                // Association failure must not fail the sync; set exists, UI reload will pick it up via type query
            }
            return newId
        }

        /**
         * DB row -> domain model, for relaunching a Quick Start item or a Device Selector group
         * pick (plan §8). Title/list-titles only, no per-set queries -- callers navigate on this
         * immediately, then load each list's actual sets on a background Thread so the control
         * GUI shows up without waiting on a potentially-many-lists DB fetch.
         */
        fun buildAdvertisementSetCollectionSkeletonFromEntity(collectionWithLists: CollectionWithLists): AdvertisementSetCollection {
            val collection = AdvertisementSetCollection()
            collection.title = collectionWithLists.collection.title
            collectionWithLists.lists.forEach { listEntity ->
                val list = AdvertisementSetList()
                list.title = listEntity.title
                collection.advertisementSetLists.add(list)
            }
            return collection
        }
    }
}