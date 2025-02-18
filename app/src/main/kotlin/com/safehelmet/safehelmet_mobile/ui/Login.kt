import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safehelmet.safehelmet_mobile.BackendValues
import com.safehelmet.safehelmet_mobile.api.HttpClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) } // Stato per mostrare/nascondere la password

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Campo email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Checkbox per mostrare/nascondere la password
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = showPassword,
                onCheckedChange = { showPassword = it }
            )
            Text(
                text = "Show Password",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottone di login
        Button(
            onClick = {
                onLogin(email, password)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login", fontSize = 18.sp)
        }
    }
}


// Funzione di login asincrona
suspend fun login(email: String, password: String): Boolean {
    return suspendCancellableCoroutine { continuation ->
        Log.i("Login", "Start Login Request")

        val json = JSONObject()
        json.put("email", email.trim())
        json.put("password", password.trim())


        // Esegui la richiesta HTTP in modo asincrono
        HttpClient.postRequest(
            "/api/v1/login", json.toString()
        ) { response ->
            Log.i("Login", "Credentials accepted: ${response?.isSuccessful}")
            if (response?.isSuccessful == true) {
                val jsonResponse = JSONObject(response.body?.string().toString())
                BackendValues.workerID = jsonResponse.getJSONObject("user").getString("id")
                // Se la risposta è positiva, prosegui e restituisci true
                continuation.resume(true)
            } else {
                // In caso di errore, restituisci false
                continuation.resume(false)
            }
        }
    }
}
