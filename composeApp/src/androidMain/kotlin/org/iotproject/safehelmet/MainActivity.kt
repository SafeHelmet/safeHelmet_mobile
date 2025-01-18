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
                Log.i("BluetoothManager", "Bluetooth enabled successfully.")
            } else {
                Log.e("BluetoothManager", "The user refused to enable Bluetooth.")
            }
        }

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Log.i("BluetoothManager", "Permission conceded")
                bleManager.requestEnableBluetooth()
            } else {
                Log.e("BluetoothManager", "Missing permission")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)

        // Initialize the launcher in the BluetoothManager class
        bleManager.initializeBluetoothManager(this, enableBluetoothLauncher, permissionsLauncher)
        bleManager.initializeBluetooth()

        setContent {
            BluetoothScreenWrapper(bleManager)
        }
    }

}