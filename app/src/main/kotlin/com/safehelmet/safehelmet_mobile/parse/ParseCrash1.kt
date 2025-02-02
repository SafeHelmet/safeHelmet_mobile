package com.safehelmet.safehelmet_mobile.parse

class ParseCrash1(byteArray: ByteArray) : BaseParse(byteArray) {

    var avg_x: Float = 0.0F
    var avg_y: Float = 0.0F
    var avg_z: Float = 0.0F
    var avg_g: Float = 0.0F
    var std_g: Float= 0.0F

    override val sensorLengths: Map<String, Int> = mapOf(
        "avg_x" to 4,
        "avg_y" to 4,
        "avg_z" to 4,
        "avg_g" to 4,
        "std_g" to 4
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        avg_x = extractFloatSensor("avg_x", offset)
        avg_y = extractFloatSensor("avg_y", offset)
        avg_z = extractFloatSensor("avg_z", offset)
        avg_g = extractFloatSensor("avg_g", offset)
        std_g = extractFloatSensor("std_g", offset)
    }

    override fun printValues(): String {
        return buildString {
            append("avg_x: $avg_x, ")
            append("avg_y: $avg_y, ")
            append("avg_z: $avg_z, ")
            append("avg_g: $avg_g, ")
            append("std_g: $std_g")
        }
    }
}