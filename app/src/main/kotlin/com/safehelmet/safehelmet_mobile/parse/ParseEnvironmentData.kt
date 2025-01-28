package com.safehelmet.safehelmet_mobile.parse

class ParseEnvironmentData(byteArray: ByteArray) : BaseParse(byteArray) {

    var temp: Float = 0.0F
    var hum: Float = 0.0F
    var lux: Float = 0.0F
    var crash: Float = 0.0F
    var gas: BooleanArray = BooleanArray(3) { false } // 3 bit
    var anomaly: BooleanArray = BooleanArray(5) { false } // 5 bit

    override val sensorLengths: Map<String, Int> = mapOf(
        "temp" to 4,
        "hum" to 4,
        "lux" to 4,
        "crash" to 4,
        "gas" to 1,
        "anomaly" to 1
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        temp = extractFloatSensor("temp", offset)
        hum = extractFloatSensor("hum", offset)
        lux = extractFloatSensor("lux", offset)
        crash = extractFloatSensor("crash", offset)
        gas = extractBitsSensor("gas", 0, 2, offset) // Firsts 3 bit
        anomaly = extractBitsSensor("anomaly", 0, 4, offset) // Firsts 5 bit
    }


    fun printValues(): String {
        return buildString {
            append("Temperature: $temp, ")
            append("Humidity: $hum, ")
            append("Lux: $lux, ")
            append("Crash: $crash, ")
            append("Gas values: ")
            for (g in gas) append(if (g) "1" else "0")
            append(", ")

            append("Anomaly values: ")
            for (a in anomaly) append(if (a) "1" else "0")

        }
    }
}
