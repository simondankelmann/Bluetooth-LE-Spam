package de.simon.dankelmann.bluetoothlespam.PermissionCheck

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.simon.dankelmann.bluetoothlespam.Constants.Constants

class PermissionCheck() {
    companion object {

        private val _logTag = "PermissionCheck"

        /**
         * Gets a list of permissions that are relevant for the SDK level we are running on.
         *
         * Deliberately never includes ACCESS_BACKGROUND_LOCATION — that one is owned entirely by
         * the "Advertise/Detect Spam in Background" switches on the Preferences screen (see
         * [de.simon.dankelmann.bluetoothlespam.ui.preferences.PreferencesRoute]) and must never be
         * bundled into a multi-permission request anyway (Android denies it without showing a
         * dialog if requested alongside other still-ungranted permissions).
         */
        fun getAllRelevantPermissions(): List<String> {
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
            }

            // Coarse location is still needed, only fine location can be dropped
            allPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

            return allPermissions
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

        /**
         * Requests every not-yet-granted permission in [permissions] in a single system call.
         * Calling `ActivityCompat.requestPermissions()` separately per permission in a loop is a
         * known Android footgun — each call supersedes the previous one before its dialog can be
         * shown, so only the last permission in the loop ever gets a real prompt and the rest are
         * silently skipped. Batching avoids that.
         */
        fun requestMissingPermissions(permissions: List<String>, activity: Activity) {
            val missing = permissions.filterNot { checkPermission(it, activity) }
            if (missing.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity, missing.toTypedArray(), Constants.REQUEST_CODE_SINGLE_PERMISSION
                )
            }
        }

        fun checkPermission(permission: String, context: Context): Boolean {
            val result = ContextCompat.checkSelfPermission(context, permission)
            return result == PackageManager.PERMISSION_GRANTED
        }
    }
}
