package org.iotproject.safehelmet

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

actual class BLEManager(private val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    init {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    // Funzione per controllare e richiedere i permessi
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String?) {
        if (!checkPermissions()) {
            Log.e(TAG, "Permessi BLE non concessi.")
            return
        }

        val device: BluetoothDevice? = try {
            bluetoothAdapter?.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Indirizzo dispositivo non valido: $deviceAddress", e)
            return
        }

        if (device == null) {
            Log.e(TAG, "Dispositivo non trovato.")
            return
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        Log.d(TAG, "Connessione in corso...")
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Log.d(TAG, "Connesso al dispositivo.")
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnesso dal dispositivo.")
                    gatt.close()
                    bluetoothGatt = null
                }
                else -> Log.d(TAG, "Stato della connessione cambiato: $newState")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Servizi scoperti.")
            } else {
                Log.e(TAG, "Errore nella scoperta dei servizi: $status")
            }
        }
    }

    fun findCharacteristics(serviceUUID: UUID) {
        if (!checkPermissions()) {
            Log.e(TAG, "Permessi BLE non concessi.")
            return
        }

        val service = bluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            for (characteristic in service.characteristics) {
                Log.d(TAG, "Caratteristica trovata: ${characteristic.uuid}")
            }
        } else {
            Log.e(TAG, "Servizio non trovato: $serviceUUID")
        }
    }

    @SuppressLint("MissingPermission")
    fun writeToCharacteristic(serviceUUID: UUID, characteristicUUID: UUID, value: ByteArray?) {
        if (!checkPermissions()) {
            Log.e(TAG, "Permessi BLE non concessi.")
            return
        }

        val service = bluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicUUID)
            if (characteristic != null) {
                characteristic.value = value
                val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
                if (success) {
                    Log.d(TAG, "Scrittura caratteristica in corso...")
                } else {
                    Log.e(TAG, "Errore nella scrittura della caratteristica.")
                }
            } else {
                Log.e(TAG, "Caratteristica non trovata: $characteristicUUID")
            }
        } else {
            Log.e(TAG, "Servizio non trovato: $serviceUUID")
        }
    }

    @SuppressLint("MissingPermission")
    fun closeConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.d(TAG, "Connessione GATT chiusa.")
    }

    @SuppressLint("MissingPermission")
    fun startScanning(scanCallback: ScanCallback) {
        if (!checkPermissions()) {
            Log.e(TAG, "Permessi BLE non concessi.")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.startScan(scanCallback)
        Log.d(TAG, "Scansione avviata.")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning(scanCallback: ScanCallback) {
        if (!checkPermissions()) {
            Log.e(TAG, "Permessi BLE non concessi.")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
        Log.d(TAG, "Scansione interrotta.")
    }


    companion object {
        private const val TAG = "org.iotproject.safehelmet.BLEManager"
    }
}
