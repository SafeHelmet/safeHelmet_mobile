package com.safehelmet.safehelmet_mobile.parse

abstract class BaseParse(private val byteArray: ByteArray) {

    abstract val sensorLengths: Map<String, Int>

    fun extractFloatSensor(sensor: String, offset: IntArray): Float {
        val length = sensorLengths[sensor] ?: error("Invalid sensor: $sensor")
        val value = byteArray.copyOfRange(offset[0], offset[0] + length).toFloat()
        offset[0] += length
        return value
    }

    fun extractBitsSensor(sensor: String, fromBit: Int, toBit: Int, offset: IntArray): BooleanArray {
        val length = sensorLengths[sensor] ?: error("Invalid sensor: $sensor")
        val value = extractBits(byteArray[offset[0]], fromBit, toBit)
        offset[0] += length
        return value
    }

    private fun extractBits(byte: Byte, fromBit: Int, toBit: Int): BooleanArray {
        require(fromBit in 0..7 && toBit in 0..7 && fromBit <= toBit) {
            "fromBit e toBit devono essere compresi tra 0 e 7, e fromBit <= toBit"
        }

        val result = BooleanArray(toBit - fromBit + 1)
        for (i in fromBit..toBit) {
            val bit = (byte.toInt() shr i) and 1
            result[i - fromBit] = bit == 1
        }

        return result
    }

    private fun ByteArray.toFloat(): Float {
        if (this.size != 4) return 0.0f
        return Float.fromBits(
            (this[3].toInt() and 0xff shl 24) or
                    (this[2].toInt() and 0xff shl 16) or
                    (this[1].toInt() and 0xff shl 8) or
                    (this[0].toInt() and 0xff)
        )
    }

    abstract fun printValues() : String
}
