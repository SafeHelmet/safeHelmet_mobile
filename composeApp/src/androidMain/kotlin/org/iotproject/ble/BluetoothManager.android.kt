package org.iotproject.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

actual class BleManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()


    private var enableBluetoothLauncher: ActivityResultLauncher<Intent>? = null
    private var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var activity: Activity? = null


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
        if (bluetoothAdapter == null) {
            Log.e("BluetoothManager", "Questo dispositivo non supporta il Bluetooth.")
            return
        }

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
        activity: Activity,
        launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    ) {
        if (!hasPermissions()) {
            launcher.launch(bluetoothPermissions)
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

                Log.d("BluetoothManager", "Dispositivo trovato: ${device.name} - ${device.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BluetoothManager", "Errore nella scansione: $errorCode")
        }
    }

    actual fun startScanning() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
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
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
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
}
