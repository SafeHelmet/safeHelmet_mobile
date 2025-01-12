package org.iotproject.ble

// BluetoothCallback.kt
actual interface BluetoothCallback {
    actual fun onConnected(device: PeripheralDevice)
    actual fun onDisconnected(device: PeripheralDevice)
    actual fun onCharacteristicRead(characteristic: String, value: String)
    actual fun onCharacteristicWrite(characteristic: String, value: String)
}