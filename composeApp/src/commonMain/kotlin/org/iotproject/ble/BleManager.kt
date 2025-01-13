package org.iotproject.ble

// BluetoothManager.kt
expect class BleManager {
    var onDevicesFound: ((Set<BleDevice>) -> Unit)?
    var onServicesDiscovered: ((Set<BleService>) -> Unit)?

    fun initializeBluetooth()
    fun startScanning()
    fun stopScanning()

    fun connectToPeripheral(uuid: String)
    fun disconnectFromPeripheral()

    fun discoverServices()
    fun readCharacteristic(characteristicUUID: String): String?
    fun writeCharacteristic(characteristicUUID: String, value: String)
}
