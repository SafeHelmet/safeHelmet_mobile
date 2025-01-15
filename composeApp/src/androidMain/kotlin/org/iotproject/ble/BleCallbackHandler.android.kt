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
import java.util.UUID.fromString


actual class BleCallbackHandler(private val context: Context) : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        when (newState) {
            android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                Log.i("BluetoothManager", "Connesso a ${gatt.device.address}")
                // Scansiona i servizi disponibili
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.discoverServices()
                }
            }

            android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                Log.i("BluetoothManager", "Disconnesso da ${gatt.device.address}")
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i("BluetoothManager", "Servizi scoperti per ${gatt.device.address}")
            val notifyCharacteristic =
                gatt.getService(fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"))
                    .getCharacteristic(fromString("f47ac10b-58cc-4372-a567-0e02b2c3d481"))
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            if (gatt.setCharacteristicNotification(notifyCharacteristic, true)) {
                Log.i("BluetoothManager", "Notifica attivata per la caratteristica")
            } else {
                Log.e("BluetoothManager", "Errore nella configurazione della notifica")
            }

            val descriptor0 = notifyCharacteristic.descriptors[0]
            if (gatt.writeDescriptor(
                    descriptor0,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == BluetoothStatusCodes.SUCCESS
            ) {
                Log.i("BluetoothManager", "Descrizione attivata")
            } else {
                Log.e("BluetoothManager", "Errore nella configurazione della descrizione")
            }

        } else {
            Log.e("BluetoothManager", "Errore nella scoperta dei servizi: $status")
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
            Log.i("BluetoothManager", "Caratteristica letta: ${newString}")
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        Log.i("BluetoothManager", "Caratteristica cambiata: ${String(value)}")
    }
}