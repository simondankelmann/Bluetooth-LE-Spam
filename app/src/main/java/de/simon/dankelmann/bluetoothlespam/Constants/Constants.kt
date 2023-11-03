package de.simon.dankelmann.bluetoothlespam.Constants


enum class LogLevel{
    Error, Warning, Info, Success
}

class Constants {
    companion object {
        const val ADVERTISE_TIMEOUT = 160
        const val UUID_GOOGLE_FAST_PAIRING = "0000fe2c-0000-1000-8000-00805f9b34fb"

        // Request Codes
        const val REQUEST_CODE_ENABLE_BLUETOOTH = 1
        const val REQUEST_CODE_MULTIPLE_PERMISSIONS = 2
        const val REQUEST_CODE_SINGLE_PERMISSION = 3

    }
}