package org.iotproject.safehelmet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App(
//    onConnectClick: () -> Unit,
//    onSendToArduinoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize() // Occupa tutto lo spazio disponibile
            .wrapContentSize(Alignment.Center) // Centra il contenuto nella Box
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Permette ai pulsanti di occupare il 90% della larghezza
        ) {
            Button(
                onClick = onConnectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp) // Spazio verticale tra i pulsanti
                    .height(60.dp)
            ) {
                Text("Connect")
            }
            Button(
                onClick = onSendToArduinoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(60.dp)
            ) {
                Text("Send to Arduino")
            }
        }
    }
}

// Callback functions
val onConnectClick: () -> Unit = {}
val onSendToArduinoClick: () -> Unit = {}
