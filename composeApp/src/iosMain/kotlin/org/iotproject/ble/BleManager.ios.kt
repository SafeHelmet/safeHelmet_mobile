package org.iotproject.ble

// BluetoothManager.kt
actual class BleManager {
    actual var onDevicesFound: ((Set<BleDevice>) -> Unit)? = {
        TODO("Ios implementation")
    }

    actual fun initializeBluetooth() {
        TODO("Ios implementation")
    }

    actual fun startScanning() {
        TODO("Ios implementation")
    }

    actual fun stopScanning() {
        TODO("Ios implementation")
    }

    actual fun connectToPeripheral(uuid: String) {
        TODO("Ios implementation")
    }

    actual fun disconnectFromPeripheral() {
        TODO("Ios implementation")
    }

    actual fun discoverServices() {
        TODO("Ios implementation")
    }

    actual fun readCharacteristic(characteristicUUID: String, onResult: (String?) -> Unit){
        TODO("Not yet implemented")
    }

    actual fun writeCharacteristic(characteristicUUID: String, value: String) {
        TODO("Ios implementation")
    }

}