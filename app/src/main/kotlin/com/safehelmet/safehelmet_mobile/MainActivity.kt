package com.safehelmet.safehelmet_mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safehelmet.safehelmet_mobile.api.HttpClient
import com.safehelmet.safehelmet_mobile.ble.BleDevice
import com.safehelmet.safehelmet_mobile.ble.BleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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


enum class ConnectionState {
    NON_CONNECTED,
    CONNECTED
}


@Composable
fun BluetoothScreenWrapper(bleManager: BleManager) {

    var connectionState by remember { mutableStateOf(ConnectionState.NON_CONNECTED) }
    val devices = remember { mutableStateListOf<BleDevice>() }

    // Imposta la callback di BleManager
    bleManager.onDevicesFound = { foundDevices: Set<BleDevice> ->
        devices.clear()
        devices.addAll(foundDevices)
    }

    when (connectionState) {
        ConnectionState.NON_CONNECTED -> NonConnectedScreen(
            devices = devices,
            bleManager = bleManager,
            onConnectButtonClick = { connectionState = ConnectionState.CONNECTED }
        )

        ConnectionState.CONNECTED -> ConnectedScreen(
            bleManager = bleManager,
            onDisconnectButtonClick = { connectionState = ConnectionState.NON_CONNECTED }
        )
    }
}

@Composable
fun NonConnectedScreen(
    devices: List<BleDevice>,
    bleManager: BleManager,
    onConnectButtonClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Buttons for starting and stopping scanning
        Button(
            onClick = { bleManager.startScanning() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Start Scanning", fontSize = 18.sp)
        }

        Button(
            onClick = { bleManager.stopScanning() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Stop Scanning", fontSize = 18.sp)
        }

        val apiClient = remember { HttpClient() }

        // Esegui la chiamata API quando il pulsante viene premuto
        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    apiClient.getRequest(
                        "https://safehelmet-backend.onrender.com/api/v1/workers",
                    ) { response ->
                        response?.body?.let { Log.i("BOH", it.string()) }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("API call")
        }

        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    apiClient.postRequest(
                        "https://safehelmet-backend.onrender.com/api/v1/worksites",
                        """
                                {
                                    "name": "Cantiere della sburra",
                                    "latitude": 45.0,
                                    "longitude": 9.0,
                                    "start_date_of_work": "2023-10-01T00:00:00Z",
                                    "end_date_of_work": "2023-12-31T00:00:00Z"
                                }
                            """

                    ) { response ->
                        response?.body?.let { Log.i("BOH", it.string()) }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Test POST")
        }

        // Show a list of scanned devices
        Text("Dispositivi trovati:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    onConnect = {
                        bleManager.connectToPeripheral(device.address)
                        onConnectButtonClick()
                    }
                )
            }
        }
    }
}

@Composable
fun ConnectedScreen(
    bleManager: BleManager,
    onDisconnectButtonClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Sei connesso al dispositivo!",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = { onDisconnectButtonClick(); bleManager.disconnectFromPeripheral() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Disconnetti", fontSize = 18.sp)
        }

    }
}


@Composable
fun DeviceItem(device: BleDevice, onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Nome: ${device.name ?: "Sconosciuto"}",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "MAC: ${device.address}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Connetti", fontSize = 18.sp)
        }
    }
}
