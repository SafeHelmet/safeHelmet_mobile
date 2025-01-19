package org.iotproject.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.nio.charset.StandardCharsets
import java.util.UUID


//    uuidFrom16Bit(0x0044)
//    uuidFrom16Bit(0x0053)
fun uuidFrom16Bit(shortUuid: Int): UUID {
    return UUID.fromString(String.format("%04x-0000-1000-8000-00805f9b34fb", shortUuid))
}


actual class BleCallbackHandler(private val context: Context) : BluetoothGattCallback() {

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

        val descriptor0 = dataCharacteristic?.descriptors?.get(0)
        if (descriptor0?.let {
                gatt.writeDescriptor(
                    it,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            } == BluetoothStatusCodes.SUCCESS
        ) {
            Log.i("BluetoothManager", "Description activated")
        } else {
            Log.e("BluetoothManager", "Error in the configuration of the description")
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
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i("BluetoothManager", "Services discovered for ${gatt.device.address}")

            subscribeToCharacteristic(gatt,0x0044)
            subscribeToCharacteristic(gatt,0x0053)

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
        val parseSensorData = ParseSensorData(value)
        Log.i("BluetoothManager", parseSensorData.printValues())
    }
}
