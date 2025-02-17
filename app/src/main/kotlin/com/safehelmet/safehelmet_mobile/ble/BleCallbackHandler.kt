package com.safehelmet.safehelmet_mobile.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.safehelmet.safehelmet_mobile.parse.BaseParse
import com.safehelmet.safehelmet_mobile.parse.ParseCollector
import com.safehelmet.safehelmet_mobile.parse.ParseCrash1
import com.safehelmet.safehelmet_mobile.parse.ParseCrash2
import com.safehelmet.safehelmet_mobile.parse.ParseData
import com.safehelmet.safehelmet_mobile.parse.ParseSleep
import java.nio.charset.StandardCharsets
import java.util.UUID

fun uuidFrom16Bit(shortUuid: Int): UUID {
    return UUID.fromString(String.format("%04x-0000-1000-8000-00805f9b34fb", shortUuid))
}


class BleCallbackHandler(private val context: Context, private val onDisconnected: () -> Unit) : BluetoothGattCallback() {

    private fun subscribeToCharacteristic(gatt: BluetoothGatt, characteristicUUID: Int) {
        val dataCharacteristic =
            gatt.services[2].characteristics.find { characteristic ->
                characteristic.descriptors.any { descriptor ->
                    descriptor.uuid == uuidFrom16Bit(characteristicUUID)
                }
            }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (gatt.setCharacteristicNotification(dataCharacteristic, true)) {
            Log.i("BluetoothManager", "Notification activated for the characteristic")
        } else {
            Log.e("BluetoothManager", "Error in the configuration of notification")
        }

    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        when (newState) {
            android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                Log.i("BluetoothManager", "Connected to ${gatt.device.address}")
                // scan for available services
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.discoverServices()
                }
            }

            android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                Log.i("BluetoothManager", "Disconnected from ${gatt.device.address}")
                gatt.close()
                // Chiamata alla callback per aggiornare la UI
                onDisconnected()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i("BluetoothManager", "Services discovered for ${gatt.device.address}")

            subscribeToCharacteristic(gatt, 0x0044)  // data
            subscribeToCharacteristic(gatt, 0x4331)  // crash1
            subscribeToCharacteristic(gatt, 0x4332)  // crash2
            subscribeToCharacteristic(gatt, 0x0053)  // sleep

        } else {
            Log.e("BluetoothManager", "Error in the discovery of services: $status")
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val newString = String(value, StandardCharsets.UTF_8)
            Log.i("BluetoothManager", "Characteristic read: $newString")
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)

        val parseValue: BaseParse? = when (characteristic.descriptors[1].uuid) {
            uuidFrom16Bit(0x0044) -> ParseData(value)
            uuidFrom16Bit(0x4331) -> ParseCrash1(value)
            uuidFrom16Bit(0x4332) -> ParseCrash2(value)
            uuidFrom16Bit(0x0053) -> ParseSleep(value)
            else -> null
        }
        ParseCollector.processParse(parseValue)

        parseValue?.let { Log.i("BluetoothManager", it.printValues()) }
    }
}
