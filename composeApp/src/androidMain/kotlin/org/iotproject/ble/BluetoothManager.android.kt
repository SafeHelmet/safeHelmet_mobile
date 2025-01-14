package org.iotproject.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import java.nio.charset.StandardCharsets
import java.util.UUID.fromString

actual class BleManager(private val context: Context) {
    private val scannedDevices : MutableSet<BleDevice> = mutableSetOf()

    actual var onDevicesFound: ((Set<BleDevice>) -> Unit)? = null

    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var enableBluetoothLauncher: ActivityResultLauncher<Intent>? = null
    private var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var activity: Activity? = null

    private var peripherals = mutableMapOf<String, BluetoothDevice>()
    private var GYAAAT: BluetoothGatt? = null
    private var notifyBro: BluetoothGattCharacteristic? = null


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
        stopScanning()
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
        val bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
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
                    val notifyCharacteristic = gatt.getService(fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479")).getCharacteristic(fromString("f47ac10b-58cc-4372-a567-0e02b2c3d481"))
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    if (gatt.setCharacteristicNotification(notifyCharacteristic, true)){
                        Log.i("BluetoothManager", "Notifica attivata per la caratteristica")
                    }else{
                        Log.e("BluetoothManager", "Errore nella configurazione della notifica")
                    }

                    val descriptor0 = notifyCharacteristic.descriptors[0]
                    if (gatt.writeDescriptor(descriptor0, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS){
                        Log.i("BluetoothManager", "Descrizione attivata")
                    }else{
                        Log.e("BluetoothManager", "Errore nella configurazione della descrizione")
                    }

                } else {
                    Log.e("BluetoothManager", "Errore nella scoperta dei servizi: $status")
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val newString = String(value, StandardCharsets.UTF_8)
                    Log.i("BluetoothManager", "Caratteristica letta: ${newString}")
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                super.onCharacteristicChanged(gatt, characteristic, value)
                Log.i("BluetoothManager", "Caratteristica cambiata: ${String(value)}")
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

    actual fun readCharacteristic(characteristicUUID: String, onResult: (String?) -> Unit) {
        if (GYAAAT == null) {
            Log.e("BluetoothManager", "Nessuna connessione attiva per leggere la caratteristica.")
            onResult(null)
            return
        }

        val characteristic = GYAAAT!!.services
            .flatMap { it.characteristics }
            .find { it.uuid.toString() == characteristicUUID }

        if (characteristic == null) {
            Log.e("BluetoothManager", "Caratteristica non trovata: $characteristicUUID")
            onResult(null)
            return
        }

        // Controllo dei permessi
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BluetoothManager", "Permesso BLUETOOTH_CONNECT non concesso.")
            onResult(null)
            return
        }

        // Avvia la lettura asincrona, passando il callback per ricevere il risultato
        val success = GYAAAT!!.readCharacteristic(characteristic)

        if (success) {
            Log.i("BluetoothManager", "Lettura della caratteristica avviata: $characteristicUUID")
        } else {
            Log.e("BluetoothManager", "Errore durante la lettura della caratteristica: $characteristicUUID")
            onResult(null)
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
