package org.iotproject.safehelmet

import co.touchlab.kermit.Logger
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class BluetoothService(context: ApplicationContext) {
    private val blueFalcon = BlueFalcon(log = null, context = context)

    private val _peripherals = MutableStateFlow<List<BluetoothPeripheral>>(emptyList())
    val peripherals: StateFlow<List<BluetoothPeripheral>> = _peripherals

    fun startScanning() {
        blueFalcon.scan()
        blueFalcon.peripherals.collect { peripheral ->
            Logger.i(tag= "PERI", messageString = "Dispositivo trovato: ${peripheral}")
        }
    }

    fun stopScanning() {
        blueFalcon.stopScanning()
    }
}
