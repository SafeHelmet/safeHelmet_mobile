package org.iotproject.safehelmet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothService: BluetoothService

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
                Toast.makeText(this, "Bluetooth permissions are required for this app to work", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothService = BluetoothService(application)

        // Verifica se i permessi sono concessi
        if (arePermissionsGranted()) {
            startBluetoothScanning()
        } else {
            // Se i permessi non sono concessi, chiedi all'utente
            requestPermissionsLauncher.launch(bluetoothPermissions)
        }

        setContent {
            val devices by bluetoothService.peripherals.collectAsState()

            BluetoothScreen(
                devices = devices.map { it.name ?: "Unknown Device" },
                onStartScanning = { bluetoothService.startScanning() },
                onStopScanning = { bluetoothService.stopScanning() }
            )
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBluetoothScanning() {
        lifecycleScope.launch {
            bluetoothService.startScanning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.stopScanning()
    }
}
