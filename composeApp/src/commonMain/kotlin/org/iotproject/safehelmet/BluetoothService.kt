package org.iotproject.safehelmet

import co.touchlab.kermit.Logger
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothPeripheral
import dev.bluefalcon.BluetoothService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object BleDelegate: BlueFalconDelegate {
    override fun didDiscoverServices(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.i(tag = "SERVICES", messageString = "SERVICE: ${bluetoothPeripheral.services.keys}")
        myservice = bluetoothPeripheral.services[createUuidFromString("f47ac10b-58cc-4372-a567-0e02b2c3d479")]
        myservice?.characteristics?.forEach { characteristic ->
            if (characteristic.uuid == createUuidFromString("f47ac10b-58cc-4372-a567-0e02b2c3d480")) {
                mycharacteristic = characteristic
            }
        }
    }

    override fun didDiscoverCharacteristics(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.i(tag = "CHARACTERISTICS", messageString = "CHARACTERISTICS: ${bluetoothPeripheral.characteristics.keys}")

    }
}

var mycharacteristic: BluetoothCharacteristic? = null
var myservice: BluetoothService? = null

class BluetoothService(context: ApplicationContext) {

    private val blueFalcon = BlueFalcon(log = null, context = context)
    val peripherals = MutableStateFlow<Set<BluetoothPeripheral>>(emptySet())
    private var connectedPeripheral: BluetoothPeripheral? = null


    init {
        blueFalcon.delegates.add(BleDelegate)
    }

    // Funzioni scan

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

    // funzioni per connect

    @OptIn(ExperimentalUuidApi::class)
    private fun discoverServices(peripheral: BluetoothPeripheral) {
        blueFalcon.discoverServices(peripheral)

        Logger.i(tag = "Bluetooth", messageString = "PeriperhalID: ${peripheral.uuid}")

        peripheral.services.forEach { service ->
            Logger.i(tag = "Bluetooth", messageString = "Service: ${service.key}")
            service.value.characteristics.forEach { characteristic ->
                Logger.i(tag = "Bluetooth", messageString = "Characteristic: ${characteristic.uuid}")
            }
        }
    }

    fun connectToDevice(device: BluetoothPeripheral) {
        connectedPeripheral = device
        discoverServices(connectedPeripheral!!)
        blueFalcon.connect(connectedPeripheral!!, autoConnect = true)
        Logger.i(tag = "Bluetooth", messageString = "Connesso a ${device.name}")
        stopScanning()
    }

    // funzioni after connect

    fun sendLedCommand(command: String) {


        connectedPeripheral?.let { mycharacteristic?.let { it1 ->
            blueFalcon.writeCharacteristic(it,
                it1,
                command, null)
        } }

    }
}
