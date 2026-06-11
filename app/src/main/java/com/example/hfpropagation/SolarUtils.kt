package com.example.hfpropagation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.math.roundToInt

// --- DATA MODEL ---
data class SolarData(
    val ssn: Int,
    val sfi: Int,
    val kIndex: Int,
    val avgFoF2: Double = 0.0,
    val ssnEffective: Int = 0,
    val stationValues: Map<String, Double> = emptyMap(),
    val stationM3000: Map<String, Double> = emptyMap(),  // M(D) factor per statie
    val stationMuf:   Map<String, Double> = emptyMap()   // MUF(3000) calculat direct
)

object SolarUtils {
    private const val SIDC_SSN_URL = "https://www.sidc.be/SILSO/FORECASTS/KFprediML.txt"
    private const val DRAO_SFI_URL = "https://spaceweather.gc.ca/solar_flux_data/daily_flux_values/fluxtable.txt"
    private const val NOAA_K_INDEX_URL = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"

    // GIRO DIDBase - NEW API (confirmed by Ivan Galkin/LGDC, June 2026)
    // Old /common/DIDBGetValues is RETIRED - replaced by /fastchar/getbest
    // Direct access works without whitelist - no proxy needed!
    // Per LGDC request: always use fromDate/toDate to reduce server load
    // Format: ?ursiCode=AT138&charName=foF2,hmF2&fromDate=2026.06.09T18:00&toDate=2026.06.10T08:00
    private const val GIRO_FASTCHAR_URL = "https://lgdc.uml.edu/fastchar/getbest"

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(net) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchSolarData(
        context: Context,
        s: AppStrings,
        currentSsn: Int,
        txLat: Double?,
        txLon: Double?
    ): SolarData? = withContext(Dispatchers.IO) {
        if (!isOnline(context)) {
            if (currentSsn <= 0) {
                showToast(context, s.warningDefaultData)
                return@withContext SolarData(123, 123, 2, 0.0, 123)
            } else {
                showToast(context, s.errorNoInternet)
                return@withContext null
            }
        }

        try {
            showToast(context, "📡 V2.4: Syncing Ionospheric Data...")

            val stationsToQuery = if (txLat != null && txLon != null) {
                getClosestGlobalStations(txLat, txLon)
            } else {
                showToast(context, s.infoGenericData)
                // Fallback stations when no TX location is set.
                // AT138 (Athens) is confirmed active on GIRO primary endpoint.
                listOf("AT138", "BC840", "TO535")
            }

            val deferredSfiK = async { fetchNoaaData() }
            val deferredSsn = async { fetchSmoothedSsn() }
            val deferredStationData = async { fetchStationData(stationsToQuery) }

            val (sfi, kIndex) = deferredSfiK.await()
            val smoothedSsn = deferredSsn.await() ?: 111.1
            val stationDataResult = deferredStationData.await()
            val stationMap    = stationDataResult.mapValues { it.value.foF2 }
            val stationM3000Map = stationDataResult.mapValues { it.value.m3000 }
            val stationMufMap   = stationDataResult.mapValues { it.value.muf3000 }

            val avgFoF2 = if (stationMap.isNotEmpty()) stationMap.values.average() else 0.0
            val resultSsn = smoothedSsn.coerceIn(0.0, 250.0).roundToInt()

            val ssnEffective = if (avgFoF2 > 0) {
                ((avgFoF2 - 2.2) * 33.0).coerceIn(0.0, 250.0).roundToInt()
            } else {
                resultSsn
            }

            StorageUtils.saveSSN(context, resultSsn)
            StorageUtils.saveSFI(context, sfi)
            StorageUtils.saveKIndex(context, kIndex)
            StorageUtils.saveAvgFoF2(context, avgFoF2)

            showToast(context, "✅ Sync: SSN=$resultSsn, SFI=$sfi, K=$kIndex, -> SSNe=$ssnEffective")

            return@withContext SolarData(resultSsn, sfi, kIndex, avgFoF2, ssnEffective, stationMap, stationM3000Map, stationMufMap)

        } catch (e: Exception) {
            Log.e("VOACAP_SOLAR", "Fetch error: ${e.message}")
            showToast(context, s.errorFetchFailed)
            return@withContext null
        }
    }

    private fun fetchSmoothedSsn(): Double? {
        return try {
            val data = fetchWithAgent(SIDC_SSN_URL)
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH) + 1

            data.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith(":")) return@forEach

                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val pYear = parts[0].toIntOrNull()
                    val pMonth = parts[1].toIntOrNull()

                    if (pYear == year && pMonth == month) {
                        return parts.getOrNull(4)?.toDoubleOrNull() ?: parts.getOrNull(3)?.toDoubleOrNull()
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("SIDC", "Parse error: ${e.message}")
            null
        }
    }

    // Returneaza map cu foF2, muF si m3000 per statie
    data class StationData(val foF2: Double, val muf3000: Double, val m3000: Double)

    private fun fetchStationFoF2Values(stationCodes: List<String>): Map<String, Double> {
        // Wrapper pentru compatibilitate - returneaza foF2
        return fetchStationData(stationCodes).mapValues { it.value.foF2 }
    }

    fun fetchStationData(stationCodes: List<String>): Map<String, StationData> {
        val results = mutableMapOf<String, StationData>()

        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val now = java.util.Date()
        val sixHoursAgo = java.util.Date(System.currentTimeMillis() - 21600000L)
        // Format confirmat: 2026%2F06%2F10+05%3A00%3A00
        val fromDateStr = sdf.format(sixHoursAgo).replace(" ", "+").replace("/", "%2F").replace(":", "%3A")
        val toDateStr   = sdf.format(now).replace(" ", "+").replace("/", "%2F").replace(":", "%3A")

        val dataLine = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")

        stationCodes.forEach { code ->
            var lastFoF2  = -1.0
            var lastMuf   = -1.0
            var lastTime  = ""

            // Request 1: foF2
            try {
                val url = "$GIRO_FASTCHAR_URL?ursiCode=$code&charName=foF2&DMUF=3000&fromDate=$fromDateStr&toDate=$toDateStr"
                Log.d("GIRO", "foF2: $url")
                val resp = fetchWithAgent(url)
                resp.lines().forEach { line ->
                    val t = line.trim()
                    if (!t.startsWith("#") && dataLine.containsMatchIn(t)) {
                        val p = t.split(Regex("""\s+""")).filter { it.isNotBlank() }
                        if (p.size >= 3) {
                            val cs   = p[1].toIntOrNull() ?: 0
                            val fof2 = p[2].toDoubleOrNull()
                            if (fof2 != null && fof2 in 4.0..20.0 && (cs >= 40 || cs == 999)) {
                                lastFoF2 = fof2; lastTime = p[0]
                            }
                        }
                    }
                }
                Log.d("GIRO", "foF2 $code: $lastFoF2 MHz @ $lastTime")
            } catch (e: Exception) {
                Log.e("GIRO", "foF2 error $code: ${e.message}")
                lastFoF2 = -2.0  // -2 = station unavailable (fetch failed)
            }

            // Delay 2 secunde intre request-uri (cerinta LGDC)
            Thread.sleep(2000L)

            // Request 2: MUF(D) direct - charName=MUF%28D%29
            try {
                val url = "$GIRO_FASTCHAR_URL?ursiCode=$code&charName=MUF%28D%29&DMUF=3000&fromDate=$fromDateStr&toDate=$toDateStr"
                Log.d("GIRO", "MUF(D): $url")
                val resp = fetchWithAgent(url)
                resp.lines().forEach { line ->
                    val t = line.trim()
                    if (!t.startsWith("#") && dataLine.containsMatchIn(t)) {
                        val p = t.split(Regex("""\s+""")).filter { it.isNotBlank() }
                        if (p.size >= 3) {
                            val cs  = p[1].toIntOrNull() ?: 0
                            val muf = p[2].toDoubleOrNull()
                            if (muf != null && muf in 5.0..50.0 && (cs >= 40 || cs == 999)) {
                                lastMuf = muf
                            }
                        }
                    }
                }
                Log.d("GIRO", "MUF $code: $lastMuf MHz")
            } catch (e: Exception) {
                Log.e("GIRO", "MUF error $code: ${e.message}")
            }

            // Delay 2 secunde inainte de urmatoarea statie
            Thread.sleep(2000L)

            if (lastFoF2 == -2.0) {
                // Station unavailable - mark with sentinel value
                results[code] = StationData(-1.0, -1.0, -1.0)
                Log.w("GIRO", "Station $code marked as unavailable")
            } else if (lastFoF2 > 0) {
                val mufUsed   = if (lastMuf > 0) lastMuf else lastFoF2 * 3.2
                val m3000Used = if (lastFoF2 > 0 && lastMuf > 0) lastMuf / lastFoF2 else 3.2
                results[code] = StationData(lastFoF2, mufUsed, m3000Used)
                Log.d("GIRO", "Station $code: foF2=$lastFoF2 MUF=$mufUsed M3000=${String.format("%.2f", m3000Used)}")
            } else {
                Log.w("GIRO", "Station $code: no valid data")
            }
        }
        return results
    }

    private fun fetchNoaaData(): Pair<Int, Int> {
        var sfi = 140
        var kIndex = 2

        // 1. SFI extraction from DRAO Penticton
        try {
            val draoResp = fetchWithAgent(DRAO_SFI_URL)
            val lines = draoResp.lines().filter { it.isNotBlank() && !it.startsWith("#") }
            if (lines.isNotEmpty()) {
                val lastLine = lines.last().trim()
                val parts = lastLine.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    // parts[3] is "Flux Adjusted"
                    val sfiAdjusted = parts[5].toDoubleOrNull()
                    if (sfiAdjusted != null) {
                        sfi = sfiAdjusted.roundToInt()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DRAO", "SFI Parse error: ${e.message}")
        }

        // 2. K-Index extraction from NOAA
        try {
            val kResp = fetchWithAgent(NOAA_K_INDEX_URL)
            val kArr = JSONArray(kResp)
            val latestKStr = kArr.getJSONArray(kArr.length() - 1).getString(1)
            kIndex = latestKStr.toDoubleOrNull()?.roundToInt() ?: 2
        } catch (e: Exception) {
            Log.e("NOAA_K", "K-Index Parse error: ${e.message}")
        }

        return Pair(sfi, kIndex)
    }

    private fun fetchWithAgent(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 HFProp/1.0")
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true
        return try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                // Read error body for diagnostic, then throw with status code (not URL)
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Exception("HTTP $code${if (body.isNotBlank()) ": ${body.take(120)}" else ""}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    data class IonosondeStation(val code: String, val name: String, val lat: Double, val lon: Double)

    // Station list for the GIRO FastChar API (lgdc.uml.edu/fastchar/getbest).
    // IMPORTANT: fromDate/toDate parameters no longer work on this API (return 404).
    // Tested with new API: ?ursiCode=CODE&charName=foF2,hmF2&fromDate=...&toDate=...
    // are marked with active=true. Others are kept for distance calculations but
    // will silently return no data (logged as WARN in Logcat).
    val globalStationDatabase = listOf(
        // --- EUROPE (confirmed active on GIRO primary) ---
        IonosondeStation("AT138", "Athens, Greece",          38.01,  23.53),
        IonosondeStation("SO148", "Sopron, Hungary",         47.63,  16.72),
        IonosondeStation("JR055", "Juliusruh, Germany",      54.63,  13.41),
        IonosondeStation("DB049", "Dourbes, Belgium",        50.10,   4.60),
        IonosondeStation("PQ052", "Pruhonice, Czech Rep.",   50.00,  14.60),
        IonosondeStation("RL052", "Chilton, UK",             51.50,  -0.60),
        IonosondeStation("EB040", "Roquetes, Spain",         40.80,   0.50),
        IonosondeStation("WM918", "Warsaw, Poland",          52.21,  21.12),
        IonosondeStation("RO041", "Rome, Italy",             41.90,  12.50),
        IonosondeStation("FF051", "Fairford, UK",            51.68,  -1.79),
        // --- NORTH AMERICA ---
        IonosondeStation("BC840", "Boulder, CO, USA",        40.00, -105.27),
        IonosondeStation("WP937", "Wallops Is, VA, USA",     37.90,  -75.50),
        IonosondeStation("AU930", "Austin, TX, USA",         30.40,  -97.70),
        IonosondeStation("GA749", "Gander, Canada",          48.90,  -54.50),
        IonosondeStation("AL945", "Alpena, MI, USA",         45.07,  -83.56),
        IonosondeStation("PA836", "Pt. Arguello, CA, USA",   35.60, -120.60),
        // --- ASIA / PACIFIC ---
        IonosondeStation("TO535", "Tokyo, Japan",            35.70,  139.50),
        IonosondeStation("KT52P", "Kokubunji, Japan",        35.71,  139.49),
        IonosondeStation("JJ433", "Jeju, South Korea",       33.40,  126.30),
        IonosondeStation("CMH1Q", "Chiang Mai, Thailand",    18.76,   98.93),
        IonosondeStation("DA412", "Darwin, Australia",      -12.45,  130.83),
        IonosondeStation("CB435", "Canberra, Australia",    -35.32,  149.00),
        // --- SOUTH AMERICA ---
        IonosondeStation("FZ03L", "Fortaleza, Brazil",       -3.80,  -38.40),
        IonosondeStation("SJ02J", "Sao Jose Campos, Brazil",-23.20,  -45.90),
        IonosondeStation("TUM1J", "Tucuman, Argentina",     -26.90,  -65.40),
        IonosondeStation("SMJ2L", "Santa Maria, Brazil",    -29.70,  -53.80),
        // --- AFRICA ---
        IonosondeStation("HM034", "Hermanus, South Africa", -34.40,   19.20),
        IonosondeStation("GR13L", "Grahamstown, S. Africa", -33.30,   26.50),
        IonosondeStation("LT023", "Louis Trichardt, S.Afr.",-23.10,   29.70),
        // --- POLAR ---
        IonosondeStation("MC478", "McMurdo, Antarctica",    -77.85,  166.66),
        IonosondeStation("AS00Q", "Ascension Island",        -7.95,  -14.40),
        IonosondeStation("Troll", "Troll St., Antarctica",  -72.01,    2.53)
    )

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Returns the codes of the N closest ionosonde stations to the TX location.
     * We always return a fixed count (default 3) regardless of distance,
     * because the old +100km filter excluded all stations for many European locations
     * (e.g. the closest station to Orsova is Sopron ~350km, Athens ~700km,
     * but Athens is often the only reliably active GIRO station in the region).
     */
    fun getClosestGlobalStations(txLat: Double, txLon: Double, count: Int = 3): List<String> {
        if (globalStationDatabase.isEmpty()) return emptyList()

        return globalStationDatabase
            .map { it to calculateDistance(txLat, txLon, it.lat, it.lon) }
            .sortedBy { it.second }
            .take(count)
            .map { it.first.code }
    }
}