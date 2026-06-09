package com.example.hfpropagation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object SolarUtils {
    // NOAA daily solar indices (text, last 30 days); col 5 = SESC Sunspot Number
    private const val SOLAR_URL = "https://services.swpc.noaa.gov/text/daily-solar-indices.txt"

    suspend fun fetchCurrentSSN(): Int = withContext(Dispatchers.IO) {
        try {
            val lines = URL(SOLAR_URL).readText().lines()
            // Skip header/comment lines (start with ':' or '#' or are blank)
            val dataLines = lines.filter { it.isNotBlank() && !it.startsWith(':') && !it.startsWith('#') }
            val lastLine = dataLines.lastOrNull() ?: return@withContext 70
            val fields = lastLine.trim().split("\\s+".toRegex())
            // fields: year(0) month(1) day(2) flux(3) ssn(4) ...
            fields.getOrNull(4)?.toIntOrNull() ?: 70
        } catch (e: Exception) {
            70
        }
    }
}