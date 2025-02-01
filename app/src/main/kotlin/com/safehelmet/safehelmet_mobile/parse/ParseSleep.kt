package com.safehelmet.safehelmet_mobile.parse

class ParseSleep(byteArray: ByteArray) : BaseParse(byteArray) {

    var sleep: BooleanArray = BooleanArray(1) { false }

    override val sensorLengths: Map<String, Int> = mapOf(
        "sleep" to 1
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        sleep = extractBitsSensor("sleep", 0, 0, offset) // Firsts 5 bit
    }

    override fun printValues(): String {
        return buildString {
            for (w in sleep) append(if (w) "1" else "0")
        }
    }
}