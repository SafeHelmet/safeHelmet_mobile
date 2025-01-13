package org.iotproject.ble

// BluetoothManager.kt
actual class BleManager {
    actual var onDevicesFound: ((Set<BleDevice>) -> Unit)?
        get() = TODO("Not yet implemented")
        set(value) {}

    actual var onServicesDiscovered: ((Set<BleService>) -> Unit)?
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun initializeBluetooth() {
    }

    actual fun startScanning() {
    }

    actual fun stopScanning() {
    }

    actual fun connectToPeripheral(uuid: String) {
    }

    actual fun disconnectFromPeripheral() {
    }

    actual fun discoverServices() {
    }

    actual fun readCharacteristic(characteristicUUID: String): String? {
        TODO("Not yet implemented")
    }

    actual fun writeCharacteristic(characteristicUUID: String, value: String) {
    }

}