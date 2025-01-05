package org.iotproject.safehelmet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothService: BluetoothService

    companion object {
        private const val BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothService = BluetoothService(application)

        // Verifica se i permessi sono concessi
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Richiedi permessi
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE
            )
        }

        setContent {
            // Osserva i dispositivi in tempo reale
            val devices by bluetoothService.peripherals.collectAsState(initial = emptyList())

            BluetoothScreen(
                devices = devices.map { it.name ?: "Unknown Device" },
                onStartScanning = { bluetoothService.startScanning() },
                onStopScanning = { bluetoothService.stopScanning() }
            )
        }

        // Avvia la scansione automaticamente
        lifecycleScope.launch {
            bluetoothService.startScanning()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permessi concessi, avvia la scansione
                lifecycleScope.launch {
                    bluetoothService.startScanning()
                }
            } else {
                // Permessi negati, mostra un messaggio o disabilita funzionalità
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.stopScanning()
    }
}
