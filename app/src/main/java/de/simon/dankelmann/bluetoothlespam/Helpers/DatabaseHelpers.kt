package de.simon.dankelmann.bluetoothlespam.Helpers

import android.os.ParcelUuid
import android.util.Log
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

        /**
         * Version of the built-in advertisement-set data. Bump whenever the built-in generators
         * gain new entries so existing installs re-sync them (see [syncBuiltInSets] /
         * [de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository.builtInSeedVersion]).
         */
        const val BUILT_IN_SEED_VERSION = 1

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
            var advertisementSets = mutableListOf<AdvertisementSet>()

            entities.forEach { entitiy ->
                var advertisementSet = getAdvertisementSetFromEntity(entitiy)
                advertisementSets.add(advertisementSet)
            }

            return advertisementSets.toList()
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

        /**
         * Additively tops up the built-in advertisement sets on app upgrade (called from
         * `BleSpamApplication` when [de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository.builtInSeedVersion]
         * is behind [BUILT_IN_SEED_VERSION]) -- [seedBuiltInListsAndCollections] only ever runs on a
         * brand-new database, so without this, model IDs added to the generators after a user's first
         * launch would never reach them.
         *
         * Idempotent: a set is matched by its advertised payload (type + service/manufacturer data,
         * NOT its title, so renaming a device does not re-add it), so only genuinely new payloads are
         * inserted, and each is appended to the existing built-in list for its type. Nothing is
         * deleted, so user-created collections -- which reference the same per-type lists -- are
         * untouched. Must run off the main thread, and never concurrently with [seedingThread]
         * (`BleSpamApplication` only calls it when the database already existed, i.e. not a fresh
         * install).
         */
        fun syncBuiltInSets() {
            val database = AppDatabase.getInstance()
            val context = AppContext.getContext()

            // Load payload rows once and index by advertiseDataId so signatures need no per-set query.
            val serviceDataByAdvertiseDataId =
                database.advertiseDataServiceDataDao().getAll().groupBy { it.advertiseDataId }
            val manufacturerDataByAdvertiseDataId =
                database.advertiseDataManufacturerSpecificDataDao().getAll().groupBy { it.advertiseDataId }

            fun payloadSignature(type: AdvertisementSetType, servicePart: String, manufacturerPart: String) =
                "${type.name}|svc[$servicePart]|mfg[$manufacturerPart]"

            fun signatureForEntity(entity: AdvertisementSetEntity): String {
                val svc = (serviceDataByAdvertiseDataId[entity.advertiseDataId] ?: emptyList())
                    .map { "${it.serviceUuid}=${it.serviceData ?: ""}" }.sorted().joinToString(",")
                val mfg = (manufacturerDataByAdvertiseDataId[entity.advertiseDataId] ?: emptyList())
                    .map { "${it.manufacturerId}=${it.manufacturerSpecificData}" }.sorted().joinToString(",")
                return payloadSignature(entity.type, svc, mfg)
            }

            fun signatureForSet(set: AdvertisementSet): String {
                val svc = set.advertiseData.services
                    .map { "${it.serviceUuid}=${it.serviceData?.toHexString() ?: ""}" }.sorted().joinToString(",")
                val mfg = set.advertiseData.manufacturerData
                    .map { "${it.manufacturerId}=${it.manufacturerSpecificData.toHexString()}" }.sorted().joinToString(",")
                return payloadSignature(set.type, svc, mfg)
            }

            // add() returns false when already present, so this also collapses duplicate keys within
            // a generator (matching mapOf's last-wins), exactly like the original seed.
            val knownSignatures = database.advertisementSetDao().getAll()
                .map { signatureForEntity(it) }.toHashSet()

            // Built-in per-type lists are titled "<Type> List" (see seedBuiltInListsAndCollections).
            val listIdByTitle = database.advertisementSetListDao().getAll().associate { it.title to it.id }
            val nextPositionByListId = mutableMapOf<Int, Int>()

            var added = 0
            // Batch every insert into one transaction (same reason as the initial seed): a top-up
            // of hundreds/thousands of sets must not be hundreds/thousands of separate commits.
            database.runInTransaction {
                AppDatabase.builtInAdvertisementSetGenerators.forEach { generator ->
                    generator.getAdvertisementSets(null).forEach { set ->
                        if (!knownSignatures.add(signatureForSet(set))) return@forEach

                        val listTitle = "${context.getString(set.type.stringResId())} List"
                        val listId = listIdByTitle[listTitle]
                        if (listId == null) {
                            // No built-in list for this type (e.g. a type added since the first seed).
                            // Skip rather than create an orphan set -- a new type is a code change that
                            // also updates seedBuiltInListsAndCollections.
                            Log.w(_logTag, "syncBuiltInSets: no built-in list '$listTitle'; skipping new ${set.type.name} set")
                            return@forEach
                        }

                        val position = nextPositionByListId.getOrPut(listId) {
                            database.associationListSetDao().findByListId(listId).size
                        }
                        val setId = saveAdvertisementSet(set)
                        database.associationListSetDao().insertItem(
                            AssociationListSetEntity(
                                id = 0,
                                advertisementSetId = setId,
                                advertisementSetListId = listId,
                                position = position,
                            ),
                        )
                        nextPositionByListId[listId] = position + 1
                        added++
                    }
                }
            }
            Log.d(_logTag, "syncBuiltInSets: added $added new built-in advertisement set(s)")
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