package org.iotproject.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes

actual class BleManager(private val context: Context) {

    // Lista dei dispositivi BLE trovati
    private val scannedDevices = mutableListOf<BleDevice>()
    // Callback per notificare i dispositivi trovati
    actual var onDevicesFound: ((List<BleDevice>) -> Unit)? = null

    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var enableBluetoothLauncher: ActivityResultLauncher<Intent>? = null
    private var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var activity: Activity? = null

    private var peripherals = mutableMapOf<String, BluetoothDevice>()
    private var GYAAAT: BluetoothGatt? = null


    // Inizializza il BluetoothManager con l'Activity e i launchers
    fun initializeBluetoothManager(
        activity: Activity,
        enableBluetoothLauncher: ActivityResultLauncher<Intent>,
        permissionsLauncher: ActivityResultLauncher<Array<String>>
    ) {
        this.activity = activity
        this.enableBluetoothLauncher = enableBluetoothLauncher
        this.permissionsLauncher = permissionsLauncher
    }


    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher?.launch(enableBtIntent)
            ?: Log.e("BluetoothManager", "ActivityResultLauncher non inizializzato.")
    }



    actual fun initializeBluetooth() {
        // Verifica che il dispositivo supporti il Bluetooth

        // Controlla se il Bluetooth è abilitato
        if (!bluetoothAdapter.isEnabled) {
            Log.i("BluetoothManager", "Il Bluetooth non è abilitato. Richiesta di abilitazione.")
            requestEnableBluetooth()
            return
        }

        // Verifica dei permessi richiesti
        if (!hasPermissions()) {
            Log.e("BluetoothManager", "Permessi Bluetooth mancanti. Richiesta in corso.")
            permissionsLauncher?.launch(bluetoothPermissions)
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
    fun requestPermissions(
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

    fun hasPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Verifica permessi
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        // Log dettagliato
        if (missingPermissions.isNotEmpty()) {
            Log.e("BluetoothManager", "Permessi mancanti: ${missingPermissions.joinToString()}")
            return false
        }

        return true
    }

    actual fun connectToPeripheral(uuid: String) {
        val device = peripherals[uuid]
        if (device == null){
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
        val bluetoothGatt = device.connectGatt(context, false, object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                when (newState) {
                    android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                        Log.i("BluetoothManager", "Connesso a ${gatt.device.address}")
                        // Scansiona i servizi disponibili
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            gatt.discoverServices()
                        }
                    }
                    android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i("BluetoothManager", "Disconnesso da ${gatt.device.address}")
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("BluetoothManager", "Servizi scoperti per ${gatt.device.address}")
                } else {
                    Log.e("BluetoothManager", "Errore nella scoperta dei servizi: $status")
                }
            }
        })

        // Salva il riferimento al BluetoothGatt nell'oggetto PeripheralDevice
        GYAAAT = bluetoothGatt
    }


    actual fun disconnectFromPeripheral() {
        if (GYAAAT != null) {
            Log.i("BluetoothManager", "Disconnessione dal dispositivo: ${GYAAAT!!.device.address}")

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                GYAAAT!!.disconnect()
                GYAAAT!!.close()
                GYAAAT = null
            }
        } else {
            Log.e("BluetoothManager", "Nessuna connessione attiva trovata per il dispositivo.")
        }
    }


    actual fun discoverServices() {
        if (GYAAAT == null) {
            Log.e("BluetoothManager", "Nessuna connessione attiva per la scoperta dei servizi.")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Permessi mancanti per la scoperta dei servizi.")
            return
        }

        val success = GYAAAT!!.discoverServices()
        if (success) {
            Log.i("BluetoothManager", "Richiesta di scoperta dei servizi inviata con successo.")
        } else {
            Log.e("BluetoothManager", "Errore durante l'invio della richiesta di scoperta dei servizi.")
        }
    }


    actual fun readCharacteristic(characteristicUUID: String): String? {
        if (GYAAAT == null) {
            Log.e("BluetoothManager", "Nessuna connessione attiva per leggere la caratteristica.")
            return null
        }

        val characteristic = GYAAAT!!.services
            .flatMap { it.characteristics }
            .find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Caratteristica non trovata: $characteristicUUID")
            return null
        }

        val success = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            null
        }else{
            GYAAAT!!.readCharacteristic(characteristic)
        }

        return if (success == true) {
            Log.i("BluetoothManager", "Lettura della caratteristica avviata: $characteristicUUID")
            null // Puoi ritornare il valore letto in un callback separato.
        } else {
            Log.e("BluetoothManager", "Errore durante la lettura della caratteristica: $characteristicUUID")
            null
        }
    }


    actual fun writeCharacteristic(characteristicUUID: String, value: String) {
        if (GYAAAT == null) {
            Log.e("BluetoothManager", "Nessuna connessione attiva per scrivere sulla caratteristica.")
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
        val gattService = GYAAAT!!.services.find { service ->
            service.characteristics.any { it.uuid.toString() == characteristicUUID }
        }

        if (gattService == null) {
            Log.e("BluetoothManager", "Servizio con la caratteristica $characteristicUUID non trovato.")
            return
        }

        val characteristic = gattService.characteristics.find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Caratteristica con UUID $characteristicUUID non trovata.")
            return
        }

        // Scrivi il valore sulla caratteristica
        val response = GYAAAT!!.writeCharacteristic(characteristic, value.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        if (response == BluetoothStatusCodes.SUCCESS) {
            Log.i("BluetoothManager", "Valore scritto sulla caratteristica: $value")
        } else {
            Log.e("BluetoothManager", "Errore durante la scrittura sulla caratteristica.")
        }
    }

}
