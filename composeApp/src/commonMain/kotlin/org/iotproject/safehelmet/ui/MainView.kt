package org.iotproject.safehelmet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.iotproject.ble.BleManager


@Composable
fun BluetoothScreenWrapper(bleManager: BleManager) {
    Column(modifier = Modifier.padding(16.dp)){
        Button(
            onClick = { bleManager.startScanning() }
        ){
            Text("Start Scanning")
        }

        Button(
            onClick = { bleManager.stopScanning() }
        ){
            Text("Stop Scanning")
        }

    }
}
