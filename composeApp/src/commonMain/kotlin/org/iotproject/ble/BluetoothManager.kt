package org.iotproject.ble

// BluetoothManager.kt
expect class BluetoothManager {
    fun initializeBluetooth()
    fun startScanning()
    fun stopScanning()
}
