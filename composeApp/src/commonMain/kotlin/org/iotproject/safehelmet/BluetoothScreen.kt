package org.iotproject.safehelmet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BluetoothScreen(
    devices: List<String>,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onDeviceSelected: (String) -> Unit,
    onLedCommand: (String) -> Unit // Callback per inviare il comando LED
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onStartScanning) {
                Text("Start Scanning")
            }
            Button(onClick = onStopScanning) {
                Text("Stop Scanning")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(devices) { device ->
                Text(
                    text = device,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            onDeviceSelected(device)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pulsante per accendere il LED
        Button(onClick = { onLedCommand("ON") }) {
            Text("Accendi LED")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pulsante per spegnere il LED
        Button(onClick = { onLedCommand("OFF") }) {
            Text("Spegni LED")
        }
    }
}
