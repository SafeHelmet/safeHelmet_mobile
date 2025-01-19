package org.iotproject.safehelmet

import androidx.compose.ui.window.ComposeUIViewController
import org.iotproject.ble.BleManager
import org.iotproject.safehelmet.ui.BluetoothScreenWrapper

fun MainViewController() = ComposeUIViewController { BluetoothScreenWrapper(BleManager())
}