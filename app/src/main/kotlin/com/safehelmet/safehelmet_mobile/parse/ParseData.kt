package com.safehelmet.safehelmet_mobile.parse

class ParseData(byteArray: ByteArray) : BaseParse(byteArray) {

    var temp: Float = 0.0F
    var hum: Float = 0.0F
    var lux: Float = 0.0F
    var gas: BooleanArray = BooleanArray(3) { false } // 3 bit
    var wearables: BooleanArray = BooleanArray(2) { false } // 5 bit

    override val sensorLengths: Map<String, Int> = mapOf(
        "temp" to 4,
        "hum" to 4,
        "lux" to 4,
        "gas" to 1,
        "wearables" to 1
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        temp = extractFloatSensor("temp", offset)
        hum = extractFloatSensor("hum", offset)
        lux = extractFloatSensor("lux", offset)
        gas = extractBitsSensor("gas", 0, 2, offset)
        wearables = extractBitsSensor("anomaly", 0, 1, offset)
    }


    fun printValues(): String {
        return buildString {
            append("Temperature: $temp, ")
            append("Humidity: $hum, ")
            append("Lux: $lux, ")
            append("Gas values: ")
            for (g in gas) append(if (g) "1" else "0")
            append(", ")

            append("Anomaly values: ")
            for (w in wearables) append(if (w) "1" else "0")

        }
    }
}
