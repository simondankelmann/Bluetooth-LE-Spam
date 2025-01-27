package de.simon.dankelmann.bluetoothlespam.PermissionCheck

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.simon.dankelmann.bluetoothlespam.Constants.Constants

class PermissionCheck (){
    companion object {

        private val _logTag = "PermissionCheck"

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
            if (permission == "android.permission.BLUETOOTH_ADVERTISE" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // android.permission.BLUETOOTH_ADVERTISE was first introduced in api level 31
                return true
            }

            if (permission == "android.permission.BLUETOOTH_CONNECT" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            {
                // android.permission.BLUETOOTH_CONNECT was first introduced in api level 31
                return true
            }

            if (permission == "android.permission.BLUETOOTH_SCAN" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            {
                // android.permission.BLUETOOTH_CONNECT was first introduced in api level 31
                return true
            }

            if ((permission == "android.permission.BLUETOOTH" || permission == "android.permission.BLUETOOTH_ADMIN") && Build.VERSION.SDK_INT > Build.VERSION_CODES.R)
            {
                // android.permission.BLUETOOTH and android.permission.BLUETOOTH_ADMIN have a max sdk version of 30
                return true
            }

            if (permission == "android.permission.FOREGROUND_SERVICE_LOCATION" && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                // android.permission.FOREGROUND_SERVICE_LOCATION was first introduced in api level 29
                return true
            }

            if (permission == "android.permission.ACCESS_BACKGROUND_LOCATION" && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                // android.permission.ACCESS_BACKGROUND_LOCATION was first introduced in api level 29
                return true
            }

            if (permission == "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                // android.permission.FOREGROUND_SERVICE_SPECIAL_USE was first introduced in api level 29
                return true
            }

            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
