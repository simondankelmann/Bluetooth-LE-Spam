package de.simon.dankelmann.bluetoothlespam.Database

import androidx.room.RoomDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity

@androidx.room.Database(entities = [AdvertisementSetEntity::class], version = 1, exportSchema = false)
abstract class Database : RoomDatabase() {

}