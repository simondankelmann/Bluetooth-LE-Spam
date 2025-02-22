package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Models.LogEntryModel
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class LogFileManager private constructor() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private var currentLogFile: File? = null
    private var logcatThread: Thread? = null
    private var isLogging = false
    private var isLoggingEnabled = false

    companion object {
        @Volatile
        private var instance: LogFileManager? = null

        fun getInstance(): LogFileManager {
            return instance ?: synchronized(this) {
                instance ?: LogFileManager().also { instance = it }
            }
        }
    }

    fun enableLogging(context: Context) {
        isLoggingEnabled = true
        initializeLogFile(context)
    }

    fun disableLogging() {
        isLoggingEnabled = false
        stopLogging()
    }

    fun initializeLogFile(context: Context) {
        if (!isLoggingEnabled) return

        try {
            val timestamp = fileNameDateFormat.format(Date())
            val logFileName = "app_logs_${timestamp}.txt"
            
            val directory = getLogDirectory(context)
            directory?.mkdirs()
            
            currentLogFile = directory?.let { dir ->
                File(dir, logFileName).apply {
                    if (!exists()) {
                        createNewFile()
                        writeToLog("Log file initialized: ${name}", "INFO", context)
                        startLogcatCapture(context)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLogcatCapture(context: Context) {
        if (logcatThread?.isAlive == true) return

        isLogging = true
        logcatThread = thread {
            var process: Process? = null
            var bufferedReader: BufferedReader? = null
            try {
                process = Runtime.getRuntime().exec("logcat -v time")
                bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

                while (isLogging) {
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
                        writeToLog(line, level, context)
                    }
                }
            } catch (e: Exception) {
                Log.e("LogFileManager", "Error capturing logcat: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    bufferedReader?.close()
                    process?.destroy()
                } catch (e: Exception) {
                    Log.e("LogFileManager", "Error cleaning up logcat resources: ${e.message}")
                }
            }
        }
    }

    fun stopLogging() {
        isLogging = false
        logcatThread?.join()
        logcatThread = null
    }

    private fun getLogFile(context: Context): File {
        return currentLogFile ?: run {
            initializeLogFile(context)
            currentLogFile!!
        }
    }

    fun writeToLog(logEntry: LogEntryModel, context: Context) {
        if (!isLoggingEnabled) return
        writeToLog(logEntry.message, logEntry.level.toString(), context)
    }

    private fun writeToLog(message: String, level: String, context: Context) {
        if (!isLoggingEnabled) return
        try {
            val logFile = getLogFile(context)
            val timestamp = dateFormat.format(Date())
            val logMessage = "$timestamp [$level] $message\n"
            
            FileWriter(logFile, true).use { writer ->
                writer.write(logMessage)
                writer.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogDirectory(context: Context): File? {
        return context.getExternalFilesDir(null)
    }
}
