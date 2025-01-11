package org.iotproject.safehelmet

import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.flow.MutableStateFlow

class BluetoothService(context: ApplicationContext) {

    private val blueFalcon = BlueFalcon(log = null, context = context)
    val peripherals = MutableStateFlow<Set<BluetoothPeripheral>>(emptySet())
    private var connectedPeripheral: BluetoothPeripheral? = null


    fun startScanning() {

    }

    fun stopScanning() {

    }

    fun connectToDevice(device: BluetoothPeripheral) {

    }


    fun sendLedCommand(command: String) {

    }
}
