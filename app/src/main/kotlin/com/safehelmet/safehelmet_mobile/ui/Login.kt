import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.safehelmet.safehelmet_mobile.api.HttpClient

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onLogin(username, password)
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}

// Funzione di login asincrona
suspend fun login(username: String, password: String): Boolean {
    return suspendCancellableCoroutine { continuation ->
        Log.i("Login", "Start Login Request")

        // Esegui la richiesta HTTP in modo asincrono
        HttpClient.postRequest(
            "/api/v1/login", "username=$username&password=$password"
        ) { response ->
            Log.i("Login", "Response received: ${response?.isSuccessful}")

            if (response?.isSuccessful == true) {
                // Se la risposta è positiva, prosegui e restituisci true
                continuation.resume(true)
            } else {
                // In caso di errore, restituisci false
                continuation.resume(false)
            }
        }
    }
}
