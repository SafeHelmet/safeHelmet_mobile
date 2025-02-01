package com.safehelmet.safehelmet_mobile.parse

class ParseCrash2(byteArray: ByteArray) : BaseParse(byteArray) {

    var std_x: Float = 0.0F
    var std_y: Float = 0.0F
    var std_z: Float = 0.0F
    var max_g: Float = 0.0F
    var incorrect_posture_percentuage: Float= 0.0F

    override val sensorLengths: Map<String, Int> = mapOf(
        "std_x" to 4,
        "std_y" to 4,
        "std_z" to 4,
        "max_g" to 4,
        "incorrect_posture_percentuage" to 4
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        std_x = extractFloatSensor("std_x", offset)
        std_y = extractFloatSensor("std_y", offset)
        std_z = extractFloatSensor("std_z", offset)
        max_g = extractFloatSensor("max_g", offset)
        incorrect_posture_percentuage = extractFloatSensor("incorrect_posture_percentuage", offset)
    }

    override fun printValues(): String {
        return buildString {
            append("std_x: $std_x, ")
            append("std_y: $std_y, ")
            append("std_z: $std_z, ")
            append("max_g: $max_g, ")
            append("incorrect_posture_percentuage: $incorrect_posture_percentuage")
        }
    }
}