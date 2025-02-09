package com.safehelmet.safehelmet_mobile

import LoginScreen
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.safehelmet.safehelmet_mobile.api.HttpClient
import com.safehelmet.safehelmet_mobile.ble.BleDevice
import com.safehelmet.safehelmet_mobile.ble.BleManager
import com.safehelmet.safehelmet_mobile.parse.ParseCollector
import com.safehelmet.safehelmet_mobile.ui.theme.Purple40
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import login


class MainActivity : ComponentActivity() {
    var isLogin = mutableStateOf(false)
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
            if (!isLogin.value) {
                LoginScreen { username, password ->
                    lifecycleScope.launch {
                        val loginSuccessful = login(username, password)

                        if (loginSuccessful) {
                            isLogin.value = true
                        }else{
                            isLogin.value = true
                            Toast.makeText(this@MainActivity, "Not a valid login", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                BluetoothScreenWrapper(bleManager)
            }
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

    // Regex per filtrare i dispositivi il cui nome inizia con "SafeHelmet"
    val safeHelmetRegex = Regex("^SafeHelmet-.*")

    // Imposta la callback di BleManager
    bleManager.onDevicesFound = { foundDevices: Set<BleDevice> ->
        devices.clear()
        // Filtra i dispositivi il cui nome inizia con "SafeHelmet"
        val filteredDevices = foundDevices.filter { device ->
            device.name?.matches(safeHelmetRegex) == true
        }
        devices.addAll(filteredDevices) // Aggiunge solo i dispositivi filtrati
    }

    // Funzione per aggiornare la lista dei dispositivi
    val onStartScanning = {
        devices.clear() // Pulisce la lista prima di ogni scansione
    }

    when (connectionState) {
        ConnectionState.NON_CONNECTED -> NonConnectedScreen(
            devices = devices,
            bleManager = bleManager,
            onConnectButtonClick = { connectionState = ConnectionState.CONNECTED },
            onStartScanning = onStartScanning // Passa la callback per aggiornare la lista
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
    onConnectButtonClick: () -> Unit,
    onStartScanning: () -> Unit // Aggiungi questa callback
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Buttons for starting and stopping scanning
        Button(
            onClick = {
                bleManager.startScanning()
                onStartScanning() // Chiama la callback per aggiornare la lista
            },
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
    val data by ParseCollector.state

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

        Text(
            text = data,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                onDisconnectButtonClick()
                bleManager.disconnectFromPeripheral()
            },
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onConnect() }, // Permette il tap sull'intera card
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)) // Grigio chiaro
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Sconosciuto",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple40 // Blu acceso
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .height(40.dp)
                    .padding(start = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple40)
            ) {
                Text("Connetti", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}


