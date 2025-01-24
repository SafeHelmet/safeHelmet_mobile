package com.safehelmet.safehelmet_mobile.ble

// Byte Length for each sensor
data class Length(
    val temp: Int = 4,
    val hum: Int = 4,
    val lux: Int = 4,
    val crash: Int = 4,
    val gas: Int = 1,
    val anomaly: Int = 1
)

class ParseSensorData(byteArray: ByteArray) {

    var originalByteArray: ByteArray = byteArray
    var temp: Float = 0.0F
    var hum: Float = 0.0F
    var lux: Float = 0.0F
    var crash: Float = 0.0F
    var gas: BooleanArray = BooleanArray(3) { false } // 3 bit
    var anomaly: BooleanArray = BooleanArray(5) { false } // 5 bit

    init {
        var offset = 0

        temp = byteArray.copyOfRange(offset, offset + lengths.temp).toFloat()
        offset += lengths.temp

        hum = byteArray.copyOfRange(offset, offset + lengths.hum).toFloat()
        offset += lengths.hum

        lux = byteArray.copyOfRange(offset, offset + lengths.lux).toFloat()
        offset += lengths.lux

        crash = byteArray.copyOfRange(offset, offset + lengths.crash).toFloat()
        offset += lengths.crash

        gas = extractBits(byteArray[offset], 0, 2)
        offset += lengths.gas

        anomaly = extractBits(byteArray[offset], 0, 4)
        offset += lengths.anomaly
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

    private fun ByteArray.toFloat(): Float {
        if (this.size != 4) return 0.0f
        return Float.fromBits(
            (this[3].toInt() and 0xff shl 24) or
                    (this[2].toInt() and 0xff shl 16) or
                    (this[1].toInt() and 0xff shl 8) or
                    (this[0].toInt() and 0xff)
        )
    }

    companion object {
        val lengths = Length()
    }
}
