package org.iotproject.ble

// BluetoothManager.kt
expect class BleManager {
    var onDevicesFound: ((List<BleDevice>) -> Unit)?

    fun initializeBluetooth()
    fun startScanning()
    fun stopScanning()

    fun connectToPeripheral(uuid: String)
    fun disconnectFromPeripheral()

    fun discoverServices()
    fun readCharacteristic(characteristicUUID: String): String?
    fun writeCharacteristic(characteristicUUID: String, value: String)
}
