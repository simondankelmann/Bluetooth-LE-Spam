package de.simon.dankelmann.bluetoothlespam.Helpers

import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataServiceDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.PeriodicAdvertisingParametersEntity
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseData
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import java.util.UUID

class DatabaseHelpers {
    companion object{
        private const val _logTag = "DatabaseHelpers"

        fun saveAdvertisementSet(advertisementSet: AdvertisementSet):Int{
            var database = AppDatabase.getInstance()

            var advertisementSetEntity:AdvertisementSetEntity = AdvertisementSetEntity(
                advertisementSet.id,
                advertisementSet.title,
                advertisementSet.target,
                advertisementSet.type,
                advertisementSet.duration,
                advertisementSet.maxExtendedAdvertisingEvents,
                0 ,
                0 ,
                0 ,
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
            }

            // RETURN ID
            return advertiseDataId
        }
    }
}