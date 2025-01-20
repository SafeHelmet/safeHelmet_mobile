package org.iotproject.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class ApiClient {
    private val client = HttpClient()

    suspend fun greeting(): String {
        val response = client.get("https://ktor.io/docs/")
        return response.bodyAsText()
    }
}
