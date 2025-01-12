package org.iotproject.ble

// CentralDevice.kt
actual class CentralDevice {
    actual fun startScanning() {
    }

    actual fun stopScanning() {
    }

    actual fun connectToPeripheral(peripheral: PeripheralDevice) {
    }

    actual fun disconnectFromPeripheral(peripheral: PeripheralDevice) {
    }
}