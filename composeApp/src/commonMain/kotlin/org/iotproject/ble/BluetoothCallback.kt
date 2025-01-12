package org.iotproject.ble

// BluetoothCallback.kt
expect interface BluetoothCallback {
    fun onConnected(device: PeripheralDevice)
    fun onDisconnected(device: PeripheralDevice)
    fun onCharacteristicRead(characteristic: String, value: String)
    fun onCharacteristicWrite(characteristic: String, value: String)
}
