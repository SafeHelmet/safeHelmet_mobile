package org.iotproject.safehelmet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import org.iotproject.ble.BleManager
import org.iotproject.safehelmet.ui.BluetoothScreenWrapper

class MainActivity : ComponentActivity() {
    private lateinit var bleManager: BleManager

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.i("BluetoothManager", "Bluetooth abilitato con successo.")
            } else {
                Log.e("BluetoothManager", "L'utente ha rifiutato di abilitare il Bluetooth.")
            }
        }

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Log.i("BluetoothManager", "Permessi concessi")
                bleManager.requestEnableBluetooth()
            } else {
                Log.e("BluetoothManager", "Permessi mancanti")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)

        // Inizializza il launcher nella classe BluetoothManager
        bleManager.initializeBluetoothManager(this, enableBluetoothLauncher, permissionsLauncher)

        bleManager.initializeBluetooth()


        setContent {
            BluetoothScreenWrapper(bleManager)
        }
    }


}