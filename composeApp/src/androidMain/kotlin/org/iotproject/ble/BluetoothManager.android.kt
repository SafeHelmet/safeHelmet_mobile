package org.iotproject.ble

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

actual class BleManager(private val context: Context) {

    private val scannedDevices: MutableSet<BleDevice> = mutableSetOf()
    actual var onDevicesFound: ((Set<BleDevice>) -> Unit)? = null

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


    actual fun initializeBluetooth() {

        if (!hasPermissions()) {
            requestPermissions()
            return
        }

        // Controlla se il Bluetooth è abilitato
        if (!bluetoothAdapter.isEnabled) {
            Log.i("BluetoothManager", "Il Bluetooth non è abilitato. Richiesta di abilitazione.")
            requestEnableBluetooth()
            return
        }

        Log.i("BluetoothManager", "Bluetooth inizializzato correttamente.")
    }


    // Permessi richiesti
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )


    // Metodo per richiedere i permessi
    private fun requestPermissions(
    ) {
        if (!hasPermissions()) {
            permissionsLauncher?.launch(bluetoothPermissions)
        } else {
            Log.i("BluetoothManager", "Permessi già concessi")
        }
    }

    // Callback condiviso
    private val scanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("BluetoothManager", "Mancano i permessi")
                    return
                }
                peripherals[device.address] = device
                scannedDevices.add(BleDevice(device.name, device.address))
                onDevicesFound?.invoke(scannedDevices)
                Log.d("BluetoothManager", "Dispositivo trovato: ${device.name} - ${device.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BluetoothManager", "Errore nella scansione: $errorCode")
        }
    }

    actual fun startScanning() {
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Mancano i permessi")
            return
        }
        bluetoothLeScanner?.startScan(scanCallback)
        Log.i("BluetoothManager", "Scansione Bluetooth avviata.")
    }

    actual fun stopScanning() {
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Mancano i permessi")
            return
        }
        bluetoothLeScanner?.stopScan(scanCallback) // Usa lo stesso callback
        Log.i("BluetoothManager", "Scansione Bluetooth interrotta.")
    }

    private fun hasPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Verifica permessi
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        // Log dettagliato
        if (missingPermissions.isNotEmpty()) {
            Log.e("BluetoothManager", "Permessi mancanti: ${missingPermissions.joinToString()}")
            return false
        }

        return true
    }

    actual fun connectToPeripheral(uuid: String) {
        stopScanning()
        val device = peripherals[uuid]
        if (device == null) {
            Log.e("BluetoothManager", "Dispositivo non trovato")
            return
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Permessi mancanti per la connessione.")
            return
        }

        Log.i("BluetoothManager", "Connessione al dispositivo: ${device.name} - ${device.address}")

        // Usa il metodo connectGatt per stabilire la connessione
        val bluetoothGatt = device.connectGatt(context, false, BleCallbackHandler(context))

        // Salva il riferimento al BluetoothGatt nell'oggetto PeripheralDevice
        gatt = bluetoothGatt
    }


    actual fun disconnectFromPeripheral() {
        if (gatt != null) {
            Log.i("BluetoothManager", "Disconnessione dal dispositivo: ${gatt!!.device.address}")

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gatt!!.disconnect()
                gatt!!.close()
                gatt = null
            }
        } else {
            Log.e("BluetoothManager", "Nessuna connessione attiva trovata per il dispositivo.")
        }
    }


    actual fun readCharacteristic(characteristicUUID: String) {
        if (gatt == null) {
            Log.e("BluetoothManager", "Nessuna connessione attiva per leggere la caratteristica.")
            return
        }

        val characteristic = gatt!!.services
            .flatMap { it.characteristics }
            .find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Caratteristica non trovata: $characteristicUUID")
            return
        }

        // Controllo dei permessi
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Permesso BLUETOOTH_CONNECT non concesso.")
            return
        }

        // Avvia la lettura asincrona, passando il callback per ricevere il risultato
        val success = gatt!!.readCharacteristic(characteristic)

        if (success) {
            Log.i("BluetoothManager", "Lettura della caratteristica avviata: $characteristicUUID")
        } else {
            Log.e(
                "BluetoothManager",
                "Errore durante la lettura della caratteristica: $characteristicUUID"
            )
        }
    }


    actual fun writeCharacteristic(characteristicUUID: String, value: String) {
        if (gatt == null) {
            Log.e(
                "BluetoothManager",
                "Nessuna connessione attiva per scrivere sulla caratteristica."
            )
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Permessi mancanti per scrivere sulla caratteristica.")
            return
        }

        // Trova la caratteristica desiderata
        val gattService = gatt!!.services.find { service ->
            service.characteristics.any { it.uuid.toString() == characteristicUUID }
        }

        if (gattService == null) {
            Log.e(
                "BluetoothManager",
                "Servizio con la caratteristica $characteristicUUID non trovato."
            )
            return
        }

        val characteristic =
            gattService.characteristics.find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Caratteristica con UUID $characteristicUUID non trovata.")
            return
        }

        // Scrivi il valore sulla caratteristica
        val response = gatt!!.writeCharacteristic(
            characteristic,
            value.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        if (response == BluetoothStatusCodes.SUCCESS) {
            Log.i("BluetoothManager", "Valore scritto sulla caratteristica: $value")
        } else {
            Log.e("BluetoothManager", "Errore durante la scrittura sulla caratteristica.")
        }
    }

}
