package org.iotproject.safehelmet

import android.app.Application
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import dev.bluefalcon.ServiceFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import kotlinx.coroutines.flow.forEach

class BluetoothService(application: Application) {
    private val blueFalcon = BlueFalcon(log = null, application)

    private val _peripherals = MutableStateFlow<List<BluetoothPeripheral>>(emptyList())
    val peripherals: StateFlow<List<BluetoothPeripheral>> = _peripherals

    fun startScanning() {
        blueFalcon.scan()
        blueFalcon.peripherals.collect { peripheral ->
            Log.d("PERI", "Dispositivo trovato: ${peripheral ?: "Sconosciuto"}")
        }
    }

    fun stopScanning() {
        blueFalcon.stopScanning()
    }
}
