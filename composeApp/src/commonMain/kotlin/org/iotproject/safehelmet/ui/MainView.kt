package org.iotproject.safehelmet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.iotproject.ble.BleManager
import org.iotproject.ble.BleDevice

enum class ConnectionState {
    NON_CONNECTED,
    CONNECTED
}

@Composable
fun BluetoothScreenWrapper(bleManager: BleManager) {

    var connectionState by remember { mutableStateOf(ConnectionState.NON_CONNECTED) }
    val devices = remember { mutableStateListOf<BleDevice>() }

    // Imposta la callback di BleManager
    bleManager.onDevicesFound = { foundDevices: Set<BleDevice> ->
        devices.clear()
        devices.addAll(foundDevices)
    }

    when (connectionState) {
        ConnectionState.NON_CONNECTED -> NonConnectedScreen(
            devices = devices,
            bleManager = bleManager,
            onConnectButtonClick = { connectionState = ConnectionState.CONNECTED }
        )
        ConnectionState.CONNECTED -> ConnectedScreen(
            bleManager = bleManager,
            onDisconnectButtonClick = { connectionState = ConnectionState.NON_CONNECTED }
        )
    }
}

@Composable
fun NonConnectedScreen(
    devices: List<BleDevice>,
    bleManager: BleManager,
    onConnectButtonClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Buttons for starting and stopping scanning
        Button(
            onClick = { bleManager.startScanning() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Start Scanning", fontSize = 18.sp)
        }

        Button(
            onClick = { bleManager.stopScanning() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Stop Scanning", fontSize = 18.sp)
        }

        // Show a list of scanned devices
        Text("Dispositivi trovati:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    onConnect = {
                        bleManager.connectToPeripheral(device.address)
                        onConnectButtonClick()
                    }
                )
            }
        }
    }
}

@Composable
fun ConnectedScreen(
    bleManager: BleManager,
    onDisconnectButtonClick: () -> Unit
) {

    var ledValue: MutableState<String?> = mutableStateOf(null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sei connesso al dispositivo!", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
        Button(
            onClick = {onDisconnectButtonClick(); bleManager.disconnectFromPeripheral()},
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Disconnetti", fontSize = 18.sp)
        }
        Button(
            onClick = { bleManager.writeCharacteristic("f47ac10b-58cc-4372-a567-0e02b2c3d480", "ON") }
        ){
            Text("Accendi led")
        }
        Button(
            onClick = { bleManager.writeCharacteristic("f47ac10b-58cc-4372-a567-0e02b2c3d480", "OFF") }
        ){
            Text("Spegni led")
        }
        val scaffoldState = rememberScaffoldState() // Stato dello Scaffold
        val scope = rememberCoroutineScope()

        Scaffold(scaffoldState = scaffoldState) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        bleManager.readCharacteristic("f47ac10b-58cc-4372-a567-0e02b2c3d480") { value ->
                            // Gestisci il valore restituito nel callback
                            if (value != null) {
                                // Mostra il valore letto
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = value,
                                        actionLabel = "OK"
                                    )
                                }
                            } else {
                                // In caso di errore
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "Errore durante la lettura.",
                                        actionLabel = "OK"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Leggi stato")
                }

            }
        }
    }
}





@Composable
fun DeviceItem(device: BleDevice, onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Nome: ${device.name ?: "Sconosciuto"}",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "MAC: ${device.address}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Connetti", fontSize = 18.sp)
        }
    }
}
