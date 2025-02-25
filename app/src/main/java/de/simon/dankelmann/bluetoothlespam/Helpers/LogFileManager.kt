package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import androidx.documentfile.provider.DocumentFile
import de.simon.dankelmann.bluetoothlespam.Models.LogEntryModel
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import android.content.SharedPreferences

class LogFileManager private constructor() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private var currentLogFile: File? = null
    private var logcatThread: Thread? = null
    private var isLogging = false
    private var isLoggingEnabled = false
    private var customLogDirectory: File? = null
    private var onDirectorySelected: ((File) -> Unit)? = null
    private val PREFS_NAME = "LogFileManagerPrefs"
    private val KEY_CUSTOM_DIRECTORY = "custom_directory_path"
    private val KEY_LOGGING_ENABLED = "logging_enabled"

    fun setCustomLogDirectory(directory: File, context: Context? = null) {
        try {
            if (directory.exists() || directory.mkdirs()) {
                customLogDirectory = directory
                currentLogFile = null
                isLoggingEnabled = true
                Log.d("LogFileManager", "Custom log directory set to: ${directory.absolutePath}")
                
                // Save the directory path to SharedPreferences
                context?.let { ctx ->
                    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(KEY_CUSTOM_DIRECTORY, directory.absolutePath)
                        .putBoolean(KEY_LOGGING_ENABLED, true)
                        .apply()
                    initializeLogFile(ctx)
                    startLogcatCapture(ctx)
                }
            } else {
                Log.e("LogFileManager", "Cannot create or access directory: ${directory.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("LogFileManager", "Error setting custom directory: ${e.message}")
            e.printStackTrace()
        }
    }

    fun initializeLogFile(context: Context) {
        // Try to restore logging state and directory from SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasLoggingEnabled = prefs.getBoolean(KEY_LOGGING_ENABLED, false)
        val savedDirPath = prefs.getString(KEY_CUSTOM_DIRECTORY, null)
        
        if (wasLoggingEnabled && savedDirPath != null) {
            val savedDir = File(savedDirPath)
            if (!savedDir.exists()) {
                savedDir.mkdirs()
            }
            if (savedDir.exists() && savedDir.canWrite()) {
                customLogDirectory = savedDir
                isLoggingEnabled = true
                Log.d("LogFileManager", "Restored custom directory: ${savedDir.absolutePath}")
            } else {
                Log.e("LogFileManager", "Cannot access saved directory: ${savedDir.absolutePath}")
                return
            }
        }

        if (!isLoggingEnabled) {
            Log.d("LogFileManager", "Logging is not enabled, skipping initialization")
            return
        }

        try {
            val timestamp = fileNameDateFormat.format(Date())
            val logFileName = "app_logs_${timestamp}.txt"
            
            val targetDir = customLogDirectory ?: context.getExternalFilesDir("logs").also {
                it?.mkdirs()
            }
            
            if (targetDir?.exists() == true && targetDir.canWrite()) {
                val newLogFile = File(targetDir, logFileName)
                if (newLogFile.createNewFile()) {
                    currentLogFile = newLogFile
                    writeToLog("Log file initialized: ${logFileName}", "INFO", context)
                    Log.d("LogFileManager", "Created new log file: ${newLogFile.absolutePath}")
                } else {
                    Log.e("LogFileManager", "Failed to create log file in directory: ${targetDir.absolutePath}")
                    isLoggingEnabled = false
                    return
                }
            } else {
                Log.e("LogFileManager", "Target directory is not writable: ${targetDir?.absolutePath}")
                isLoggingEnabled = false
                return
            }
            
            // Start logging if not already started
            if (logcatThread?.isAlive != true) {
                startLogcatCapture(context)
            }
        } catch (e: Exception) {
            Log.e("LogFileManager", "Error initializing log file: ${e.message}")
            e.printStackTrace()
            isLoggingEnabled = false
        }
    }

    fun writeToLog(message: String, level: String, context: Context) {
        if (!isLoggingEnabled) return
        synchronized(this) {
            try {
                val timestamp = dateFormat.format(Date())
                val logMessage = "$timestamp [$level] $message\n"
                val logFile = getLogFile(context)
                if (logFile.exists() && logFile.canWrite()) {
                    FileWriter(logFile, true).use { writer ->
                        writer.write(logMessage)
                        writer.flush()
                    }
                } else {
                    Log.e("LogFileManager", "Log file not accessible: ${logFile.absolutePath}")
                    initializeLogFile(context)
                }
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error writing to log: ${e.message}")
                e.printStackTrace()
                // Try to reinitialize the log file
                try {
                    initializeLogFile(context)
                } catch (initError: Exception) {
                    Log.e("LogFileManager", "Failed to reinitialize log file: ${initError.message}")
                }
            }
        }
    }

    private fun getLogFile(context: Context): File {
        return currentLogFile ?: run {
            initializeLogFile(context)
            currentLogFile ?: throw IllegalStateException("Failed to initialize log file")
        }
    }

    fun enableLogging(context: Context, launcher: ActivityResultLauncher<Intent>, onDirectoryPicked: (File) -> Unit): Boolean {
        if (context !is FragmentActivity) {
            Log.e("LogFileManager", "Context must be a FragmentActivity to show folder picker")
            return false
        }

        onDirectorySelected = { directory ->
            setCustomLogDirectory(directory, context)
            onDirectoryPicked(directory)
        }

        val directoryPicker = LogDirectoryPicker(context)
        directoryPicker.pickDirectory(onDirectorySelected!!)
        return true
    }

    companion object {
        @Volatile
        private var instance: LogFileManager? = null
        const val FOLDER_PICKER_REQUEST_CODE = 1001

        fun getInstance(): LogFileManager {
            return instance ?: synchronized(this) {
                instance ?: LogFileManager().also { instance = it }
            }
        }
    }

    fun disableLogging(context: Context? = null) {
        isLoggingEnabled = false
        stopLogging()
        
        // Save the disabled state to SharedPreferences
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_LOGGING_ENABLED, false).apply()
        }
    }

    private fun startLogcatCapture(context: Context) {
        if (logcatThread?.isAlive == true) {
            stopLogging()
        }

        isLogging = true
        logcatThread = thread {
            var process: Process? = null
            var bufferedReader: BufferedReader? = null
            while (isLoggingEnabled) {
                try {
                    if (process == null || bufferedReader == null) {
                        process = Runtime.getRuntime().exec("logcat -v time")
                        bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    }

                    val line = bufferedReader.readLine()
                    if (line != null) {
                        val level = when {
                            line.contains(" E ") -> "ERROR"
                            line.contains(" W ") -> "WARN"
                            line.contains(" I ") -> "INFO"
                            line.contains(" D ") -> "DEBUG"
                            line.contains(" V ") -> "VERBOSE"
                            else -> "INFO"
                        }
                        synchronized(this) {
                            if (isLoggingEnabled) {
                                writeToLog(line, level, context)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LogFileManager", "Error capturing logcat: ${e.message}")
                    e.printStackTrace()
                    try {
                        bufferedReader?.close()
                        process?.destroy()
                    } catch (cleanupError: Exception) {
                        Log.e("LogFileManager", "Error cleaning up resources: ${cleanupError.message}")
                    }
                    bufferedReader = null
                    process = null
                    Thread.sleep(1000) // Wait before retrying
                }
            }
            try {
                bufferedReader?.close()
                process?.destroy()
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error cleaning up logcat resources: ${e.message}")
            }
        }
    }

    fun stopLogging() {
        isLoggingEnabled = false
        isLogging = false
        try {
            logcatThread?.let { thread ->
                if (thread.isAlive) {
                    thread.interrupt()
                    thread.join(5000) // Wait up to 5 seconds for thread to finish
                    if (thread.isAlive) {
                        Log.w("LogFileManager", "Logging thread did not terminate gracefully")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LogFileManager", "Error stopping logging thread: ${e.message}")
            e.printStackTrace()
        } finally {
            logcatThread = null
            currentLogFile = null
        }
    }

    fun writeToLog(logEntry: LogEntryModel, context: Context) {
        if (!isLoggingEnabled) return
        writeToLog(logEntry.message, logEntry.level.toString(), context)
    }

    fun getLogDirectory(context: Context): File? {
        return context.getExternalFilesDir("logs")
    }

    fun requestManageAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permission not required for this Android version", Toast.LENGTH_SHORT).show()
        }
    }
}
