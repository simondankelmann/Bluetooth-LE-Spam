package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.content.SharedPreferences
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityIos17CrashAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewAirtagPopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNotYourDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.EasySetupBudsAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.EasySetupWatchAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairDebugAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairDevicesAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairNonProductionAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.FastPairPhoneSetupAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.IAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespousePlayAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespouseStopAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tracks user customizations of advertisement sets (devices) without changing
 * the database schema:
 *
 * - Deleted sets are only flagged in SharedPreferences (rows stay in the DB),
 *   so they can be restored at any time.
 * - Modified sets keep a JSON backup of their original state (title + payload),
 *   so they can be restored individually.
 * - "Reset to defaults" re-applies backups, removes custom (non-default) rows
 *   and re-inserts missing defaults.
 *
 * Sets are identified by their stable database row id.
 */
object DeviceCustomizationHelper {

    private const val PREFS_NAME = "device_customizations"
    private const val KEY_DELETED_IDS = "deleted_ids"
    private const val BACKUP_PREFIX = "backup_"

    data class ResetResult(val restored: Int, val removedCustoms: Int, val readded: Int)

    private fun prefs(): SharedPreferences {
        return AppContext.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ------------------------------------------------------------------
    // Default sets (single source of truth, also used for DB seeding)
    // ------------------------------------------------------------------

    fun defaultGenerators(): List<IAdvertisementSetGenerator> {
        return listOf(
            FastPairDevicesAdvertisementSetGenerator(),
            FastPairPhoneSetupAdvertisementSetGenerator(),
            FastPairNonProductionAdvertisementSetGenerator(),
            FastPairDebugAdvertisementSetGenerator(),

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
    }

    private var _defaultKeysCache: Set<String>? = null
    private var _defaultSetsCache: Map<String, AdvertisementSet>? = null

    @Synchronized
    fun defaultSetsByKey(): Map<String, AdvertisementSet> {
        _defaultSetsCache?.let { return it }
        val map = LinkedHashMap<String, AdvertisementSet>()
        defaultGenerators().forEach { generator ->
            generator.getAdvertisementSets(null).forEach { set ->
                map.putIfAbsent(identityKeyOf(set), set)
            }
        }
        _defaultSetsCache = map
        _defaultKeysCache = map.keys
        return map
    }

    fun defaultKeys(): Set<String> {
        _defaultKeysCache?.let { return it }
        return defaultSetsByKey().keys
    }

    /**
     * Identity of a set based on its content (not its row id), used to decide
     * whether a DB row is a default set or a user-added custom set.
     */
    fun identityKeyOf(set: AdvertisementSet): String {
        val mfr = set.advertiseData.manufacturerData.joinToString(";") {
            "${it.manufacturerId}:${it.manufacturerSpecificData.toHexString()}"
        }
        val srv = set.advertiseData.services.joinToString(";") {
            "${it.serviceUuid}:${it.serviceData?.toHexString()}"
        }
        return "${set.type.name}|${set.title}|$mfr|$srv"
    }

    fun isDefaultSet(set: AdvertisementSet): Boolean {
        return defaultKeys().contains(identityKeyOf(set))
    }

    // ------------------------------------------------------------------
    // Deleted sets (soft delete by row id)
    // ------------------------------------------------------------------

    fun isDeleted(id: Int): Boolean {
        return getDeletedIds().contains(id)
    }

    fun setDeleted(id: Int, deleted: Boolean) {
        val ids = getDeletedIds().toMutableSet()
        if (deleted) ids.add(id) else ids.remove(id)
        prefs().edit().putStringSet(KEY_DELETED_IDS, ids.map { it.toString() }.toSet()).apply()
    }

    fun getDeletedIds(): Set<Int> {
        return prefs().getStringSet(KEY_DELETED_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun filterDeleted(sets: List<AdvertisementSet>): List<AdvertisementSet> {
        val deleted = getDeletedIds()
        if (deleted.isEmpty()) return sets
        return sets.filter { !deleted.contains(it.id) }
    }

    // ------------------------------------------------------------------
    // Modified sets (original-state backups)
    // ------------------------------------------------------------------

    private fun backupKey(id: Int) = "$BACKUP_PREFIX$id"

    fun isModified(id: Int): Boolean {
        return prefs().contains(backupKey(id))
    }

    fun getModifiedIds(): Set<Int> {
        return prefs().all.keys
            .filter { it.startsWith(BACKUP_PREFIX) }
            .mapNotNull { it.removePrefix(BACKUP_PREFIX).toIntOrNull() }
            .toSet()
    }

    fun getBackup(id: Int): JSONObject? {
        val raw = prefs().getString(backupKey(id), null) ?: return null
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun getBackupTitle(id: Int): String? {
        return try {
            getBackup(id)?.optString("title", null)?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    fun saveBackupIfAbsent(set: AdvertisementSet) {
        if (isModified(set.id)) return
        prefs().edit().putString(backupKey(set.id), backupToJson(set).toString()).apply()
    }

    fun clearBackup(id: Int) {
        prefs().edit().remove(backupKey(id)).apply()
    }

    private fun backupToJson(set: AdvertisementSet): JSONObject {
        val json = JSONObject()
        json.put("title", set.title)
        json.put("includeDeviceName", set.advertiseData.includeDeviceName)
        json.put("includeTxPower", set.advertiseData.includeTxPower)

        val mfr = JSONArray()
        set.advertiseData.manufacturerData.forEach {
            val entry = JSONObject()
            entry.put("rowId", it.id)
            entry.put("mid", it.manufacturerId)
            entry.put("hex", it.manufacturerSpecificData.toHexString())
            mfr.put(entry)
        }
        json.put("mfr", mfr)

        val srv = JSONArray()
        set.advertiseData.services.forEach {
            val entry = JSONObject()
            entry.put("rowId", it.id)
            entry.put("uuid", it.serviceUuid?.toString() ?: "")
            if (it.serviceData != null) entry.put("hex", it.serviceData!!.toHexString())
            else entry.put("hex", JSONObject.NULL)
            srv.put(entry)
        }
        json.put("srv", srv)
        return json
    }

    fun matchesBackup(set: AdvertisementSet): Boolean {
        val backup = getBackup(set.id) ?: return false
        return try {
            if (backup.optString("title", null) != set.title) return false
            if (backup.optBoolean("includeDeviceName") != set.advertiseData.includeDeviceName) return false
            if (backup.optBoolean("includeTxPower") != set.advertiseData.includeTxPower) return false

            val mfrBackup = backup.optJSONArray("mfr") ?: JSONArray()
            if (mfrBackup.length() != set.advertiseData.manufacturerData.size) return false
            for (i in 0 until mfrBackup.length()) {
                val entry = mfrBackup.getJSONObject(i)
                val current = set.advertiseData.manufacturerData[i]
                if (entry.optInt("mid") != current.manufacturerId) return false
                if (entry.optString("hex") != current.manufacturerSpecificData.toHexString()) return false
            }

            val srvBackup = backup.optJSONArray("srv") ?: JSONArray()
            if (srvBackup.length() != set.advertiseData.services.size) return false
            for (i in 0 until srvBackup.length()) {
                val entry = srvBackup.getJSONObject(i)
                val current = set.advertiseData.services[i]
                if ((entry.optString("uuid", "")) != (current.serviceUuid?.toString() ?: "")) return false
                val backupHex = if (entry.isNull("hex")) null else entry.optString("hex")
                if (backupHex != current.serviceData?.toHexString()) return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restores the backed-up original state into the DB row.
     * @return true if a row was restored, false if the row no longer exists
     * (backup is pruned in that case).
     */
    fun applyBackup(id: Int): Boolean {
        val backup = getBackup(id) ?: return false
        val current = try {
            DatabaseHelpers.getAdvertisementSetById(id)
        } catch (e: Exception) {
            null
        }
        if (current == null) {
            clearBackup(id)
            setDeleted(id, false)
            return false
        }
        try {
            current.title = backup.optString("title", current.title)
            current.advertiseData.includeDeviceName = backup.optBoolean("includeDeviceName", current.advertiseData.includeDeviceName)
            current.advertiseData.includeTxPower = backup.optBoolean("includeTxPower", current.advertiseData.includeTxPower)

            val mfrBackup = backup.optJSONArray("mfr") ?: JSONArray()
            val mfrById = current.advertiseData.manufacturerData.associateBy { it.id }
            for (i in 0 until mfrBackup.length()) {
                val entry = mfrBackup.getJSONObject(i)
                val target = mfrById[entry.optInt("rowId", -1)]
                    ?: current.advertiseData.manufacturerData.getOrNull(i)
                    ?: continue
                target.manufacturerId = entry.optInt("mid", target.manufacturerId)
                target.manufacturerSpecificData = StringHelpers.decodeHex(entry.optString("hex", ""))
            }

            val srvBackup = backup.optJSONArray("srv") ?: JSONArray()
            val srvById = current.advertiseData.services.associateBy { it.id }
            for (i in 0 until srvBackup.length()) {
                val entry = srvBackup.getJSONObject(i)
                val target = srvById[entry.optInt("rowId", -1)]
                    ?: current.advertiseData.services.getOrNull(i)
                    ?: continue
                val uuidRaw = entry.optString("uuid", "")
                if (uuidRaw.isNotEmpty()) {
                    target.serviceUuid = android.os.ParcelUuid.fromString(uuidRaw)
                }
                target.serviceData = if (entry.isNull("hex")) null
                else StringHelpers.decodeHex(entry.optString("hex", ""))
            }

            DatabaseHelpers.updateAdvertisementSetContent(current)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // ------------------------------------------------------------------
    // Sync missing defaults (must be called on a background thread)
    // ------------------------------------------------------------------

    @Volatile
    private var _syncedMissingDefaults = false

    /**
     * Inserts default sets missing from the DB (e.g. new Model IDs added by
     * an app update) without touching user customizations (deleted flags,
     * edits and custom sets are preserved). Safe to call on every start.
     * @return number of inserted sets
     */
    fun syncMissingDefaults(): Int {
        if (_syncedMissingDefaults) return 0
        _syncedMissingDefaults = true
        return try {
            val db = AppDatabase.getInstance()
            if (db.isSeeding) return 0
            val existing = DatabaseHelpers.getAllAdvertisementSets()
            if (existing.isEmpty()) return 0 // Fresh DB, seeding will handle it
            val existingKeys = existing.map { identityKeyOf(it) }.toSet()
            var inserted = 0
            defaultSetsByKey().forEach { (key, set) ->
                if (!existingKeys.contains(key)) {
                    try {
                        DatabaseHelpers.saveAdvertisementSet(set)
                        inserted++
                    } catch (e: Exception) {
                        // Ignore single failures, keep going
                    }
                }
            }
            inserted
        } catch (e: Exception) {
            0
        }
    }

    // ------------------------------------------------------------------
    // Reset to defaults (must be called on a background thread)
    // ------------------------------------------------------------------

    fun resetToDefaults(): ResetResult {
        var restored = 0
        getModifiedIds().forEach { id ->
            if (applyBackup(id)) restored++
            clearBackup(id)
        }

        val defaults = defaultSetsByKey()
        var remaining = DatabaseHelpers.getAllAdvertisementSets()

        var removedCustoms = 0
        remaining.forEach { set ->
            if (!defaults.containsKey(identityKeyOf(set))) {
                try {
                    DatabaseHelpers.deleteAdvertisementSetTree(set.id)
                    removedCustoms++
                } catch (e: Exception) {
                    // Ignore single failures, keep going
                }
            }
        }

        remaining = DatabaseHelpers.getAllAdvertisementSets()
        val remainingKeys = remaining.map { identityKeyOf(it) }.toSet()
        var readded = 0
        defaults.forEach { (key, set) ->
            if (!remainingKeys.contains(key)) {
                try {
                    DatabaseHelpers.saveAdvertisementSet(set)
                    readded++
                } catch (e: Exception) {
                    // Ignore single failures, keep going
                }
            }
        }

        prefs().edit().clear().apply()

        return ResetResult(restored, removedCustoms, readded)
    }

    fun clearAllCustomizations() {
        prefs().edit().clear().apply()
    }
}
