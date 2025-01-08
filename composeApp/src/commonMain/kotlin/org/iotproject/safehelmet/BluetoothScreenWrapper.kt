package org.iotproject.safehelmet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

@Composable
fun BluetoothScreenWrapper(bluetoothService: BLEManager) {
    val devices = bluetoothService.peripherals.collectAsState().value.mapNotNull { it.name }

    BluetoothScreen(
        devices = devices,
        onStartScanning = { bluetoothService.startScanning() },
        onStopScanning = { bluetoothService.stopScanning() },
        onDeviceSelected = { deviceName ->
            val selectedDevice = bluetoothService.peripherals.value.find { it.name == deviceName }
            selectedDevice?.let { bluetoothService.connectToDevice(it) }
        },
        onLedCommand = { command ->
            bluetoothService.sendLedCommand(command)  // Invia il comando di accensione o spegnimento
        }
    )
}
