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
import java.io.IOException
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
    private val KEY_LOGGER_STATE = "logger_state"
    private val DIRECTORY_FILE_NAME = "custom_directory.txt"

    init {
        // Instance will be available during initialization
        instance = this
    }

    private fun restoreLoggingState(context: Context) {
        synchronized(this) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedLoggingEnabled = prefs.getBoolean(KEY_LOGGING_ENABLED, false)
                val savedDirPath = prefs.getString(KEY_CUSTOM_DIRECTORY, null)

                if (savedLoggingEnabled && savedDirPath != null) {
                    val savedDir = File(savedDirPath)
                    if (savedDir.exists() && savedDir.canWrite()) {
                        // Verify directory is actually writable by testing file creation
                        val testFile = File(savedDir, ".test_write")
                        try {
                            if (testFile.createNewFile()) {
                                testFile.delete() // Clean up test file
                                customLogDirectory = savedDir
                                isLoggingEnabled = true
                                Log.d("LogFileManager", "Restored logging state with directory: ${savedDir.absolutePath}")
                                return
                            } else {
                                Log.e("LogFileManager", "Failed write test in directory: ${savedDir.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.e("LogFileManager", "Error testing directory write access: ${e.message}")
                        } finally {
                            testFile.delete() // Ensure test file is cleaned up
                        }
                    } else {
                        Log.e("LogFileManager", "Saved directory is not accessible: ${savedDir.absolutePath}")
                    }
                    clearLoggingState(context)
                }
                
                Log.d("LogFileManager", "No valid logging state to restore. Last known directory: ${customLogDirectory?.absolutePath ?: "none"}")
                clearLoggingState(context)
                
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error restoring logging state: ${e.message}")
                e.printStackTrace()
                clearLoggingState(context)
            }
        }
    }

    private fun clearLoggingState(context: Context) {
        isLoggingEnabled = false
        isLogging = false
        customLogDirectory = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            clear()
            apply()
        }
    }

    private fun isLogDirectoryValid(): Boolean {
        return customLogDirectory?.let { dir ->
            dir.exists() && dir.canWrite()
        } ?: false
    }

    fun initialize(context: Context) {
        synchronized(this) {
            try {
                // First restore the logging state
                restoreLoggingState(context)
                
                // Stop any existing logging first
                stopLogging()
                
                // Try to restore from SharedPreferences first
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val prefsDirPath = prefs.getString(KEY_CUSTOM_DIRECTORY, null)
                val prefsLoggingEnabled = prefs.getBoolean(KEY_LOGGING_ENABLED, false)
                
                // Then check the backup file
                val dirFile = File(context.dataDir, DIRECTORY_FILE_NAME)
                val fileDirPath = if (dirFile.exists()) {
                    try {
                        dirFile.readText().trim().takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        Log.e("LogFileManager", "Error reading backup file: ${e.message}")
                        null
                    }
                } else null
                
                // Determine which path to use (prefer SharedPreferences, fall back to file)
                val finalDirPath = when {
                    prefsDirPath != null -> prefsDirPath
                    fileDirPath != null -> fileDirPath
                    else -> null
                }
                
                if (finalDirPath != null) {
                    val savedDir = File(finalDirPath)
                    if (savedDir.exists() && savedDir.canWrite()) {
                        // Ensure directory is actually writable by testing file creation
                        val testFile = File(savedDir, ".test_write")
                        try {
                            if (testFile.createNewFile()) {
                                testFile.delete() // Clean up test file
                                
                                // Synchronize both storage mechanisms
                                dirFile.parentFile?.mkdirs() // Ensure parent directory exists
                                dirFile.writeText(finalDirPath)
                                
                                prefs.edit().apply {
                                    putString(KEY_CUSTOM_DIRECTORY, finalDirPath)
                                    putBoolean(KEY_LOGGING_ENABLED, true)
                                    putBoolean(KEY_LOGGER_STATE, true)
                                    commit() // Use commit for immediate write
                                }
                                
                                // Update memory state
                                customLogDirectory = savedDir
                                isLoggingEnabled = true
                                
                                try {
                                    if (isLogDirectoryValid()) {
                                        initializeLogFile(context)
                                        startLogcatCapture(context)
                                        Log.d("LogFileManager", "Logging initialized successfully with directory: ${savedDir.absolutePath}")
                                        return
                                    }
                                } catch (e: Exception) {
                                    Log.e("LogFileManager", "Failed to initialize logging: ${e.message}")
                                    e.printStackTrace()
                                }
                            } else {
                                Log.e("LogFileManager", "Failed write test in directory: ${savedDir.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.e("LogFileManager", "Error testing directory write access: ${e.message}")
                        } finally {
                            testFile.delete() // Ensure test file is cleaned up
                        }
                    } else {
                        Log.e("LogFileManager", "Saved directory is not accessible: ${savedDir.absolutePath}")
                        // Clean up invalid state
                        dirFile.delete()
                        prefs.edit().apply {
                            remove(KEY_CUSTOM_DIRECTORY)
                            putBoolean(KEY_LOGGING_ENABLED, false)
                            commit()
                        }
                    }
                }
                
                // If we reach here, no valid state was found or initialization failed
                Log.d("LogFileManager", "No valid logging state to restore. Last known directory: ${customLogDirectory?.absolutePath ?: "none"}")
                disableLogging(context)
                
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error during initialization: ${e.message}")
                e.printStackTrace()
                disableLogging(context)
            }
        }
    }

    fun setCustomLogDirectory(directory: File, context: Context) {
        synchronized(this) {
            try {
                // First validate the directory
                if (!directory.exists()) {
                    if (!directory.mkdirs()) {
                        Log.e("LogFileManager", "Cannot create directory: ${directory.absolutePath}")
                        throw IOException("Failed to create directory")
                    }
                }

                // Verify write permissions
                if (!directory.canWrite()) {
                    Log.e("LogFileManager", "Cannot write to directory: ${directory.absolutePath}")
                    throw IOException("No write permission for directory")
                }

                // Create a text file to store the directory path
                val dirFile = File(context.dataDir, DIRECTORY_FILE_NAME)
                try {
                    // Ensure parent directory exists
                    dirFile.parentFile?.mkdirs()
                    dirFile.writeText(directory.absolutePath)
                    Log.d("LogFileManager", "Directory path saved to: ${dirFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e("LogFileManager", "Failed to write directory path to backup file: ${e.message}")
                    e.printStackTrace()
                    // Continue anyway as we'll still save to SharedPreferences
                }

                // Stop existing logging first to ensure clean state
                stopLogging()

                // Save state to preferences BEFORE updating memory state
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().apply {
                    clear() // Clear any existing state first
                    putString(KEY_CUSTOM_DIRECTORY, directory.absolutePath)
                    putBoolean(KEY_LOGGING_ENABLED, true)
                    putBoolean(KEY_LOGGER_STATE, true)
                    commit() // Use commit() for synchronous write
                }

                // Update memory state
                customLogDirectory = directory
                isLoggingEnabled = true

                // Initialize new log file and start capture
                initializeLogFile(context)
                startLogcatCapture(context)
                Log.d("LogFileManager", "Custom log directory set and logging started: ${directory.absolutePath}")
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error setting custom directory: ${e.message}")
                e.printStackTrace()
                // Reset memory state
                isLoggingEnabled = false
                isLogging = false
                customLogDirectory = null
                
                // Clear preferences on failure
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                    remove(KEY_CUSTOM_DIRECTORY)
                    putBoolean(KEY_LOGGING_ENABLED, false)
                    putBoolean(KEY_LOGGER_STATE, false)
                    commit() // Use commit() to ensure immediate cleanup
                }
                
                throw e // Re-throw the exception to notify the caller
            }
        }
    }

    fun initializeLogFile(context: Context) {
        try {
            // Only use the custom directory if set, don't fall back to default
            val logDir = customLogDirectory
            if (logDir == null) {
                Log.e("LogFileManager", "No custom directory set. Current directory path: null")
                isLoggingEnabled = false
                return
            }

            // Ensure the directory exists and is writable
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.e("LogFileManager", "Cannot create directory: ${logDir.absolutePath}")
                isLoggingEnabled = false
                return
            }

            if (!logDir.canWrite()) {
                Log.e("LogFileManager", "Cannot write to directory: ${logDir.absolutePath}")
                isLoggingEnabled = false
                return
            }

            // Directory is valid, enable logging
            isLoggingEnabled = true
            Log.d("LogFileManager", "Using log directory: ${logDir.absolutePath}\nFull directory path: ${logDir.canonicalPath}")

            // Create new log file
            val timestamp = fileNameDateFormat.format(Date())
            val logFileName = "app_logs_${timestamp}.txt"
            val newLogFile = File(logDir, logFileName)

            // Check if file already exists to avoid false error
            if (!newLogFile.exists() && !newLogFile.createNewFile()) {
                Log.e("LogFileManager", "Failed to create log file: ${newLogFile.absolutePath}")
                isLoggingEnabled = false
                return
            }

            currentLogFile = newLogFile
            writeToLog("Log file initialized: ${logFileName}", "INFO", context)
            Log.d("LogFileManager", "Created new log file: ${newLogFile.absolutePath}")
            
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
                val logFile = getLogFile(context)
                if (!logFile.exists() || !logFile.canWrite()) {
                    Log.e("LogFileManager", "Log file not accessible, reinitializing...")
                    initializeLogFile(context)
                    return
                }

                val timestamp = dateFormat.format(Date())
                val logMessage = "$timestamp [$level] $message\n"

                try {
                    FileWriter(logFile, true).use { writer ->
                        writer.write(logMessage)
                        writer.flush()
                    }
                } catch (e: Exception) {
                    Log.e("LogFileManager", "Failed to write to log file: ${e.message}")
                    // If writing fails, try to reinitialize the log file
                    initializeLogFile(context)
                }
            } catch (e: Exception) {
                Log.e("LogFileManager", "Critical error in writeToLog: ${e.message}")
                e.printStackTrace()
                isLoggingEnabled = false
                stopLogging()
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

        // Check if we already have a valid directory and logging is enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDirPath = prefs.getString(KEY_CUSTOM_DIRECTORY, null)
        if (savedDirPath != null) {
            val savedDir = File(savedDirPath)
            if (savedDir.exists() && savedDir.canWrite()) {
                setCustomLogDirectory(savedDir, context)
                onDirectoryPicked(savedDir)
                // Ensure logging state is saved
                prefs.edit().putBoolean(KEY_LOGGING_ENABLED, true).apply()
                return true
            }
        }

        // If no valid directory exists, show directory picker
        onDirectorySelected = { directory ->
            setCustomLogDirectory(directory, context)
            onDirectoryPicked(directory)
            // Ensure logging state is saved
            prefs.edit().putBoolean(KEY_LOGGING_ENABLED, true).apply()
        }

        val directoryPicker = LogDirectoryPicker(context)
        directoryPicker.initialize(launcher)
        directoryPicker.pickDirectory(onDirectorySelected!!)
        return true
    }

    fun getLogDirectory(context: Context): File? {
        return customLogDirectory
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
    
    fun isLoggingEnabledAndValid(): Boolean {
        return isLoggingEnabled && isLogDirectoryValid()
    }

    fun disableLogging(context: Context) {
        synchronized(this) {
            stopLogging()
            isLoggingEnabled = false
            isLogging = false
            
            // Clear all logging-related preferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                clear()
                commit() // Use commit instead of apply for immediate effect
            }
            
            // Clean up resources
            try {
                currentLogFile?.let { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error cleaning up log file: ${e.message}")
            }
            
            customLogDirectory = null
            currentLogFile = null
            
            Log.d("LogFileManager", "Logging disabled and resources cleaned up")
        }
    }

    private fun startLogcatCapture(context: Context) {
        synchronized(this) {
            if (logcatThread?.isAlive == true) {
                Log.d("LogFileManager", "Logging thread already running")
                return
            }

            // Validate directory and state before proceeding
            if (!isLogDirectoryValid()) {
                Log.e("LogFileManager", "Cannot start logging: Directory ${customLogDirectory?.absolutePath ?: "null"} is not valid or writable")
                isLoggingEnabled = false
                isLogging = false
                return
            }

            // Initialize log file if needed
            if (currentLogFile == null || !currentLogFile!!.exists()) {
                initializeLogFile(context)
            }

            if (currentLogFile == null) {
                Log.e("LogFileManager", "Failed to initialize log file")
                isLoggingEnabled = false
                isLogging = false
                return
            }

            // Update logging state
            isLoggingEnabled = true
            isLogging = true
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putBoolean(KEY_LOGGING_ENABLED, true)
                putBoolean(KEY_LOGGER_STATE, true)
                apply()
            }

            logcatThread = thread {
                var process: Process? = null
                var bufferedReader: BufferedReader? = null

                try {
                    // Clear the existing logcat buffer
                    Runtime.getRuntime().exec("logcat -c").waitFor()
                    
                    // Start logcat with time format
                    process = Runtime.getRuntime().exec("logcat -v threadtime")
                    bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

                    var line: String?
                    while (isLoggingEnabled && !Thread.currentThread().isInterrupted) {
                        line = bufferedReader.readLine()
                        if (line == null) break
                        
                        val logLine = line
                        val level = when {
                            logLine.contains(" E ") -> "ERROR"
                            logLine.contains(" W ") -> "WARN"
                            logLine.contains(" I ") -> "INFO"
                            logLine.contains(" D ") -> "DEBUG"
                            logLine.contains(" V ") -> "VERBOSE"
                            else -> "INFO"
                        }

                        synchronized(this) {
                            if (isLoggingEnabled && currentLogFile?.exists() == true) {
                                writeToLog(logLine, level, context)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LogFileManager", "Error capturing logcat: ${e.message}")
                    e.printStackTrace()
                } finally {
                    try {
                        bufferedReader?.close()
                        process?.destroy()
                    } catch (cleanupError: Exception) {
                        Log.e("LogFileManager", "Error cleaning up resources: ${cleanupError.message}")
                    }
                    isLogging = false
                    isLoggingEnabled = false
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putBoolean(KEY_LOGGING_ENABLED, false)
                        putBoolean(KEY_LOGGER_STATE, false)
                        apply()
                    }
                }
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

    fun isLoggingEnabledAndValid(): Boolean {
        return customLogDirectory?.let { dir ->
            dir.exists() && dir.canWrite()
        } ?: false
    }

    private fun setLoggingState(enabled: Boolean, context: Context?) {
        val wasEnabled = isLoggingEnabled
        isLoggingEnabled = enabled && isLogDirectoryValid()
        isLogging = isLoggingEnabled

        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean(KEY_LOGGING_ENABLED, isLoggingEnabled)
                putBoolean(KEY_LOGGER_STATE, isLogging)
                if (!isLoggingEnabled) {
                    remove(KEY_CUSTOM_DIRECTORY)
                }
                apply()
            }

            // Handle state transitions
            when {
                !wasEnabled && isLoggingEnabled -> {
                    initializeLogFile(ctx)
                    startLogcatCapture(ctx)
                }
                wasEnabled && !isLoggingEnabled -> {
                    stopLogging()
                }
            }
        }
    }

    fun checkLoggingState(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLoggingEnabled = prefs.getBoolean(KEY_LOGGING_ENABLED, false)
        val savedDirPath = prefs.getString(KEY_CUSTOM_DIRECTORY, null)

        if (savedLoggingEnabled && savedDirPath != null) {
            val savedDir = File(savedDirPath)
            if (savedDir.exists() && savedDir.canWrite()) {
                customLogDirectory = savedDir
                setLoggingState(true, context)
                return true
            } else {
                // Invalid directory, clean up preferences
                prefs.edit().apply {
                    remove(KEY_CUSTOM_DIRECTORY)
                    putBoolean(KEY_LOGGING_ENABLED, false)
                    apply()
                }
            }
        }

        // Ensure logging state is set correctly
        setLoggingState(false, context)
        return false
    }
}
