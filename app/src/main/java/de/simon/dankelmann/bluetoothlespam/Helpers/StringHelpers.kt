package de.simon.dankelmann.bluetoothlespam.Helpers

class StringHelpers {

    companion object {
        fun decodeHex(string:String): ByteArray {
            check(string.length % 2 == 0) { "Must have an even length" }

            return string.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }
    }

}