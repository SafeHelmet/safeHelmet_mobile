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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PollingScheduler(private val bleManager: BleManager, private val context: Context) {
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getAnomalyNums(): Int {
        return try {
            val response = HttpClient.getRequestSync("/api/v1/polling/${BackendValues.helmetID}")
            if (response?.isSuccessful == true) {
                val json = JSONObject(response.body?.string() ?: "{}")
                json.getInt("Anomalies")
            } else {
                Log.e("Polling", "Errore HTTP: ${response?.code}")
                -1 // Valore di fallback in caso di errore
            }
        } catch (e: Exception) {
            Log.e("Polling", "Errore durante la richiesta HTTP", e)
            -1 // Valore di fallback in caso di eccezione
        }
    }
    private fun adviseBLEHelmet() {
        bleManager.adviseForAnomaly()
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return // Evita doppio avvio

        pollingJob = scope.launch {
            while (isActive) { // Controlla se il polling è attivo
                val notificationDelay = (1000 * 90).toLong() // 90 secondi
                delay(notificationDelay) // Attendi 90 secondi prima di ripetere

                val currentTime = LocalTime.now()
                val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val lastTime = currentTime.minusSeconds(notificationDelay/1000).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                try {
                    val anomalyNums = getAnomalyNums()
                    if (anomalyNums > 0) {
                        adviseBLEHelmet()
                        PollingNotification.showNotification(
                            context,
                            "Warning!",
                            "$anomalyNums anomalies detected in your worksite between $lastTime and $formattedTime!"
                        )
                        Log.i("Polling", "Anomaly detected")
                    }else if (anomalyNums == 0){
                        Log.i("Polling", "No anomaly detected")
                    } else {
                        Log.i("Polling", "Error getting anomaly numbers")
                    }
                    Log.i("Polling", "Waiting for next polling...")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel() // Ferma il polling
        pollingJob = null
    }
}

