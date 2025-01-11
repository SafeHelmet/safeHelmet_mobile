package org.iotproject.safehelmet

import androidx.compose.ui.window.ComposeUIViewController
import dev.bluefalcon.ApplicationContext

fun MainViewController() = ComposeUIViewController { BluetoothScreenWrapper(BluetoothService(
    ApplicationContext.sharedApplication()
)) }