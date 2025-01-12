package org.iotproject.ble

// CentralDevice.kt
expect class CentralDevice {
    fun connectToPeripheral(peripheral: PeripheralDevice)
    fun disconnectFromPeripheral(peripheral: PeripheralDevice)
}
