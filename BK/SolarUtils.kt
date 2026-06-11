package com.example.hfpropagation

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class SolarData(val ssn: Int, val sfi: Int, val kIndex: Int)

object SolarUtils {
    // 100% Verified NOAA Endpoints
    private const val SOLAR_CYCLE_URL = "https://services.swpc.noaa.gov/json/solar-cycle/observed-solar-cycle-indices.json"
    private const val K_INDEX_URL = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"

    suspend fun fetchSolarData(context: Context): SolarData? = withContext(Dispatchers.IO) {
        try {
            showToast(context, "Contacting NOAA Servers...")

            fun fetchWithAgent(urlString: String): String {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:100.0) Gecko/100.0 Firefox/100.0")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${connection.responseCode} on $urlString")
                }
                return connection.inputStream.bufferedReader().use { it.readText() }
            }

            // --- PAS 2: Desc?rcare SSN ?i SFI ---
            val dailyResponse = fetchWithAgent(SOLAR_CYCLE_URL)
            val dailyArray = JSONArray(dailyResponse)

            var ssn = -1
            var sfi = -1

            // C?ut?m de la coad? la cap cea mai recent? intrare care con?ine date valide (uneori ultimele intr?ri sunt goale)
            for (i in dailyArray.length() - 1 downTo 0) {
                val entry = dailyArray.getJSONObject(i)
                if (entry.has("ssn") && entry.has("f10.7")) {
                    val tempSsn = entry.optDouble("ssn", -1.0)
                    val tempSfi = entry.optDouble("f10.7", -1.0)

                    if (tempSsn > 0 && tempSfi > 0) {
                        ssn = tempSsn.toInt()
                        sfi = tempSfi.toInt()
                        break // Am g?sit datele, oprim c?utarea
                    }
                }
            }

            // --- PAS 3: Desc?rcare K-Index ---
            val kResponse = fetchWithAgent(K_INDEX_URL)
            val kArray = JSONArray(kResponse)
            // Structura e de tip matrice: [ ["time", "kp_index", ...], ["2026-...", "3.00", ...] ]
            val latestKEntry = kArray.getJSONArray(kArray.length() - 1)
            // K-Index vine ca un String cu zecimale (ex: "2.33" sau "3.00")
            val kIndexString = latestKEntry.getString(1)
            val kIndex = kIndexString.toDoubleOrNull()?.toInt() ?: -1

            // --- PAS 4: Validare ?i Salvare ---
            if (ssn != -1 && sfi != -1 && kIndex != -1) {
                StorageUtils.saveSSN(context, ssn)
                StorageUtils.saveSFI(context, sfi)
                StorageUtils.saveKIndex(context, kIndex)

                showToast(context, "? Data Updated: SSN=$ssn, SFI=$sfi, K=$kIndex")
                return@withContext SolarData(ssn, sfi, kIndex)
            } else {
                showToast(context, "?? NOAA returned partial data.")
                return@withContext SolarData(110, 140, 2)
            }

        } catch (e: Exception) {
            Log.e("VOACAP_SOLAR", "Fetch error: ${e.message}")
            showToast(context, "? Error: ${e.message}")
            return@withContext null
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}