package org.iotproject.safehelmet

import androidx.compose.ui.window.ComposeUIViewController
import dev.bluefalcon.ApplicationContext

// TODO al posto di App ci va BluetoothScreenWrapper

fun MainViewController() = ComposeUIViewController { BluetoothScreenWrapper(BluetoothService(
    ApplicationContext()
)) }