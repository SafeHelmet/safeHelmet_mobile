package com.safehelmet.safehelmet_mobile.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class BleDevice(
    val name: String?,
    val address: String
)

class BleManager(private val context: Context) {

    private val scannedDevices: MutableSet<BleDevice> = mutableSetOf()
    var onDevicesFound: ((Set<BleDevice>) -> Unit)? = null

    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var enableBluetoothLauncher: ActivityResultLauncher<Intent>? = null
    private var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var activity: Activity? = null

    private var peripherals = mutableMapOf<String, BluetoothDevice>()
    private var gatt: BluetoothGatt? = null


    fun initializeBluetoothManager(
        activity: Activity,
        enableBluetoothLauncher: ActivityResultLauncher<Intent>,
        permissionsLauncher: ActivityResultLauncher<Array<String>>
    ) {
        this.activity = activity
        this.enableBluetoothLauncher = enableBluetoothLauncher
        this.permissionsLauncher = permissionsLauncher
    }


    fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher?.launch(enableBtIntent)
            ?: Log.e("BluetoothManager", "ActivityResultLauncher not initialized.")
    }


    fun initializeBluetooth() {

        if (!hasPermissions()) {
            requestPermissions()
            return
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            Log.i("BluetoothManager", "Bluetooth is not enabled. Requesting to enable it.")
            requestEnableBluetooth()
            return
        }

        Log.i("BluetoothManager", "Bluetooth initialized correctly.")
    }


    // Required permissions
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )


    // Method to request permissions
    private fun requestPermissions(
    ) {
        if (!hasPermissions()) {
            permissionsLauncher?.launch(bluetoothPermissions)
        } else {
            Log.i("BluetoothManager", "Permissions already granted")
        }
    }

    // Shared Callback
    private val scanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("BluetoothManager", "Permissions are missing")
                    return
                }
                peripherals[device.address] = device
                scannedDevices.add(BleDevice(device.name, device.address))
                onDevicesFound?.invoke(scannedDevices)
                Log.d("BluetoothManager", "Found device: ${device.name} - ${device.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BluetoothManager", "Error on scan: $errorCode")
        }
    }

    fun startScanning() {
        scannedDevices.clear()  // Pulisce la lista prima di ogni scansione
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Missing permissions")
            return
        }
        bluetoothLeScanner?.startScan(scanCallback)
        Log.i("BluetoothManager", "Bluetooth scan started.")
    }

    fun stopScanning() {
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Missing permissions")
            return
        }
        bluetoothLeScanner?.stopScan(scanCallback) // Use the same callback
        Log.i("BluetoothManager", "Bluetooth scan stopped.")
    }

    private fun hasPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {  // Detailed log
            Log.e("BluetoothManager", "Missing permissions: ${missingPermissions.joinToString()}")
            return false
        }

        return true
    }

    fun connectToPeripheral(uuid: String) {
        stopScanning()
        val device = peripherals[uuid]
        if (device == null) {
            Log.e("BluetoothManager", "Device not found")
            return
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Can't connect due to missing permission.")
            return
        }

        Log.i("BluetoothManager", "Connected to device: ${device.name} - ${device.address}")

        // Use the connectGatt method to establish the connection
        val bluetoothGatt = device.connectGatt(context, false, BleCallbackHandler(context))

        gatt = bluetoothGatt
    }


    fun disconnectFromPeripheral() {
        if (gatt != null) {
            Log.i("BluetoothManager", "Disconnecting from device: ${gatt!!.device.address}")

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt!!.disconnect()
                Log.i("BluetoothManager", "Disconnect called.")
                gatt!!.close()
                Log.i("BluetoothManager", "GATT connection closed.")
                gatt = null
            } else {
                Log.e("BluetoothManager", "BLUETOOTH_CONNECT permission not granted.")
            }
        } else {
            Log.e("BluetoothManager", "No active connection found for the device.")
        }
    }


    fun readCharacteristic(characteristicUUID: String) {
        if (gatt == null) {
            Log.e("BluetoothManager", "No active connection to read the characteristic.")
            return
        }

        val characteristic = gatt!!.services
            .flatMap { it.characteristics }
            .find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Characteristic not found: $characteristicUUID")
            return
        }

        // Permission check
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Permission BLUETOOTH_CONNECT non conceded.")
            return
        }

        // Start the asynchronous read, passing the callback to receive the result
        val success = gatt!!.readCharacteristic(characteristic)

        if (success) {
            Log.i("BluetoothManager", "Characteristic read started: $characteristicUUID")
        } else {
            Log.e(
                "BluetoothManager",
                "Error during characteristic read: $characteristicUUID"
            )
        }
    }


    fun writeCharacteristic(characteristicUUID: String, value: String) {
        if (gatt == null) {
            Log.e(
                "BluetoothManager",
                "No active connection to write on the characteristic."
            )
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Missing permissions to write the characteristic.")
            return
        }

        // Find the desired characteristic
        val gattService = gatt!!.services.find { service ->
            service.characteristics.any { it.uuid.toString() == characteristicUUID }
        }

        if (gattService == null) {
            Log.e(
                "BluetoothManager",
                "Service with characteristic $characteristicUUID not found."
            )
            return
        }

        val characteristic =
            gattService.characteristics.find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Characteristic with UUID $characteristicUUID not found.")
            return
        }

        // Write the value on the characteristic
        val response = gatt!!.writeCharacteristic(
            characteristic,
            value.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        if (response == BluetoothStatusCodes.SUCCESS) {
            Log.i("BluetoothManager", "Value written on characteristic: $value")
        } else {
            Log.e("BluetoothManager", "Error writing to characteristic.")
        }
    }

}
