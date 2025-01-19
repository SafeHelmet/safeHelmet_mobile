package org.iotproject.ble

import platform.CoreBluetooth.*
import co.touchlab.kermit.Logger


// BluetoothManager.kt
actual class BleManager {
    actual var onDevicesFound: ((Set<BleDevice>) -> Unit)? = null
    private var centralManager: CBCentralManager? = null
    private var delegate: BleCallbackHandler? = null
    private var logger = Logger.withTag("BluetoothManager")
    private var scannedDevices = mutableSetOf<BleDevice>()

    actual fun initializeBluetooth() {
        delegate = BleCallbackHandler()
        centralManager = CBCentralManager()
        centralManager?.delegate = delegate
    }

    actual fun startScanning() {
        logger.i("Starting scanning")
        centralManager?.scanForPeripheralsWithServices(null, null)
        delegate?.onDeviceFound = { peripheral ->
            val deviceName = peripheral.name ?: "Unknown Device"
            val deviceAddress = peripheral.identifier.UUIDString
            scannedDevices.add(BleDevice(deviceName, deviceAddress))
            onDevicesFound?.invoke(scannedDevices)
        }
    }

    actual fun stopScanning() {
        centralManager?.stopScan()
    }

    actual fun connectToPeripheral(uuid: String) {
        // TODO: Ios implementation
    }

    actual fun disconnectFromPeripheral() {
        // TODO: Ios implementation
    }

    actual fun readCharacteristic(characteristicUUID: String) {
        // TODO: Not yet implemented
    }

    actual fun writeCharacteristic(characteristicUUID: String, value: String) {
        // TODO: Ios implementation
    }

}