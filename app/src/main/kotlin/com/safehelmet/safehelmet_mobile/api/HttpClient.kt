package com.safehelmet.safehelmet_mobile.api

import android.util.Log
import com.safehelmet.safehelmet_mobile.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


object HttpClient {

    private val client = OkHttpClient()
    private const val AUTH_TOKEN = BuildConfig.API_TOKEN
    private const val BASE_URL = "https://safehelmet-backend.onrender.com"

    // Funzione per eseguire la richiesta con autorizzazione
    fun getRequest(url: String, callback: (Response?) -> Unit) {
        val request = Request.Builder()
            .url(BASE_URL+url)
            .addHeader("Authorization", AUTH_TOKEN)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Gestisci l'errore
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Gestisci la risposta
                    callback(response)
                } else {
                    // Gestisci errore nella risposta
                    Log.e("API_ERROR", "Response failed with status code: ${response.code}")
                    Log.e("API_ERROR", "Error body: ${response.body?.string() ?: "No error body"}")
                    callback(null)
                }
            }
        })
    }

    fun getRequestSync(url: String): Response? {
        return try {
            val request = Request.Builder()
                .url(BASE_URL + url)
                .addHeader("Authorization", AUTH_TOKEN)
                .build()

            val response = client.newCall(request).execute() // Chiamata sincrona

            if (response.isSuccessful) {
                response
            } else {
                Log.e("API_ERROR", "Response failed with status code: ${response.code}")
                Log.e("API_ERROR", "Error body: ${response.body?.string() ?: "No error body"}")
                null
            }
        } catch (e: IOException) {
            Log.e("API_ERROR", "Network request failed: ${e.message}", e)
            null
        }
    }



    // Funzione per eseguire una richiesta POST con autorizzazione
    fun postRequest(url: String, json: String, callback: (Response?) -> Unit) {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL+url)
            .post(body)
            .addHeader("Authorization", AUTH_TOKEN) // Aggiungi il token di autorizzazione
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(response)
                } else {
                    Log.e("API_ERROR", "Response failed with status code: ${response.code}")
                    Log.e("API_ERROR", "Error body: ${response.body?.string() ?: "No error body"}")
                    callback(null)
                }
            }
        })
    }
}
