package com.example.hfpropagation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object SolarUtils {
    // NOAA's real-time solar indices JSON
    private const val SOLAR_URL = "https://services.swpc.noaa.gov/json/solar-indices.json"

    suspend fun fetchCurrentSSN(): Int = withContext(Dispatchers.IO) {
        try {
            val response = URL(SOLAR_URL).readText()
            // Regex to find the most recent "ssn" value in the JSON response
            val ssnMatch = "\"ssn\":(\\d+)".toRegex().find(response)
            ssnMatch?.groupValues?.get(1)?.toInt() ?: 70
        } catch (e: Exception) {
            70 // Fallback if internet is down
        }
    }
}