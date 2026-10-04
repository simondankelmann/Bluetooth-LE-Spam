package de.simon.dankelmann.bluetoothlespam.Helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class LogDirectoryPicker(private val activity: FragmentActivity) {

    private val _logTag = "LogDirectoryPicker"
    private var onDirectorySelected: ((File) -> Unit)? = null
    private lateinit var directoryPickerLauncher: ActivityResultLauncher<Intent>

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun initialize(launcher: ActivityResultLauncher<Intent>) {
        directoryPickerLauncher = launcher
    }

    fun pickDirectory(callback: (File) -> Unit) {
        onDirectorySelected = callback
        Log.d(_logTag, "pickDirectory called")
        if (!hasAllFilesPermission()) {
            Log.d(_logTag, "Requesting storage permission")
            requestStoragePermission()
            return
        }
        Log.d(_logTag, "Launching directory picker")
        launchDirectoryPicker()
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Log.d(_logTag, "Requesting manage all files access permission")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e(_logTag, "Error requesting storage permission: ${e.message}")
                e.printStackTrace()
            }
        } else {
            // For Android versions below 11, request WRITE_EXTERNAL_STORAGE permission
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        }
    }

    private fun launchDirectoryPicker() {
        Log.d(_logTag, "Launching directory picker intent")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        directoryPickerLauncher.launch(intent)
    }

    fun handleResult(uri: Uri?) {
        Log.d(_logTag, "handleResult called with URI: $uri")
        if (!hasAllFilesPermission()) {
            Log.e(_logTag, "All files access permission not granted")
            return
        }
        uri?.let {
            try {
                Log.d(_logTag, "Taking persistable URI permission")
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                activity.contentResolver.takePersistableUriPermission(uri, takeFlags)

                Log.d(_logTag, "Getting DocumentFile from URI")
                val documentFile = DocumentFile.fromTreeUri(activity, uri)
                if (documentFile?.isDirectory == true && documentFile.canWrite()) {
                    val path = getPathFromUri(uri)
                    val selectedDir = if (path != null) File(path) else null
                    if (selectedDir != null && (selectedDir.exists() || selectedDir.mkdirs())) {
                        Log.d(_logTag, "Successfully created/accessed directory: ${selectedDir.absolutePath}")
                        onDirectorySelected?.invoke(selectedDir)
                    } else {
                        Log.e(_logTag, "Failed to create/access directory")
                    }
                } else {
                    Log.e(_logTag, "Selected location is not a writable directory")
                }
            } catch (e: Exception) {
                Log.e(_logTag, "Error handling directory selection: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            if (split.size >= 2) {
                val type = split[0]
                var path = split[1]
                if (type == "primary") {
                    path = Environment.getExternalStorageDirectory().absolutePath + "/" + path
                    Log.d(_logTag, "Resolved path: $path")
                    return path
                } else {
                    // Handle non-primary volumes
                    val storageDir = File("/storage/", type)
                    if (storageDir.exists()) {
                        path = storageDir.absolutePath + "/" + path
                        Log.d(_logTag, "Resolved path: $path")
                        return path
                    }
                }
            }
            return null
        } catch (e: Exception) {
            Log.e(_logTag, "Error getting path from URI: ${e.message}")
            return null
        }
    }
}