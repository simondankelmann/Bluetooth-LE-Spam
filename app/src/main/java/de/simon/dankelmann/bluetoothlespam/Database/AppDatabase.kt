package de.simon.dankelmann.bluetoothlespam.Database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityIos17CrashAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewAirtagPopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNotYourDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.EasySetupBudsAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.EasySetupWatchAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairDevicesAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairDebugAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairNonProductionAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairPhoneSetupAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespousePlayAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespouseStopAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseDataDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseDataManufacturerSpecificDataDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseDataServiceDataDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertiseSettingsDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisementSetCollectionDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisementSetDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisementSetListDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AdvertisingSetParametersDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AssociationCollectionListDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.AssociationListSetDao
import de.simon.dankelmann.bluetoothlespam.Database.Dao.PeriodicAdvertisingParametersDao
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataServiceDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociatonCollectionListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociationListSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.PeriodicAdvertisingParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Migrations.Migration_1_2
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers

@androidx.room.Database(
    entities = [AdvertiseDataEntity::class,
                AdvertiseDataManufacturerSpecificDataEntity::class,
                AdvertiseDataServiceDataEntity::class,
                AdvertisementSetCollectionEntity::class,
                AdvertisementSetEntity::class,
                AdvertisementSetListEntity::class,
                AdvertiseSettingsEntity::class,
                AdvertisingSetParametersEntity::class,
                AssociatonCollectionListEntity::class,
                AssociationListSetEntity::class,
                PeriodicAdvertisingParametersEntity::class],
    version = 2,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    var isSeeding = false
    abstract fun advertiseDataDao(): AdvertiseDataDao
    abstract fun advertiseDataManufacturerSpecificDataDao(): AdvertiseDataManufacturerSpecificDataDao
    abstract fun advertiseDataServiceDataDao(): AdvertiseDataServiceDataDao

    abstract fun advertisementSetCollectionDao(): AdvertisementSetCollectionDao

    abstract fun advertisementSetDao(): AdvertisementSetDao

    abstract fun advertisementSetListDao(): AdvertisementSetListDao

    abstract fun advertiseSettingsDao(): AdvertiseSettingsDao

    abstract fun advertisingSetParametersDao(): AdvertisingSetParametersDao

    abstract fun associationCollectionListDao(): AssociationCollectionListDao

    abstract fun associationListSetDao(): AssociationListSetDao

    abstract fun periodicAdvertisingParametersDao(): PeriodicAdvertisingParametersDao


    companion object {
        private const val _logTag = "AppDatabase"
        private var INSTANCE: AppDatabase? = null

        fun getInstance(): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(AppContext.getContext()).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "BluetoothLeSpamDatabase.db")
                .addCallback(seedDatabaseCallback(context))
                .addMigrations(Migration_1_2)
                //.fallbackToDestructiveMigration()
                .build()

        private fun seedDatabaseCallback(context: Context): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Thread {
                        synchronized(this) {
                            seedingThread.run()
                        }
                    }.start()
                }
            }
        }

        val seedingThread = Runnable {
            Log.d(_logTag, "Starting Database Seeding")
            getInstance().isSeeding = true

            val advertisementSetGenerators = listOf(
                FastPairDevicesAdvertisementSetGenerator(),
                FastPairPhoneSetupAdvertisementSetGenerator(),
                FastPairNonProductionAdvertisementSetGenerator(),
                FastPairDebugAdvertisementSetGenerator(),

                //ContinuityDevicePopUpAdvertisementSetGenerator(),
                ContinuityNotYourDevicePopUpAdvertisementSetGenerator(),
                ContinuityNewDevicePopUpAdvertisementSetGenerator(),
                ContinuityNewAirtagPopUpAdvertisementSetGenerator(),
                ContinuityActionModalAdvertisementSetGenerator(),
                ContinuityIos17CrashAdvertisementSetGenerator(),

                SwiftPairAdvertisementSetGenerator(),

                EasySetupWatchAdvertisementSetGenerator(),
                EasySetupBudsAdvertisementSetGenerator(),

                LovespousePlayAdvertisementSetGenerator(),
                LovespouseStopAdvertisementSetGenerator()
            )

            advertisementSetGenerators.forEach{ generator ->
                val advertisementSets = generator.getAdvertisementSets(null)
                advertisementSets.forEach{ advertisementSet ->
                    DatabaseHelpers.saveAdvertisementSet(advertisementSet)
                }
            }

            getInstance().isSeeding = false
            Log.d(_logTag, "Database Seeding finished")
        }
    }
}