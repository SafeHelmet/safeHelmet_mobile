package org.iotproject.ble

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import objcnames.classes.Protocol
import platform.CoreBluetooth.*
import platform.darwin.NSUInteger
import co.touchlab.kermit.Logger
import platform.Foundation.NSNumber
import platform.darwin.NSObject


actual class BleCallbackHandler : NSObject(), CBCentralManagerDelegateProtocol {
    var onDeviceFound: ((CBPeripheral) -> Unit)? = null
    private val logger = Logger.withTag("BluetoothManager")
    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerStateUnknown -> {
                logger.i("Bluetooth state is unknown")
            }
            CBManagerStateResetting -> {
                logger.i("Bluetooth is resetting")
            }
            CBManagerStateUnsupported -> {
                logger.i("Bluetooth is not supported on this device")
            }
            CBManagerStateUnauthorized -> {
                logger.i("Bluetooth is not authorized. Request permissions.")
                // Avvisa l'utente che i permessi sono mancanti
            }
            CBManagerStatePoweredOff -> {
                logger.i("Bluetooth is powered off. Ask the user to enable it.")
            }
            CBManagerStatePoweredOn -> {
                logger.i("Bluetooth is powered on. Start scanning.")
            }
            else -> {
                logger.w("Unhandled Bluetooth state: ${central.state}")
            }
        }
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, Any?>,
        RSSI: NSNumber
    ) {
        val deviceName = didDiscoverPeripheral.name ?: "Unknown Device"
        println("Dispositivo trovato: $deviceName")
        onDeviceFound?.invoke(didDiscoverPeripheral)
    }





    @BetaInteropApi
    override fun `class`(): ObjCClass? {
        TODO("Not yet implemented")
    }

    @ExperimentalForeignApi
    override fun conformsToProtocol(aProtocol: Protocol?): Boolean {
        TODO("Not yet implemented")
    }

    override fun description(): String? {
        TODO("Not yet implemented")
    }

    override fun hash(): NSUInteger {
        TODO("Not yet implemented")
    }

    override fun isEqual(`object`: Any?): Boolean {
        TODO("Not yet implemented")
    }

    @BetaInteropApi
    override fun isKindOfClass(aClass: ObjCClass?): Boolean {
        TODO("Not yet implemented")
    }

    @BetaInteropApi
    override fun isMemberOfClass(aClass: ObjCClass?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isProxy(): Boolean {
        TODO("Not yet implemented")
    }

    @ExperimentalForeignApi
    override fun performSelector(aSelector: COpaquePointer?): Any? {
        TODO("Not yet implemented")
    }

    @ExperimentalForeignApi
    override fun performSelector(aSelector: COpaquePointer?, withObject: Any?): Any? {
        TODO("Not yet implemented")
    }

    @ExperimentalForeignApi
    override fun performSelector(
        aSelector: COpaquePointer?,
        withObject: Any?,
        _withObject: Any?
    ): Any? {
        TODO("Not yet implemented")
    }

    @ExperimentalForeignApi
    override fun respondsToSelector(aSelector: COpaquePointer?): Boolean {
        TODO("Not yet implemented")
    }

    @BetaInteropApi
    override fun superclass(): ObjCClass? {
        TODO("Not yet implemented")
    }
}