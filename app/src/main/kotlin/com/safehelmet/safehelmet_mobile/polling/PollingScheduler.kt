package com.safehelmet.safehelmet_mobile.polling

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.safehelmet.safehelmet_mobile.api.HttpClient
import org.json.JSONObject
import com.safehelmet.safehelmet_mobile.BackendValues as appContext

class PollingScheduler(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        try {

            Log.i("Polling", "Polling scheduler started")

            // Se il valore è vero, avvisa il caschetto in BLE
            if (isReadingAnomaly()) {
                adviseBLEHelmet()
            }

            return Result.success() // Indica che il lavoro è stato completato con successo
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry() // Ritenta se c'è stato un errore
        }
    }

    private fun isReadingAnomaly(): Boolean {
        return try {
            val response = HttpClient.getRequestSync("/api/v1/polling/${appContext.helmetID}")
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
        //BluetoothManager.adviseForAnomaly()
    }
}
