package com.safehelmet.safehelmet_mobile.parse

class ParseSleep(byteArray: ByteArray) : BaseParse(byteArray) {

    var sleep: Boolean = false

    override val sensorLengths: Map<String, Int> = mapOf(
        "sleep" to 1
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        sleep = extractBitsSensor("sleep", 0, 0, offset)[0]
    }

    override fun printValues(): String {
        return buildString {
            "sleep ${sleep}"
        }
    }
}