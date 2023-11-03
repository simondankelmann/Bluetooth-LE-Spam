package de.simon.dankelmann.bluetoothlespam.PermissionCheck

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Constants.Constants

class PermissionCheck (){
    companion object {
        private val _logTag = "PermissionCheck"
        fun checkPermission(permission:String, activity: Activity):Boolean{
            if (permission == "android.permission.BLUETOOTH_ADVERTISE" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            {
                // android.permission.BLUETOOTH_ADMIN was first introduced in api level 31
                return true
            }

            if(ContextCompat.checkSelfPermission(AppContext.getContext(), permission) == PackageManager.PERMISSION_GRANTED){
                //Log.d(_logTag, "Permission granted: $permission")
                return true
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), Constants.REQUEST_CODE_SINGLE_PERMISSION)
            }
            Log.d(_logTag, "Permission not granted: $permission")
            return false
        }

        fun requireAllPermissions(activity: Activity, permissions: Array<String>){
            var requestNeeded: MutableList<String> = mutableListOf()
            var dialogNeeded: MutableList<String> = mutableListOf()

            permissions.forEachIndexed { index, permission ->
                if(ContextCompat.checkSelfPermission(AppContext.getContext(), permission) == PackageManager.PERMISSION_GRANTED){
                    //Log.d(_logTag, "Permission granted: $permission")
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                        // Show an explanation asynchronously
                        Log.d(_logTag, "Show explanation for: $permission")
                        dialogNeeded.add(permission)
                    } else {
                        requestNeeded.add(permission)
                    }
                }
            }

            // SHOW DIALOG
            if(requestNeeded.size > 0){
                requestPermissions(activity, requestNeeded.toTypedArray())
                // showAlert(activity, requestNeeded.toTypedArray())
            }

            if(dialogNeeded.size > 0){
                showArrayAlert(activity, dialogNeeded)
                /*
                dialogNeeded.forEachIndexed { index, permission ->
                    run {
                        showAlert(activity, permission)
                    }
                }*/
            }
        }

        private fun showAlert(activity: Activity, permission:String) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Requesting permission")
            builder.setMessage("App requires the following permission: $permission, please grant it in order to proceed")
            builder.setPositiveButton("OK", { dialog, which -> requestPermissions(activity, arrayOf(permission)) })
            builder.setNeutralButton("Cancel", null)
            val dialog = builder.create()
            dialog.show()
        }

        private fun showArrayAlert(activity: Activity, permissions:MutableList<String>) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Requesting permission")
            builder.setMessage("App requires the following permission: ${permissions.toString()}, please grant it in order to proceed")
            builder.setPositiveButton("OK") { dialog, which ->
                requestPermissions(
                    activity,
                    permissions.toTypedArray()
                )
            }
            builder.setNeutralButton("Cancel", null)
            val dialog = builder.create()
            dialog.show()
        }

        private fun requestPermissions(activity: Activity, permissions: Array<String>){
            ActivityCompat.requestPermissions(activity, permissions, Constants.REQUEST_CODE_MULTIPLE_PERMISSIONS)
        }

        fun processPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
            var result = 0
            if (grantResults.isNotEmpty()) {
                for (item in grantResults) {
                    result += item
                }
            }
            if (result == PackageManager.PERMISSION_GRANTED) return true
            return false
        }
    }
}