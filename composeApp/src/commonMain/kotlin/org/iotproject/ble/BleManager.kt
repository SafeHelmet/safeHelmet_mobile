package org.iotproject.ble

// BluetoothManager.kt
expect class BleManager {
    fun initializeBluetooth()
    fun startScanning()
    fun stopScanning()
}
