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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.safehelmet.safehelmet_mobile.ble.BleDevice
import com.safehelmet.safehelmet_mobile.ble.BleManager
import com.safehelmet.safehelmet_mobile.parse.ParseCollector
import com.safehelmet.safehelmet_mobile.ui.theme.Purple40
import kotlinx.coroutines.launch
import login
import org.json.JSONException
import org.json.JSONObject


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
                            //isLogin.value = true
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
    val jsonMap = remember(data) { parseJsonToMap(data) }

    val usesGasProtection = jsonMap["uses_gas_protection"]?.toBoolean() ?: false
    val usesWeldingProtection = jsonMap["uses_welding_protection"]?.toBoolean() ?: false

    val groupedData = remember(jsonMap) { groupData(jsonMap) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section (Protections and Title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f) // Adjust weight as needed
        ) {
            Text(
                "Sei connesso al dispositivo!",
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProtectionStatusLabel(label = "Gas Protection", isActive = usesGasProtection)
                ProtectionStatusLabel(label = "Welding Protection", isActive = usesWeldingProtection)
            }
        }

        // Middle Section (Grouped Data Cards)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState()) // Correct way to add vertical scrolling
        ) {
            // Gas Data Group
            GroupedDataCard(title = "Gas Rilevati", fields = groupedData["gas"] ?: emptyList())
            Spacer(modifier = Modifier.height(8.dp)) // Spacing between cards

            // Standard Values Group
            GroupedDataCard(title = "Standard Values", fields = groupedData["std_"] ?: emptyList())
            Spacer(modifier = Modifier.height(8.dp))

            // Average Values Group
            GroupedDataCard(title = "Average Values", fields = groupedData["avg_"] ?: emptyList())
            Spacer(modifier = Modifier.height(8.dp))

            // Other Data Group
            GroupedDataCard(title = "Other Data", fields = groupedData["other"] ?: emptyList())
        }


        // Bottom Section (Disconnect Button)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f) // Adjust weight as needed
        ) {
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
}


@Composable
fun GroupedDataCard(title: String, fields: List<Map.Entry<String, String>>) {
    if (fields.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Elevation for the bigger card
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    color = Purple40,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                fields.forEach { entry ->
                    FieldItem(key = entry.key, value = entry.value)
                }
            }
        }
    }
}



@Composable
fun FieldItem(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reduced vertical padding
        verticalAlignment = Alignment.CenterVertically // Align items vertically in the row
    ) {
        Text(
            text = key + ":", // Added colon for better readability
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Purple40,
            modifier = Modifier.weight(0.4f) // Key takes 40% of the width
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(0.6f) // Value takes 60% of the width
        )
    }
}

// Funzione che analizza il JSON e lo raggruppa in base ai prefissi
fun groupData(jsonMap: Map<String, String>): Map<String, List<Map.Entry<String, String>>> {
    val grouped = mutableMapOf<String, MutableList<Map.Entry<String, String>>>()

    jsonMap.forEach { entry ->
        when {
            entry.key.startsWith("std_") -> {
                grouped.getOrPut("std_") { mutableListOf() }.add(entry)
            }
            entry.key.startsWith("avg_") -> {
                grouped.getOrPut("avg_") { mutableListOf() }.add(entry)
            }
            entry.key == "methane" || entry.key == "carbon_monoxide" || entry.key == "smoke_detection" -> {
                grouped.getOrPut("gas") { mutableListOf() }.add(entry)
            }
            else -> {
                grouped.getOrPut("other") { mutableListOf() }.add(entry)
            }
        }
    }

    return grouped
}

@Composable
fun ProtectionStatusLabel(label: String, isActive: Boolean) {
    Row(
        //modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Text(
            text = if (isActive) "Attivo" else "Non Attivo",
            fontSize = 16.sp,
            color = if (isActive) Color.Green else Color.Red,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}


// Funzione che analizza il JSON e lo trasforma in una mappa di chiavi e valori
fun parseJsonToMap(json: String): Map<String, String> {
    // Usa una libreria come Gson o Moshi per fare il parsing del JSON
    // Qui usiamo un formato fittizio per l'esempio
    return try {
        val jsonObject = JSONObject(json)
        jsonObject.keys().asSequence().associateWith { jsonObject[it].toString() }
    } catch (e: JSONException) {
        emptyMap() // In caso di errore, restituisce una mappa vuota
    }
}




@Composable
fun DeviceItem(device: BleDevice, onConnect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
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
                    color = Purple40
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,
                    fontSize = 12.sp,
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


