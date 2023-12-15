package de.simon.dankelmann.bluetoothlespam.Helpers

class StringHelpers {

    companion object {
        fun decodeHex(string:String): ByteArray {
            check(string.length % 2 == 0) { "Must have an even length" }

            return string.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }
        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

        fun intToHexString(input:Int):String{
            return String.format("%02x", input)
        }

        fun byteToHexString(input:Byte):String{
            return String.format("%02x", input)
        }




    }

}