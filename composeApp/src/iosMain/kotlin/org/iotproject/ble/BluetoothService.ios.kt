package org.iotproject.ble

// BluetoothService.kt
actual class BluetoothService {
    actual fun startAdvertising() {
    }

    actual fun stopAdvertising() {
    }

    actual fun onCharacteristicRead(characteristic: String): String {
        TODO("Not yet implemented")
    }

    actual fun onCharacteristicWrite(characteristic: String, value: String) {
    }
}