package org.iotproject.safehelmet

import co.touchlab.kermit.Logger
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.flow.MutableStateFlow


class BluetoothService(context: ApplicationContext) {

    private val blueFalcon = BlueFalcon(log = null, context = context)
    val peripherals = MutableStateFlow<Set<BluetoothPeripheral>>(emptySet())


    fun startScanning() {
        blueFalcon.scan()
        blueFalcon.peripherals.collect { peripheral ->
            Logger.i(tag = "PERI", messageString = "Dispositivo trovato: $peripheral")
            peripherals.value += peripheral
        }
    }

    fun stopScanning() {
        blueFalcon.stopScanning()
    }
}

