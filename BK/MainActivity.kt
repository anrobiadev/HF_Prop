package com.example.hfpropagation

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.hfpropagation.ui.theme.HFPropagationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.hfpropagation.BuildConfig

// --- DATA MODELS ---
data class BandPrediction(val bandName: String, val hourlyProbabilities: List<Int>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_HFPropagation)
        super.onCreate(savedInstanceState)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        setContent {
            val context = LocalContext.current

            // --- LOGICA DE TEM? ?I LIMB? (v2.4.0) ---
            var themePref by remember { mutableStateOf(StorageUtils.loadTheme(context)) }
            var langPref by remember { mutableStateOf(StorageUtils.loadLanguage(context)) }

            // Alegem dic?ionarul de string-uri pe baza preferin?ei salvate
           // val s = if (langPref == "Român?") RomanianStrings else EnglishStrings
            val s = when (langPref) {
                "Român?" -> RomanianStrings
                "Magyar" -> HungarianStrings
                else -> EnglishStrings
            }
            val darkTheme = when (themePref) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            HFPropagationTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DroidPropMainScreen(
                        context = context,
                        s = s,
                        currentTheme = themePref,
                        currentLang = langPref,
                        onThemeChanged = { newTheme ->
                            themePref = newTheme
                            StorageUtils.saveTheme(context, newTheme)
                        },
                        onLangChanged = { newLang ->
                            langPref = newLang
                            StorageUtils.saveLanguage(context, newLang)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DroidPropMainScreen(
    context: Context,
    s: AppStrings,
    currentTheme: String,
    currentLang: String,
    onThemeChanged: (String) -> Unit,
    onLangChanged: (String) -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3500)
        showSplash = false
    }

    if (showSplash) {
        StartupSplashScreen()
    } else {
        MainAppContent(context, s, currentTheme, currentLang, onThemeChanged, onLangChanged)
    }
}

@Composable
fun StartupSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)), label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(250.dp)) {
            drawCircle(color = Color(0xFF003300), radius = size.minDimension / 2, style = Stroke(width = 2f))
            drawCircle(color = Color(0xFF003300), radius = size.minDimension / 4, style = Stroke(width = 2f))
            rotate(rotation) {
                drawLine(color = Color(0xFF00FF41), start = center, end = center.copy(y = 0f), strokeWidth = 6f)
            }
        }
        Text("SCANNING HF BANDS...", color = Color(0xFF00FF41), modifier = Modifier.padding(top = 340.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    context: Context,
    s: AppStrings,
    currentTheme: String,
    currentLang: String,
    onThemeChanged: (String) -> Unit,
    onLangChanged: (String) -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val icons = listOf(Icons.Default.Place, Icons.Default.Assessment, Icons.Default.Settings,
        Icons.AutoMirrored.Filled.Help
    )

    // St?ri parametri solari
    val ssn = remember { mutableIntStateOf(StorageUtils.loadSSN(context)) }
    val sfi = remember { mutableIntStateOf(StorageUtils.loadSFI(context)) }
    val kIndex = remember { mutableIntStateOf(StorageUtils.loadKIndex(context)) }
    val power = remember { mutableIntStateOf(StorageUtils.loadPower(context)) }
    val antenna = remember { mutableStateOf(StorageUtils.loadAntenna(context)) }
    val mode = remember { mutableStateOf(StorageUtils.loadMode(context)) }

    var txGridText by rememberSaveable { mutableStateOf(StorageUtils.loadTxGrid(context) ?: "") }
    var rxGridText by rememberSaveable { mutableStateOf(StorageUtils.loadRxGrid(context) ?: "") }
    var txCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var rxCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var predictionResults by remember { mutableStateOf<List<BandPrediction>?>(null) }
    var coverageData by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var chartData by remember { mutableStateOf<Map<String, List<Double>>?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                            Text(s.appTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("v$versionName", fontSize = 11.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    s.tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 12.sp) },
                            icon = { Icon(icons[index], contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> LocationTab(
                    s = s,
                    tx = txCoords, rx = rxCoords,
                    txGridText = txGridText,
                    rxGridText = rxGridText,
                    onTxGridTextChanged = { txGridText = it },
                    onRxGridTextChanged = { rxGridText = it },
                    loading = isCalculating,
                    onTxChanged = { txCoords = it },
                    onRxChanged = { rxCoords = it },
                    onRunPrediction = {
                        isCalculating = true
                        val currentSSN = ssn.intValue
                        val currentSFI = sfi.intValue
                        val currentK = kIndex.intValue
                        val currentPower = power.intValue
                        val currentMode = mode.value

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                if (!Python.isStarted()) { Python.start(AndroidPlatform(context)) }
                                val py = Python.getInstance()
                                val module = py.getModule("voacap_engine")

                                val p2p = runPrediction(context, txCoords!!, rxCoords!!, currentSSN, currentSFI, currentK, currentPower, currentMode)
                                val footprint = runCoverageCalculation(context, txCoords!!, currentSSN, currentSFI, currentK, currentPower, currentMode)

                                val pyChartRaw = module.callAttr("get_muf_luf_data", txCoords!!.first, txCoords!!.second, rxCoords!!.first, rxCoords!!.second, currentSSN, currentSFI, currentK)

                                val muf = pyChartRaw.callAttr("get", "muf")?.asList()?.map { it.toDouble() } ?: emptyList()
                                val luf = pyChartRaw.callAttr("get", "luf")?.asList()?.map { it.toDouble() } ?: emptyList()
                                val fot = pyChartRaw.callAttr("get", "fot")?.asList()?.map { it.toDouble() } ?: emptyList()

                                withContext(Dispatchers.Main) {
                                    chartData = mapOf("muf" to muf, "luf" to luf, "fot" to fot)
                                    predictionResults = p2p
                                    coverageData = footprint
                                    isCalculating = false
                                    selectedTabIndex = 1
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isCalculating = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                1 -> PropagationInfoTab(s, predictionResults, txCoords, coverageData, chartData)
                2 -> SettingsTab(
                    context = context,
                    s = s,
                    ssn = ssn.intValue,
                    sfi = sfi.intValue,
                    kIndex = kIndex.intValue,
                    power = power.intValue,
                    antenna = antenna.value,
                    mode = mode.value,
                    selectedTheme = currentTheme,
                    selectedLang = currentLang,
                    onThemeChanged = onThemeChanged,
                    onLangChanged = onLangChanged,
                    onSettingsChanged = { ns, nf, nk, np, na, nm ->
                        ssn.intValue = ns; sfi.intValue = nf; kIndex.intValue = nk
                        power.intValue = np; antenna.value = na; mode.value = nm
                        StorageUtils.saveSSN(context, ns); StorageUtils.saveSFI(context, nf)
                        StorageUtils.saveKIndex(context, nk); StorageUtils.savePower(context, np)
                        StorageUtils.saveAntenna(context, na); StorageUtils.saveMode(context, nm)
                    }
                )
                3 -> HelpTab(s)
            }
            if (isCalculating) LoadingOverlay()
        }
    }
}

// --- PYTHON BRIDGES ---

suspend fun runPrediction(
    context: Context, tx: Pair<Double, Double>, rx: Pair<Double, Double>,
    ssnParam: Int, sfiParam: Int, kIndexParam: Int, powerParam: Int, modeParam: String
): List<BandPrediction> = withContext(Dispatchers.IO) {
    val py = Python.getInstance()
    val module = py.getModule("voacap_engine")
    val res = module.callAttr("calculate_propagation", tx.first, tx.second, rx.first, rx.second, ssnParam, sfiParam, kIndexParam, powerParam, modeParam)
    res.asList().map { pyItem ->
        @Suppress("UNCHECKED_CAST")
        val pyMap = pyItem.asMap() as Map<String, PyObject>
        val bandName = pyMap["band"]?.toString() ?: "??"
        val probsList = pyMap["probs"]?.asList()?.map { it.toInt() } ?: List(24) { 0 }
        BandPrediction(bandName, probsList)
    }
}

suspend fun runCoverageCalculation(
    context: Context, tx: Pair<Double, Double>,
    ssnParam: Int, sfiParam: Int, kIndexParam: Int, powerParam: Int, modeParam: String
): List<Map<String, Any>> = withContext(Dispatchers.IO) {
    val py = Python.getInstance()
    val module = py.getModule("coverage_engine")
    val res = module.callAttr("get_area_coverage", tx.first, tx.second, ssnParam, sfiParam, kIndexParam, powerParam, modeParam)
    res.asList().map { pyItem ->
        pyItem.asMap().mapKeys { it.key.toString() }.mapValues { entry ->
            if (entry.key == "points") {
                entry.value.asList().map { p -> listOf(p.asList()[0].toDouble(), p.asList()[1].toDouble()) }
            } else entry.value.toString()
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun PropagationInfoTab(
    s: AppStrings,
    results: List<BandPrediction>?, txCoords: Pair<Double, Double>?,
    coverageData: List<Map<String, Any>>?, chartData: Map<String, List<Double>>?
) {
    val visibleBands = remember {
        mutableStateMapOf<String, Boolean>().apply {
            listOf("80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m").forEach { put(it, true) }
        }
    }

    // Logic? pentru traducere local? în tab-ul de rezultate
    val isRo = s.langLabel == "Limba"
    val tapText = if(isRo) "Atinge graficul pentru valori exacte" else "Tap graph for exact frequencies"

    var selectedDetails by remember { mutableStateOf(tapText) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = if(isRo) "Analiz? MUF / FOT / LUF" else "MUF / FOT / LUF Analysis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (chartData != null) {
                    PropagationChart(
                        mufData = chartData["muf"]!!, lufData = chartData["luf"]!!, fotData = chartData["fot"]!!,
                        onPointSelected = { hour, muf, luf, fot ->
                            selectedDetails = "UTC: %02d:00  |  MUF: %.1f  |  FOT: %.1f  |  LUF: %.1f".format(hour, muf, fot, luf)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.1f)).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(text = selectedDetails, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(if(isRo) "Nicio dat?. Ruleaz? analiza." else "No chart data. Run analysis first.", color = Color.Gray)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if(isRo) "Fiabilitate Benzi 24h" else "24h Band Reliability",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (results != null) {
            Card(modifier = Modifier.padding(vertical = 12.dp)) { Box(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) { VoacapHeatmap(results) } }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if(isRo) "Harta Amprentei Semnalului" else "Signal Footprint Map",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (txCoords != null && coverageData != null) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                coverageData.forEach { layer ->
                    val band = (layer["band"] as? String) ?: ""
                    val colorHex = (layer["color"] as? String) ?: "#FFFFFF"
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Checkbox(checked = visibleBands[band] ?: false, onCheckedChange = { visibleBands[band] = it }, colors = CheckboxDefaults.colors(checkedColor = Color(android.graphics.Color.parseColor(colorHex))))
                        Text(band, fontSize = 12.sp)
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                val activeLayers = coverageData.filter { visibleBands[it["band"] as String] == true }
                FootprintMapComponent(txCoords.first, txCoords.second, activeLayers)
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun FootprintMapComponent(txLat: Double, txLon: Double, activeLayers: List<Map<String, Any>>) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(4.0)
                controller.setCenter(GeoPoint(txLat, txLon))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            val marker = Marker(mapView).apply {
                position = GeoPoint(txLat, txLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
            }
            mapView.overlays.add(marker)
            activeLayers.forEach { layer ->
                val colorHex = layer["color"] as String
                val ptsRaw = layer["points"] as List<List<Double>>
                val poly = Polygon().apply {
                    fillPaint.color = android.graphics.Color.parseColor(colorHex)
                    fillPaint.alpha = 50
                    outlinePaint.color = android.graphics.Color.parseColor(colorHex)
                    outlinePaint.strokeWidth = 2f
                    setPoints(ptsRaw.map { GeoPoint(it[0], it[1]) })
                }
                mapView.overlays.add(poly)
            }
            mapView.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LoadingOverlay() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        Card {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Analyzing Ionosphere...")
            }
        }
    }
}