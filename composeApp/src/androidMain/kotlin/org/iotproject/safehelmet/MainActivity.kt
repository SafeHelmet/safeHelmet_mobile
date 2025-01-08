package org.iotproject.safehelmet

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothService: BLEManager

    // Permessi richiesti
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // ActivityResultLauncher per i permessi
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                // Permessi concessi, avvia la scansione
                startBluetoothScanning()
            } else {
                // Mostra un messaggio di errore se i permessi sono negati
                Toast.makeText(
                    this,
                    "I permessi Bluetooth sono necessari per utilizzare l'app.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Variabile per memorizzare il callback della scansione
    private var scanCallback: ScanCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothService = BLEManager(application)

        // Verifica se i permessi sono concessi
        if (arePermissionsGranted()) {
            startBluetoothScanning()
        } else {
            // Se i permessi non sono concessi, chiedi all'utente
            requestPermissionsLauncher.launch(bluetoothPermissions)
        }

        setContent {
            BluetoothScreenWrapper(bluetoothService)
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBluetoothScanning() {
        lifecycleScope.launch {
            // Inizializza e memorizza il callback della scansione
            scanCallback = object : ScanCallback() {
                @SuppressLint("MissingPermission")
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    val deviceName = result.device.name ?: "Dispositivo sconosciuto"
                    val deviceAddress = result.device.address
                    Toast.makeText(
                        this@MainActivity,
                        "Trovato dispositivo: $deviceName ($deviceAddress)",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Esempio: connetti al primo dispositivo trovato
                    bluetoothService.connect(deviceAddress)
                }

                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                    Toast.makeText(
                        this@MainActivity,
                        "Errore nella scansione BLE: $errorCode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Avvia la scansione
            bluetoothService.startScanning(scanCallback!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Interrompi la scansione quando l'attività è distrutta
        scanCallback?.let { bluetoothService.stopScanning(it) }
    }
}
