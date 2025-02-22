package com.safehelmet.safehelmet_mobile

//import com.safehelmet.safehelmet_mobile.polling.PollingScheduler
import com.safehelmet.safehelmet_mobile.api.HttpClient
import com.safehelmet.safehelmet_mobile.ble.BleDevice
import com.safehelmet.safehelmet_mobile.ble.BleManager
import com.safehelmet.safehelmet_mobile.parse.ParseCollector
import com.safehelmet.safehelmet_mobile.ui.theme.Purple40
import login
import LoginScreen

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.work.*
import com.safehelmet.safehelmet_mobile.polling.PollingScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import org.json.JSONException
import org.json.JSONObject


class MainActivity : ComponentActivity() {
    var isLogin = mutableStateOf(false)
    private lateinit var bleManager: BleManager
    val pollingManager = PollingScheduler()



    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.i("BluetoothManager", "Bluetooth enabled successfully.")
            } else {
                Log.e("BluetoothManager", "The user refused to enable Bluetooth.")
                bleManager.stopScanning(true)
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

//    /// TODO: Probabilmente meglio spostare in dopo che mi sono connesso al casco
//    private fun scheduleApiWorker(context: Context) {
//        val workRequest = PeriodicWorkRequestBuilder<PollingScheduler>(10, java.util.concurrent.TimeUnit.SECONDS)
//            .setConstraints(
//                Constraints.Builder()
//                    .setRequiredNetworkType(NetworkType.CONNECTED) // Esegue solo se c'è internet
//                    .build()
//            )
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//            "ApiWorkerJob",
//            ExistingPeriodicWorkPolicy.KEEP, // Evita duplicati
//            workRequest
//        )
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)

        // Initialize the launcher in the BluetoothManager class
        bleManager.initializeBluetoothManager(this, enableBluetoothLauncher, permissionsLauncher)
        bleManager.initializeBluetooth()

        // Registra il BluetoothReceiver per ricevere i cambiamenti dello stato del Bluetooth
        bleManager.registerReceiver()

        pollingManager.startPolling()

        setContent {
            var firstTimeInSettings by remember { mutableStateOf(true) } // Controlla se è il primo accesso ai Settings

            if (!isLogin.value) {
                LoginScreen { username, password ->
                    lifecycleScope.launch {
                        val loginSuccessful = login(username, password)

                        if (loginSuccessful) {
                            isLogin.value = true
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Not a valid login",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else if (firstTimeInSettings) {
                // Mostra SettingsScreen con pulsante "OK"
                SettingsScreen(
                    firstAccess = true,
                    onConfirm = { firstTimeInSettings = false } // Dopo la conferma, va a NonConnectedScreen
                )
            } else {
                //scheduleApiWorker(this)
                BluetoothScreenWrapper(bleManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Deregistra il BluetoothReceiver per evitare memory leaks
        bleManager.unregisterReceiver()
    }
}

enum class ConnectionState {
    NON_CONNECTED,
    CONNECTED
}

enum class Screen {
    NON_CONNECTED, SETTINGS
}

@Composable
fun BluetoothScreenWrapper(bleManager: BleManager) {
    var connectionState by remember { mutableStateOf(ConnectionState.NON_CONNECTED) }
    var connectedDeviceName by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.NON_CONNECTED) } // Stato per la navigazione
    val devices = remember { mutableStateListOf<BleDevice>() }
    val isBluetoothEnabled by bleManager.isBluetoothEnabled.collectAsState()

    val safeHelmetRegex = Regex("^SafeHelmet-.*")

    bleManager.onDevicesFound = { foundDevices: Set<BleDevice> ->
        devices.clear()
        val filteredDevices = foundDevices.filter { device ->
            device.name?.matches(safeHelmetRegex) == true
        }
        devices.addAll(filteredDevices)
    }

    val onStartScanning = { devices.clear() }
    val context = LocalContext.current

    // Imposta la callback per la disconnessione
    LaunchedEffect(Unit) {
        bleManager.onDisconnected = {
            connectionState = ConnectionState.NON_CONNECTED
            connectedDeviceName = null
            currentScreen = Screen.NON_CONNECTED
            // Usare MainScope per assicurarsi che il Toast venga eseguito sul thread principale
            MainScope().launch {
                Toast.makeText(
                    context,
                    "The device disconnected unexpectedly",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Se siamo connessi, mostriamo sempre la ConnectedScreen
    if (connectionState == ConnectionState.CONNECTED) {
        ConnectedScreen(
            bleManager = bleManager,
            connectedDeviceName = connectedDeviceName,
            onDisconnectButtonClick = {
                connectionState = ConnectionState.NON_CONNECTED
                connectedDeviceName = null
            }
        )
    } else {
        // Altrimenti, gestiamo la navigazione tra NON_CONNECTED e SETTINGS
        when (currentScreen) {
            Screen.NON_CONNECTED -> NonConnectedScreen(
                devices = devices,
                bleManager = bleManager,
                onConnectButtonClick = { deviceName ->
                    connectedDeviceName = deviceName
                    connectionState = ConnectionState.CONNECTED
                },
                onStartScanning = onStartScanning,
                onNavigateToSettings = {
                    currentScreen = Screen.SETTINGS
                }, // Callback per navigare a Settings
                isBluetoothEnabled = isBluetoothEnabled
            )

            Screen.SETTINGS -> SettingsScreen(
                onConfirm = { currentScreen = Screen.NON_CONNECTED } // Callback per tornare indietro
            )
        }
    }
}

@Composable
fun NonConnectedScreen(
    devices: List<BleDevice>,
    bleManager: BleManager,
    onConnectButtonClick: (String) -> Unit,
    onStartScanning: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isBluetoothEnabled: Boolean
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("Settings", fontSize = 18.sp) }

        Button(
            onClick = {
                if (isBluetoothEnabled) {
                    bleManager.startScanning()
                    onStartScanning()
                } else {
                    Log.e("BluetoothManager", "Cannot start scanning: Bluetooth is OFF.")
                }
            },
            enabled = isBluetoothEnabled,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("Start Scanning", fontSize = 18.sp) }

        Button(
            onClick = { bleManager.stopScanning() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("Stop Scanning", fontSize = 18.sp) }

        Text("Devices found:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    onConnect = {
                        bleManager.connectToPeripheral(device.address)
                        onConnectButtonClick(device.name ?: "Unknown")

                        CoroutineScope(Dispatchers.IO).launch {
                            // Prima richiesta API
                            val helmetResponse = HttpClient.getRequestSync(
                                "/api/v1/helmets/mac-address/${device.address}"
                            )

                            if (helmetResponse?.isSuccessful == true) {
                                val jsonResponse = JSONObject(helmetResponse.body?.string().orEmpty())
                                BackendValues.helmetID = jsonResponse.getInt("helmet_id").toString()
                            }

                            // Seconda richiesta API se la prima ha successo
                            BackendValues.helmetID?.let { helmetID ->
                                val attendanceResponse = HttpClient.getRequestSync(
                                    "/api/v1/attendance/check-existance/${BackendValues.workerID}/${BackendValues.worksiteID}/$helmetID"
                                )

                                if (attendanceResponse?.isSuccessful == true) {
                                    val jsonResponse2 = JSONObject(attendanceResponse.body?.string().orEmpty())
                                    BackendValues.attendanceID = jsonResponse2.getJSONObject("attendance").getString("id")
                                }
                            }

                            BackendValues.helmetID?.let { Log.i("BackendValues.helmetID", it) }
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun SettingsScreen(
    firstAccess: Boolean = false, // Aggiunto per distinguere il primo accesso
    onConfirm: () -> Unit
) {
    var selectedWorksite by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var worksiteMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isWorksiteSelected by remember { mutableStateOf(false) } // Controlla se è stato scelto un worksite

    // Ottieni i cantieri dall'API
    HttpClient.getRequest("/api/v1/workers/${BackendValues.workerID}/worksite") { response ->
        response?.body?.string()?.let { responseBody ->
            val jsonResponse = JSONObject(responseBody) // Analizza la risposta come JSONObject
            val worksiteArray = jsonResponse.getJSONArray("worksites") // Ottieni l'array "worksites"

            for (i in 0 until worksiteArray.length()) {
                val worksite = worksiteArray.getJSONObject(i)
                val id = worksite.getInt("id")
                val name = worksite.getString("name")
                worksiteMap = worksiteMap + (id to name) // Aggiungi la coppia ID -> Name al dizionario
            }


        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                if (!firstAccess || isWorksiteSelected) {
                    onConfirm()
                }
            },
            enabled = (!firstAccess || isWorksiteSelected), // Disabilita se non è selezionato
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(if (firstAccess) "OK" else "Back", fontSize = 18.sp)
        }

        Text("Settings", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        // Label
        Text("Select worksite:", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedWorksite ?: "Choose a worksite")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                worksiteMap.forEach { (id, worksite) ->
                    DropdownMenuItem(
                        text = { Text(worksite) },
                        onClick = {
                            selectedWorksite = worksite
                            expanded = false
                            BackendValues.worksiteID = id.toString()
                            isWorksiteSelected = true // Imposta come selezionato
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectedScreen(
    bleManager: BleManager,
    connectedDeviceName: String?, // Riceve il nome del dispositivo
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
        ) {
            Text(
                "Connected to: ${connectedDeviceName ?: "Unknown device"}", // Mostra il nome
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProtectionStatusLabel(label = "Gas Protection: ", isActive = usesGasProtection)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProtectionStatusLabel(
                    label = "Welding Protection: ",
                    isActive = usesWeldingProtection
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            GroupedDataCard(
                title = "Gas/smoke Presence",
                fields = groupedData["gas"] ?: emptyList()
            )
            Spacer(modifier = Modifier.height(8.dp))
            GroupedDataCard(
                title = "Standard Deviation Accelerometer Values",
                fields = groupedData["std_"] ?: emptyList()
            )
            Spacer(modifier = Modifier.height(8.dp))
            GroupedDataCard(
                title = "Mean Average Accelerometer Values",
                fields = groupedData["avg_"] ?: emptyList()
            )
            Spacer(modifier = Modifier.height(8.dp))
            GroupedDataCard(title = "Other Data", fields = groupedData["other"] ?: emptyList())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)
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
                Text("Disconnect", fontSize = 18.sp)
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
    val displayValue = when {
        value == "true" -> "Detected"
        value == "false" -> "Undetected"
        key == "incorrect_posture" -> {
            val numericValue = value.toFloatOrNull() ?: 0f
            "${(numericValue * 100).toInt()} %"  // Moltiplica per 100 e aggiungi il simbolo di percentuale
        }
        else -> value + getUnitForKey(key)
    }

    val displayKey = keyMappings[key] ?: key  // Usa il nome leggibile, se disponibile

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reduced vertical padding
        verticalAlignment = Alignment.CenterVertically // Align items vertically in the row
    ) {
        Text(
            text = displayKey + ": ", // Added colon for better readability
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Purple40,
            modifier = Modifier.weight(0.5f) // Key takes 50% of the width
        )
        Text(
            text = displayValue,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(0.5f) // Value takes 50% of the width
        )
    }
}

fun getUnitForKey(key: String): String {
    return when {
        key.contains("temperature", ignoreCase = true) -> " °C"
        key.contains("humidity", ignoreCase = true) -> " %"
        key.contains("brightness", ignoreCase = true) -> " lux"
        key.contains("_g", ignoreCase = true) -> " g"
        key.startsWith("std_") || key.startsWith("avg_") -> " m/s²"
        else -> ""
    }
}

val keyMappings = mapOf(
    "std_x" to "X",
    "std_y" to "Y",
    "std_z" to "Z",
    "std_g" to "Magnitude",
    "avg_x" to "X",
    "avg_y" to "Y",
    "avg_z" to "Z",
    "avg_g" to "Magnitude",
    "max_g" to "Max. magnitude",
    "methane" to "Methane",
    "carbon_monoxide" to "Carbon Monoxide",
    "smoke_detection" to "Smoke",
    "temperature" to "Temperature",
    "humidity" to "Humidity",
    "brightness" to "Brightness",
    "incorrect_posture" to "Incorrect posture"
)

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

            entry.key == "uses_welding_protection" || entry.key == "uses_gas_protection" || entry.key == "attendance_id" -> {
                // Non aggiungere questi elementi a nessun gruppo, vengono ignorati
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
            text = if (isActive) "Active" else "Not plugged in",
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
                    text = device.name ?: "Unknown",
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
                Text("Connect", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}


