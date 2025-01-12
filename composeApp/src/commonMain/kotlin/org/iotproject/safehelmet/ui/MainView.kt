package org.iotproject.safehelmet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.iotproject.ble.BleManager


@Composable
fun BluetoothScreenWrapper(bleManager: BleManager) {
    Column(){
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
