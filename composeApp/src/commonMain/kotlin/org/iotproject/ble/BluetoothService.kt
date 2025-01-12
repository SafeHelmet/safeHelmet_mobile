package org.iotproject.ble

// BluetoothService.kt
expect class BluetoothService {
    fun startAdvertising()
    fun stopAdvertising()
    fun onCharacteristicRead(characteristic: String): String
    fun onCharacteristicWrite(characteristic: String, value: String)
}
