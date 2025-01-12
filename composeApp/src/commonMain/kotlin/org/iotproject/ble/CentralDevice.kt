package org.iotproject.ble

// CentralDevice.kt
expect class CentralDevice {
    fun startScanning()
    fun stopScanning()
    fun connectToPeripheral(peripheral: PeripheralDevice)
    fun disconnectFromPeripheral(peripheral: PeripheralDevice)
}
