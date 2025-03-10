package com.safehelmet.safehelmet_mobile.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.os.Handler
import android.os.Looper
import com.safehelmet.safehelmet_mobile.BackendValues
import com.safehelmet.safehelmet_mobile.api.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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
    var onDisconnected: (() -> Unit)? = null // Callback per notificare la UI
    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter.isEnabled)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

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

    private val lastSeenDevices = mutableMapOf<String, Long>()
    private val deviceTimeout = 2_000L // 2 secondi di timeout
    private val handler = Handler(Looper.getMainLooper())

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

                val currentTime = System.currentTimeMillis()
                lastSeenDevices[device.address] = currentTime // Aggiorna ultimo avvistamento

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

    // Metodo per rimuovere i dispositivi non più visibili
    private val removeTask = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val iterator = scannedDevices.iterator()

            while (iterator.hasNext()) {
                val device = iterator.next()
                val lastSeen = lastSeenDevices[device.address] ?: continue

                if (currentTime - lastSeen > deviceTimeout) {
                    iterator.remove()
                    lastSeenDevices.remove(device.address)
                    Log.d("BluetoothManager", "Device removed: ${device.name} - ${device.address}")
                }
            }

            onDevicesFound?.invoke(scannedDevices) // Aggiorna UI
            handler.postDelayed(this, 1000) // Controlla ogni secondo
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
        handler.postDelayed(removeTask, 1000)
        Log.i("BluetoothManager", "Bluetooth scan started.")
    }

    fun stopScanning(bluetoothDisabled: Boolean = false) {
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
        handler.removeCallbacks(removeTask)
        if (bluetoothDisabled) {
            scannedDevices.clear()
            onDevicesFound?.invoke(scannedDevices) // Aggiorna UI
        }
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

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.i("BluetoothManager", "Bluetooth off")
                        if (gatt != null) {  // Controlla se esiste una connessione GATT attiva
                            Log.i("BluetoothManager", "Device is connected, disconnecting...")
                            disconnectFromPeripheral()
                            onDisconnected?.invoke()
                        } else {
                            Log.i("BluetoothManager", "No active connection found, skipping disconnect.")
                        }
                        requestEnableBluetooth()
                    }

                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.i("BluetoothManager", "Bluetooth turning off")
                    }

                    BluetoothAdapter.STATE_ON -> {
                        Log.i("BluetoothManager", "Bluetooth on")
                    }
                }
            }
        }
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
        // val bluetoothGatt = device.connectGatt(context, false, BleCallbackHandler(context))
        // gatt = bluetoothGatt

        gatt = device.connectGatt(context, false, BleCallbackHandler(context) {
            onDisconnected?.invoke() // Chiamata alla callback quando si disconnette
        })
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

        // Esegui la richiesta HTTP in modo asincrono
        // HttpClient.getRequestSync("/api/v1/workers/attendance/${BackendValues.attendanceID}")
        // Fire-and-forget API call (runs in the background)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HttpClient.getRequestSync("/api/v1/workers/attendance/${BackendValues.attendanceID}")
                Log.i("HttpClient", "API request sent successfully.")
            } catch (e: Exception) {
                Log.e("HttpClient", "Network request failed", e)
            }
        }

    }

    // Funzione per registrare il BluetoothReceiver
    fun registerReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
    }

    // Funzione per deregistrare il BluetoothReceiver
    fun unregisterReceiver() {
        context.unregisterReceiver(bluetoothReceiver)
    }


    fun readCharacteristic(characteristicUUID: UUID) {
        if (gatt == null) {
            Log.e("BluetoothManager", "No active connection to read the characteristic.")
            return
        }

        val characteristic = gatt!!.services
            .flatMap { it.characteristics }
            .find { it.uuid == characteristicUUID }

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

    fun adviseForAnomaly() {
        writeCharacteristic(0x0046, "1")
    }

    fun writeCharacteristic(characteristicUUID: Int, value: String) {
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

        val gattService =
            gatt!!.services[2].characteristics.find { characteristic ->
                characteristic.descriptors.any { descriptor ->
                    descriptor.uuid == uuidFrom16Bit(characteristicUUID)
                }
            }

        if (gattService == null) {
            Log.e(
                "BluetoothManager",
                "Service with characteristic $characteristicUUID not found."
            )
            return
        }

        // Write the value on the characteristic
        val response = gatt!!.writeCharacteristic(
            gattService,
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