package org.iotproject.safehelmet

import co.touchlab.kermit.Logger
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi


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

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun discoverServices(peripheral: BluetoothPeripheral) {
        blueFalcon.discoverServices(peripheral)
        delay(5000)
        Logger.i(tag = "Bluetooth", messageString = "PeriperhalID: ${peripheral.uuid}")
        peripheral.services.forEach { service ->
            Logger.i(tag = "Bluetooth", messageString = "Service: ${service.key}")
            service.value.characteristics.forEach { characteristic ->
                Logger.i(
                    tag = "Bluetooth",
                    messageString = "Characteristic: ${characteristic.uuid} ${characteristic.name}"
                )
            }
        }
    }

    suspend fun connectToDevice(device: BluetoothPeripheral) {
        // Connetti al dispositivo Bluetooth
        blueFalcon.connect(device, autoConnect = true)
        connectedPeripheral = device
        Logger.i(tag = "Bluetooth", messageString = "Connesso a ${device.name}")
        stopScanning()
        discoverServices(device)
    }

    fun sendLedCommand(command: String) {

        connectedPeripheral?.let { peripheral ->
            val ledCharacteristic =

                @OptIn(ExperimentalUuidApi::class) // This annotation makes the function aware of the experimental API
                peripheral.characteristics[createUuidFromString("f47ac10b-58cc-4372-a567-0e02b2c3d480")]

            if (ledCharacteristic == null) {
                Logger.e(tag = "Bluetooth", messageString = "Caratteristica non trovata")
                return
            }

            // Scrivere il comando sulla caratteristica
            blueFalcon.writeCharacteristic(peripheral, ledCharacteristic, command, null)
        }
    }
}
