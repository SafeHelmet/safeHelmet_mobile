package com.safehelmet.safehelmet_mobile.polling

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
        var isAnomaly = false
        HttpClient.getRequest(
            "/api/v1/polling/${appContext.helmetID}"
        ){ response ->

            val r = response?.body?.string()?.let { JSONObject(it) }
            if( r!= null)
                isAnomaly = r.getBoolean("anomaly_detected")
        }
        return isAnomaly
    }

    private fun adviseBLEHelmet() {
//        bleManager.adviseForAnomaly()
    }
}
