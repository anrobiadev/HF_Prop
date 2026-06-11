package com.example.hfpropagation

import android.content.Context
import android.content.SharedPreferences

object StorageUtils {
    private const val PREFS_NAME = "hf_prop_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Map Settings
    fun saveMapSource(context: Context, sourceName: String) {
        getPrefs(context).edit().putString("map_source", sourceName).apply()
    }

    fun loadMapSource(context: Context): String {
        return getPrefs(context).getString("map_source", "Standard") ?: "Standard"
    }

    // Solar Data (SSN, SFI, K-Index)
    fun saveSSN(context: Context, ssn: Int) = getPrefs(context).edit().putInt("ssn", ssn).apply()
    fun loadSSN(context: Context): Int = getPrefs(context).getInt("ssn", 70)

    fun saveSFI(context: Context, sfi: Int) = getPrefs(context).edit().putInt("sfi", sfi).apply()
    fun loadSFI(context: Context): Int = getPrefs(context).getInt("sfi", 120)

    fun saveKIndex(context: Context, k: Int) = getPrefs(context).edit().putInt("k_index", k).apply()
    fun loadKIndex(context: Context): Int = getPrefs(context).getInt("k_index", 2)

    // Station Data (Power, Antenna, Mode)
    fun savePower(context: Context, power: Int) = getPrefs(context).edit().putInt("power", power).apply()
    fun loadPower(context: Context): Int = getPrefs(context).getInt("power", 100)

    fun saveAntenna(context: Context, antenna: String) = getPrefs(context).edit().putString("antenna", antenna).apply()
    fun loadAntenna(context: Context): String = getPrefs(context).getString("antenna", "Dipole") ?: "Dipole"

    fun saveMode(context: Context, mode: String) = getPrefs(context).edit().putString("mode", mode).apply()
    fun loadMode(context: Context): String = getPrefs(context).getString("mode", "SSB") ?: "SSB"

    // Location Data (Maidenhead Grids)
    fun saveLocations(context: Context, txGrid: String, rxGrid: String) {
        getPrefs(context).edit()
            .putString("tx_grid", txGrid)
            .putString("rx_grid", rxGrid)
            .apply()
    }

    // --- METODELE Lipsă (Necesare pentru apelurile individuale din MainActivity) ---
    fun saveTxGrid(context: Context, grid: String) {
        getPrefs(context).edit().putString("tx_grid", grid).apply()
    }

    fun saveRxGrid(context: Context, grid: String) {
        getPrefs(context).edit().putString("rx_grid", grid).apply()
    }

    fun loadTxGrid(context: Context): String = getPrefs(context).getString("tx_grid", "") ?: ""
    fun loadRxGrid(context: Context): String = getPrefs(context).getString("rx_grid", "") ?: ""

    // --- METODELE de Corecție foF2 ---

    fun saveUseCorrection(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean("use_correction", value).apply()
    }

    fun loadUseCorrection(context: Context): Boolean {
        return getPrefs(context).getBoolean("use_correction", false)
    }

    fun saveAvgFoF2(context: Context, value: Double) {
        getPrefs(context).edit().putFloat("avg_fof2", value.toFloat()).apply()
    }

    fun loadAvgFoF2(context: Context): Double {
        return getPrefs(context).getFloat("avg_fof2", 0.0f).toDouble()
    }

    // --- Theme & Language ---

    fun saveTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString("theme_pref", theme).apply()
    }

    fun loadTheme(context: Context): String {
        return getPrefs(context).getString("theme_pref", "System") ?: "System"
    }

    fun saveLanguage(context: Context, lang: String) {
        getPrefs(context).edit().putString("lang_pref", lang).apply()
    }

    fun loadLanguage(context: Context): String {
        return getPrefs(context).getString("lang_pref", "English") ?: "English"
    }

    // ─── SOLAR DATA PERSISTENCE ──────────────────────────────────────────────────
    // Salveaza timestamp-ul ultimului fetch reusit (epoch ms)
    fun saveSolarLastUpdate(context: Context, timestampMs: Long) {
        getPrefs(context).edit().putLong("solar_last_update", timestampMs).apply()
    }

    fun loadSolarLastUpdate(context: Context): Long {
        return getPrefs(context).getLong("solar_last_update", 0L)
    }

    // Salveaza stationValues ca JSON simplu: "AT138:7.84,SO148:6.20"
    fun saveStationValues(context: Context, values: Map<String, Double>) {
        val str = values.entries.joinToString(",") { "${it.key}:${it.value}" }
        getPrefs(context).edit().putString("station_values", str).apply()
    }

    fun loadStationValues(context: Context): Map<String, Double> {
        val str = getPrefs(context).getString("station_values", "") ?: ""
        if (str.isBlank()) return emptyMap()
        return str.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toDoubleOrNull() ?: return@mapNotNull null)
            else null
        }.toMap()
    }

    // Salveaza stationMuf ca JSON simplu: "AT138:28.6,SO148:22.1"
    fun saveStationMuf(context: Context, values: Map<String, Double>) {
        val str = values.entries.joinToString(",") { "${it.key}:${it.value}" }
        getPrefs(context).edit().putString("station_muf", str).apply()
    }

    fun loadStationMuf(context: Context): Map<String, Double> {
        val str = getPrefs(context).getString("station_muf", "") ?: ""
        if (str.isBlank()) return emptyMap()
        return str.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toDoubleOrNull() ?: return@mapNotNull null)
            else null
        }.toMap()
    }

    // Formateaza timestamp pentru afisare: "10.06.2026 08:39 UTC"
    fun formatSolarUpdateTime(timestampMs: Long): String {
        if (timestampMs == 0L) return "—"
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm 'UTC'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(timestampMs))
    }

    // ─── ONLINE IMAGES CACHE ─────────────────────────────────────────────────────
    private const val ONLINE_CACHE_DIR  = "online_cache"
    private const val PREF_IMG_TIMESTAMP = "online_images_timestamp"

    private val IMAGE_URLS = listOf(
        "solar_vhf"  to "https://www.hamqsl.com/solar101vhfper.php",
        "solar_hf"   to "https://www.hamqsl.com/solar101pic.php",
        "solar_muf"  to "https://www.hamqsl.com/solarmuf.php"
    )

    fun getCacheDir(context: Context): java.io.File {
        return java.io.File(context.filesDir, ONLINE_CACHE_DIR).also { it.mkdirs() }
    }

    fun getCachedImageFile(context: Context, key: String): java.io.File {
        return java.io.File(getCacheDir(context), "$key.png")
    }

    fun hasCachedImages(context: Context): Boolean {
        return IMAGE_URLS.all { (key, _) -> getCachedImageFile(context, key).exists() }
    }

    fun saveOnlineImagesTimestamp(context: Context, timestampMs: Long) {
        getPrefs(context).edit().putLong(PREF_IMG_TIMESTAMP, timestampMs).apply()
    }

    fun loadOnlineImagesTimestamp(context: Context): Long {
        return getPrefs(context).getLong(PREF_IMG_TIMESTAMP, 0L)
    }

    fun formatOnlineUpdateTime(timestampMs: Long): String {
        if (timestampMs == 0L) return "—"
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm 'UTC'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(timestampMs))
    }

    // Descarca si salveaza toate imaginile (rulat pe IO thread)
    // Returneaza true daca cel putin una a fost salvata cu succes
    fun downloadAndCacheImages(context: Context): Boolean {
        var anySuccess = false
        val cacheDir = getCacheDir(context)
        for ((key, url) in IMAGE_URLS) {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout    = 15000
                conn.setRequestProperty("User-Agent", "HFProp/3.0 Android")
                conn.connect()
                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.readBytes()
                    java.io.File(cacheDir, "$key.png").writeBytes(bytes)
                    anySuccess = true
                }
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.w("OnlineCache", "Failed to download $key: ${e.message}")
            }
        }
        if (anySuccess) {
            saveOnlineImagesTimestamp(context, System.currentTimeMillis())
        }
        return anySuccess
    }

    // Genera HTML cu imaginile din cache local (file:// URLs)
    fun buildCachedHtml(context: Context): String {
        val cacheDir = getCacheDir(context)
        val sb = StringBuilder()
        for ((key, _) in IMAGE_URLS) {
            val file = java.io.File(cacheDir, "$key.png")
            if (file.exists()) sb.append("<img src='file://${file.absolutePath}'><br>")
        }
        return "<html><head><meta name='viewport' content='width=device-width,initial-scale=1.0'>"
            .plus("<style>body{margin:0;padding:8px;display:flex;flex-direction:column;")
            .plus("align-items:center;background:transparent;}")
            .plus("img{max-width:100%;height:auto;margin-bottom:15px;}</style></head>")
            .plus("<body>${sb}</body></html>")
    }

    fun buildLiveHtml(): String {
        return """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
                <style>
                    body { margin:0; padding:0; display:flex; flex-direction:column;
                           align-items:center; background-color:transparent; }
                    img  { max-width:100%; height:auto; margin-bottom:15px; }
                </style>
            </head>
            <body>
                <img src="https://www.hamqsl.com/solar101vhfper.php">
                <img src="https://www.hamqsl.com/solar101pic.php">
                <img src="https://www.hamqsl.com/solarmuf.php">
            </body>
            </html>
        """.trimIndent()
    }
}