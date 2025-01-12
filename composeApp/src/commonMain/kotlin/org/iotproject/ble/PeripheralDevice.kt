package org.iotproject.ble

// PeripheralDevice.kt
expect class PeripheralDevice {
    fun connect()
    fun discoverServices()
    fun readCharacteristic(characteristic: String): String
    fun writeCharacteristic(characteristic: String, value: String)
    fun disconnect()
}