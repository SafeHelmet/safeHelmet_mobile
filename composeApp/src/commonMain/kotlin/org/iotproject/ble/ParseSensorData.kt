package org.iotproject.ble

// Definizione delle lunghezze per ogni sensore
data class Length(
    val temp: Int = 4,
    val hum: Int = 4,
    val lux: Int = 4,
    val crash: Int = 4,
    val gas: Int = 1, // 1 byte, ma dobbiamo usare solo 3 bit
    val anomaly: Int = 1 // 1 byte, ma dobbiamo usare solo 5 bit
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

        // Estrai i valori per ogni sensore, incrementando l'offset ogni volta
        temp = byteArray.copyOfRange(offset, offset + lengths.temp).toFloat()
        offset += lengths.temp

        hum = byteArray.copyOfRange(offset, offset + lengths.hum).toFloat()
        offset += lengths.hum

        lux = byteArray.copyOfRange(offset, offset + lengths.lux).toFloat()
        offset += lengths.lux

        crash = byteArray.copyOfRange(offset, offset + lengths.crash).toFloat()
        offset += lengths.crash

        // Gestione del gas (3 bit)
        gas = extractBits(byteArray[offset], 3)

        offset += lengths.gas

        // Gestione dell'anomalia (5 bit)
        anomaly = extractBits(byteArray[offset], 5)
    }

    // Funzione generica per estrarre i bit da un byte
    private fun extractBits(byte: Byte, bitCount: Int): BooleanArray {
        return BooleanArray(bitCount) { index ->
            (byte.toInt() shr (bitCount - 1 - index) and 0x01) == 0x01
        }
    }

    // Funzione per stampare tutti i valori
    fun printValues(): String {
        return buildString {
            append("Temperature: $temp, ")
            append("Humidity: $hum, ")
            append("Lux: $lux, ")
            append("Crash: $crash, ")
            append("Gas values: ${gas.joinToString()}, ")
            append("Anomaly values: ${anomaly.joinToString()}")
        }
    }

    // Funzione per convertire ByteArray in Float
    private fun ByteArray.toFloat(): Float {
        require(this.size == 4) { "Il ByteArray deve contenere esattamente 4 byte per una conversione a Float" }
        return (this[0].toInt() and 0xFF shl 24 or
                (this[1].toInt() and 0xFF shl 16) or
                (this[2].toInt() and 0xFF shl 8) or
                (this[3].toInt() and 0xFF)).toFloat()
    }

    companion object {
        val lengths = Length()
    }
}
