package org.iotproject.safehelmet

import co.touchlab.kermit.Logger
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.flow.MutableStateFlow

class BluetoothService(context: ApplicationContext) {

    private val blueFalcon = BlueFalcon(log = null, context = context)
    val peripherals = MutableStateFlow<Set<BluetoothPeripheral>>(emptySet())
    private var connectedPeripheral: BluetoothPeripheral? = null


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

    fun connectToDevice(device: BluetoothPeripheral) {
        // Connetti al dispositivo Bluetooth
        blueFalcon.connect(device, autoConnect = true)
        connectedPeripheral = device
        Logger.i(tag = "Bluetooth", messageString = "Connesso a ${device.name}")
    }


    fun sendLedCommand(command: String) {
        connectedPeripheral?.let { peripheral ->
            val ledCharacteristic =
//                TODO MUX guarda qua, non riesco a trovare la characteristics dell'arduino
//                 Dovrebbero essere le impostazioni che fai all'inizio dello script ma non le vede
                peripheral.characteristics["f47ac10b58cc4372a5670e02b2c3d479"]

            if (ledCharacteristic == null) {
                Logger.e(tag = "Bluetooth", messageString = "Caratteristica non trovata")
                return
            }

            // Scrivere il comando sulla caratteristica
            blueFalcon.writeCharacteristic(peripheral, ledCharacteristic, command, null)
        }
    }
}
