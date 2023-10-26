package de.simon.dankelmann.bluetoothlespam.Constants


enum class LogLevel{
    Error, Warning, Info, Success
}

class Constants {
    companion object {
        const val ADVERTISE_TIMEOUT = 160

        const val UUID_GOOGLE_FAST_PAIRING = "0000fe2c-0000-1000-8000-00805f9b34fb"
    }
}