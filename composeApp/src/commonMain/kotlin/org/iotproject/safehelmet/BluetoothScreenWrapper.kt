package org.iotproject.safehelmet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BluetoothScreenWrapper(bluetoothService: BluetoothService) {
    val devices = bluetoothService.peripherals.collectAsState().value.mapNotNull { it.name }
    val coroutineScope = rememberCoroutineScope() // Scope per avviare coroutine

    BluetoothScreen(
        devices = devices,
        onStartScanning = { bluetoothService.startScanning() },
        onStopScanning = { bluetoothService.stopScanning() },
        onDeviceSelected = { deviceName ->
            val selectedDevice = bluetoothService.peripherals.value.find { it.name == deviceName }
            selectedDevice?.let {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        bluetoothService.connectToDevice(it)
                    }
                }
            }
        },
        onLedCommand = { command ->
            bluetoothService.sendLedCommand(command) // Invia il comando di accensione o spegnimento
        }
    )
}

