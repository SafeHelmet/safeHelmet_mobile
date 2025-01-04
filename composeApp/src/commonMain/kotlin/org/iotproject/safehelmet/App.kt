package org.iotproject.safehelmet

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {
        Column {
            Button(onClick = { println("bt1") }){
                Text("bt1")
            }
            Button(onClick = { println("bt2") }){
                Text("bt2")
            }
        }

    }
}