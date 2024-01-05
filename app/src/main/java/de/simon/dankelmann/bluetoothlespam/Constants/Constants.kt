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


        // ManufacturerIds
        const val MANUFACTURER_ID_APPLE = 76 // 0x004c == 76 = Apple
        const val MANUFACTURER_ID_MICROSOFT = 6 // 0x0006 == 6 = Microsoft
        const val MANUFACTURER_ID_SAMSUNG = 117 // 0x75 == 117 = Samsung
        const val MANUFACTURER_ID_TYPO_PRODUCTS = 255 // 0xFF == 255 == Typo Products, LLC

    }
}