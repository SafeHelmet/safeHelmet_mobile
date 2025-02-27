package com.safehelmet.safehelmet_mobile.polling

import android.content.Context
import com.safehelmet.safehelmet_mobile.ble.BleManager
import android.util.Log
import com.safehelmet.safehelmet_mobile.api.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.safehelmet.safehelmet_mobile.BackendValues
import com.safehelmet.safehelmet_mobile.notification.PollingNotification

class PollingScheduler(private val bleManager: BleManager, private val context: Context) {
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun isReadingAnomaly(): Boolean {
        return try {
            val response = HttpClient.getRequestSync("/api/v1/polling/${BackendValues.helmetID}")
            if (response?.isSuccessful == true) {
                val json = JSONObject(response.body?.string() ?: "{}")
                json.getBoolean("anomaly_detected")
            } else {
                Log.e("Polling", "Errore HTTP: ${response?.code}")
                false
            }
        } catch (e: Exception) {
            Log.e("Polling", "Errore durante la richiesta HTTP", e)
            false
        }
    }

    private fun adviseBLEHelmet() {
        bleManager.adviseForAnomaly()
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return // Evita doppio avvio

        pollingJob = scope.launch {
            while (isActive) { // Controlla se il polling è attivo
                try {
                    if (isReadingAnomaly()) {
                        adviseBLEHelmet()
                        PollingNotification.showNotification(
                            context,
                            "Warning!",
                            "An anomaly in your worksite has been detected!"
                        )
                        Log.i("Polling", "Anomaly detected")
                    }else{
                        Log.i("Polling", "No anomaly detected")
                    }
                    Log.i("Polling", "Waiting for next polling...")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000 * 90) // Attendi 90 secondi prima di ripetere
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel() // Ferma il polling
        pollingJob = null
    }
}

