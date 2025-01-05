package org.iotproject.safehelmet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothService = BluetoothService(application)

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

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.stopScanning()
    }
}
