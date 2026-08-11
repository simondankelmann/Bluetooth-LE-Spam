package de.simon.dankelmann.bluetoothlespam.PermissionCheck

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository

class PermissionCheck() {
    companion object {

        private val _logTag = "PermissionCheck"

        /**
         * Gets a list of permissions that are relevant for the SDK level we are running on.
         *
         * ACCESS_BACKGROUND_LOCATION is only pulled in once the user opts into background spam
         * detection or background advertising (Preferences screen) — requesting it upfront for
         * everyone would nag users who never need background operation.
         */
        fun getAllRelevantPermissions(context: Context): List<String> {
            val allPermissions = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                allPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                allPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                allPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                allPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                allPermissions.add(Manifest.permission.BLUETOOTH)
                allPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)

                // On SDK 31 "S" and above, we declare in the manifest that we won't use Bluetooth to get the location
                allPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isBackgroundOperationEnabled(context)) {
                    allPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            // Coarse location is still needed, only fine location can be dropped
            allPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

            return allPermissions
        }

        private fun isBackgroundOperationEnabled(context: Context): Boolean {
            val settings = SettingsRepository.getInstance(context).current
            val spamDetectionBackgroundEnabled = settings[SettingsKeys.SPAM_DETECTION_BACKGROUND_ENABLED] ?: false
            val advertisingBackgroundEnabled = settings[SettingsKeys.ADVERTISING_BACKGROUND_ENABLED] ?: false
            return spamDetectionBackgroundEnabled || advertisingBackgroundEnabled
        }

        fun checkPermissionAndRequest(permission: String, activity: Activity): Boolean {
            val isGranted = checkPermission(permission, activity)
            if (!isGranted) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(permission), Constants.REQUEST_CODE_SINGLE_PERMISSION
                )
            }
            return isGranted
        }

        fun checkPermission(permission: String, context: Context): Boolean {
            val result = ContextCompat.checkSelfPermission(context, permission)
            return result == PackageManager.PERMISSION_GRANTED
        }
    }
}
