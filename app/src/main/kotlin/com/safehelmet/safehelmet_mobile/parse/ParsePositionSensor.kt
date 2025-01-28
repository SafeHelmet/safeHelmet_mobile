package com.safehelmet.safehelmet_mobile.parse

class ParsePositionSensor(byteArray: ByteArray) : BaseParse(byteArray) {

    // Variabili per x, y, z e G (valori float)
    var x: Float = 0.0F
    var y: Float = 0.0F
    var z: Float = 0.0F
    var G: Float = 0.0F

    override val sensorLengths: Map<String, Int> = mapOf(
        "x" to 4,
        "y" to 4,
        "z" to 4,
        "G" to 4
    )

    init {
        parseSensors()
    }

    private fun parseSensors() {
        val offset = intArrayOf(0)

        x = extractFloatSensor("x", offset)
        y = extractFloatSensor("y", offset)
        z = extractFloatSensor("z", offset)
        G = extractFloatSensor("G", offset)
    }

    fun printValues(): String {
        return buildString {
            append("X: $x, ")
            append("Y: $y, ")
            append("Z: $z, ")
            append("G: $G")
        }
    }
}