package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
@Dao
interface AdvertisementSetDao {
    @Query("SELECT * FROM advertisementsetentity")
    fun getAll(): List<AdvertisementSetEntity>

    @Query("SELECT * FROM advertisementsetentity WHERE id IN (:advertisementSetEntityIds)")
    fun loadAllByIds(advertisementSetEntityIds: IntArray): List<AdvertisementSetEntity>

    /*
    @Query("SELECT * FROM advertisementsetentity WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): User
*/
    @Insert
    fun insertAll(vararg advertisementSetEntity: AdvertisementSetEntity)

    @Delete
    fun delete(advertisementSetEntity: AdvertisementSetEntity)
}