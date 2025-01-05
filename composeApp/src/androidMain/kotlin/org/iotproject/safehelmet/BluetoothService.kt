package org.iotproject.safehelmet

import android.app.Application
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import dev.bluefalcon.ServiceFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothService(application: Application) {
    private val blueFalcon = BlueFalcon(log = null, application)

    private val _peripherals = MutableStateFlow<List<BluetoothPeripheral>>(emptyList())
    val peripherals: StateFlow<List<BluetoothPeripheral>> = _peripherals

    fun startScanning() {
        val filter: ServiceFilter? = null
        blueFalcon.scan(filter)
    }

    fun stopScanning() {
        blueFalcon.stopScanning()
    }
}
