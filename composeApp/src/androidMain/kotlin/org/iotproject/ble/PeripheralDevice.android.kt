package org.iotproject.ble

// PeripheralDevice.kt
actual class PeripheralDevice {
    actual fun connect() {
    }

    actual fun discoverServices() {
    }

    actual fun readCharacteristic(characteristic: String): String {
        TODO("Not yet implemented")
    }

    actual fun writeCharacteristic(characteristic: String, value: String) {
    }

    actual fun disconnect() {
    }
}