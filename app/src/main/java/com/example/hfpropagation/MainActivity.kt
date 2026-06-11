package com.example.hfpropagation

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

// --- DATA MODELS ---
data class BandPrediction(val bandName: String, val hourlyProbabilities: List<Int>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_HFPropagation)
        super.onCreate(savedInstanceState)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        setContent {
            val context = LocalContext.current

            // --- THEME AND LANGUAGE LOGIC (v2.4.0) ---
            var themePref by remember { mutableStateOf(StorageUtils.loadTheme(context)) }
            var langPref by remember { mutableStateOf(StorageUtils.loadLanguage(context)) }

            // Select the string dictionary based on saved preference
            val s = when (langPref) {
                "Română" -> RomanianStrings
                "Magyar" -> HungarianStrings
                //"Deutsch" -> GermanStrings
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
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    // New states for foF2 correction
    var useCorrection by remember { mutableStateOf(StorageUtils.loadUseCorrection(context)) }
    var avgFoF2Value by remember { mutableStateOf(StorageUtils.loadAvgFoF2(context)) }

    // --- Solar data persistence ---
    var stationValuesMap by remember { mutableStateOf(StorageUtils.loadStationValues(context)) }
    var stationMufMap    by remember { mutableStateOf(StorageUtils.loadStationMuf(context)) }
    var solarLastUpdate  by remember { mutableStateOf(StorageUtils.loadSolarLastUpdate(context)) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // --- MODIFIED: List of 5 icons to include Online Data ---
    val icons = listOf(
        Icons.Default.Place,
        Icons.Default.Assessment,
        Icons.Default.Public, // For Online Data
        Icons.Default.Settings,
        Icons.AutoMirrored.Filled.Help
    )

    // Solar parameter states
    val ssn = remember { mutableIntStateOf(StorageUtils.loadSSN(context)) }
    val sfi = remember { mutableIntStateOf(StorageUtils.loadSFI(context)) }
    val kIndex = remember { mutableIntStateOf(StorageUtils.loadKIndex(context)) }
    val power = remember { mutableIntStateOf(StorageUtils.loadPower(context)) }
    val antenna = remember { mutableStateOf(StorageUtils.loadAntenna(context)) }
    val mode = remember { mutableStateOf(StorageUtils.loadMode(context)) }

    // --- EFFECTIVE SSN LOGIC ---
    val ssnEffective by remember(ssn.intValue, avgFoF2Value, useCorrection) {
        derivedStateOf {
            if (useCorrection && avgFoF2Value > 0.0) {
                val ssnFromIonosonde = (avgFoF2Value - 2.5) / 0.055
                ((ssn.intValue * 0.4) + (ssnFromIonosonde * 0.6)).toInt().coerceIn(0, 250)
            } else {
                ssn.intValue
            }
        }
    }

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
                    // Create the list of titles including the new Tab
                    val tabTitles = s.tabs.toMutableList().apply {
                        if (size < 5) add(2, if(s.langLabel == "Limba") "Date Online" else if (s.langLabel == "Language") "Online Data" else "Online Informacio")
                    }

                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 10.sp, maxLines = 1) },
                            icon = { Icon(icons[index], contentDescription = null, modifier = Modifier.size(18.dp)) }
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
                    stationValues = stationValuesMap,
                    onTxGridTextChanged = {
                        txGridText = it
                        StorageUtils.saveTxGrid(context, it)
                    },
                    onRxGridTextChanged = {
                        rxGridText = it
                        StorageUtils.saveRxGrid(context, it)
                    },
                    loading = isCalculating,
                    onTxChanged = { txCoords = it },
                    onRxChanged = { rxCoords = it },
                    onRunPrediction = {
                        isCalculating = true
                        val finalSSN = ssnEffective
                        val currentSFI = sfi.intValue
                        val currentK = kIndex.intValue
                        val currentPower = power.intValue
                        val currentMode = mode.value

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                if (!Python.isStarted()) { Python.start(AndroidPlatform(context)) }
                                val py = Python.getInstance()
                                val module = py.getModule("voacap_engine")

                                val p2p = runPrediction(context, txCoords!!, rxCoords!!, finalSSN, currentSFI, currentK, currentPower, currentMode, avgFoF2Value)
                                val footprint = runCoverageCalculation(context, txCoords!!, finalSSN, currentSFI, currentK, currentPower, currentMode)

                                val pyChartRaw = module.callAttr(
                                    "get_muf_luf_data",
                                    txCoords!!.first, txCoords!!.second,
                                    rxCoords!!.first, rxCoords!!.second,
                                    finalSSN, currentSFI, currentK,
                                    avgFoF2Value  // foF2 real GIRO
                                )

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

                // --- NEW: Call to external OnlineDataTab.kt file ---
                2 -> OnlineDataTab(s)

                3 -> SettingsTab(
                    context = context,
                    s = s,
                    selectedLang = currentLang,
                    selectedTheme = currentTheme,
                    onThemeChanged = onThemeChanged,
                    onLangChanged = onLangChanged,
                    ssn = ssn.intValue,
                    sfi = sfi.intValue,
                    kIndex = kIndex.intValue,
                    power = power.intValue,
                    antenna = antenna.value,
                    mode = mode.value,
                    avgFoF2 = avgFoF2Value,
                    ssnEffective = ssnEffective,
                    txLat = txCoords?.first,
                    txLon = txCoords?.second,
                    stationValues = stationValuesMap,
                    stationMuf = stationMufMap,
                    solarLastUpdate = solarLastUpdate,
                    useCorrection = useCorrection,
                    onCorrectionToggled = {
                        useCorrection = it
                        StorageUtils.saveUseCorrection(context, it)
                    },
                    onIonosondeDataReceived = { solarData ->
                        avgFoF2Value = solarData.avgFoF2
                        stationValuesMap = solarData.stationValues
                        stationMufMap    = solarData.stationMuf
                        solarLastUpdate  = System.currentTimeMillis()
                        // Persistenta: salvam toate datele pentru repornire
                        StorageUtils.saveStationValues(context, solarData.stationValues)
                        StorageUtils.saveStationMuf(context, solarData.stationMuf)
                        StorageUtils.saveSolarLastUpdate(context, solarLastUpdate)
                        StorageUtils.saveAvgFoF2(context, solarData.avgFoF2)
                        ssn.intValue = solarData.ssn
                        sfi.intValue = solarData.sfi
                        kIndex.intValue = solarData.kIndex
                        StorageUtils.saveAvgFoF2(context, solarData.avgFoF2)
                    },
                    onSettingsChanged = { ns, nf, nk, np, na, nm ->
                        ssn.intValue = ns; sfi.intValue = nf; kIndex.intValue = nk
                        power.intValue = np; antenna.value = na; mode.value = nm

                        avgFoF2Value = StorageUtils.loadAvgFoF2(context)

                        StorageUtils.saveSSN(context, ns); StorageUtils.saveSFI(context, nf)
                        StorageUtils.saveKIndex(context, nk); StorageUtils.savePower(context, np)
                        StorageUtils.saveAntenna(context, na); StorageUtils.saveMode(context, nm)
                    }
                )
                4 -> HelpTab(s)
            }
            if (isCalculating) LoadingOverlay()
        }
    }
}

// --- HELPER FUNCTIONS ---

suspend fun runPrediction(
    context: Context, tx: Pair<Double, Double>, rx: Pair<Double, Double>,
    ssnParam: Int, sfiParam: Int, kIndexParam: Int, powerParam: Int, modeParam: String,
    avgFoF2Param: Double = 0.0
): List<BandPrediction> = withContext(Dispatchers.IO) {
    val py = Python.getInstance()
    val module = py.getModule("voacap_engine")
    val res = module.callAttr("calculate_propagation", tx.first, tx.second, rx.first, rx.second, ssnParam, sfiParam, kIndexParam, powerParam, modeParam, avgFoF2Param)
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
            when (entry.key) {
                "points", "inner_points" ->
                    entry.value.asList().map { p ->
                        listOf(p.asList()[0].toDouble(), p.asList()[1].toDouble())
                    }
                "is_nvis" ->
                    entry.value.toString().lowercase() == "true"
                else -> entry.value.toString()
            }
        }
    }
}

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

    val isRo = s.langLabel == "Limba"
    val tapText = if(isRo) "Atinge graficul pentru valori exacte" else "Tap graph for exact frequencies"

    var selectedDetails by remember { mutableStateOf(tapText) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = if(isRo) "Analiză MUF / FOT / LUF" else "MUF / FOT / LUF Analysis",
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
                        Text(if(isRo) "Nicio dată. Rulează analiza." else "No chart data. Run analysis first.", color = Color.Gray)
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
            var showSkipZones by remember { mutableStateOf(true) }

            // --- Harta cu Layers FAB overlay ---
            Card(modifier = Modifier.fillMaxWidth().height(460.dp)) {
                val activeLayers = coverageData.filter { visibleBands[it["band"] as String] == true }
                FootprintMapComponent(
                    txLat = txCoords.first,
                    txLon = txCoords.second,
                    activeLayers = activeLayers,
                    showSkipZones = showSkipZones,
                    coverageData = coverageData,
                    visibleBands = visibleBands,
                    showSkipZonesState = showSkipZones,
                    onShowSkipZonesChanged = { showSkipZones = it },
                    onBandToggled = { band, visible -> visibleBands[band] = visible },
                    isRo = isRo
                )
            }


        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun FootprintMapComponent(
    txLat: Double,
    txLon: Double,
    activeLayers: List<Map<String, Any>>,
    showSkipZones: Boolean = true,
    coverageData: List<Map<String, Any>> = emptyList(),
    visibleBands: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean> = androidx.compose.runtime.snapshots.SnapshotStateMap(),
    showSkipZonesState: Boolean = true,
    onShowSkipZonesChanged: (Boolean) -> Unit = {},
    onBandToggled: (String, Boolean) -> Unit = { _, _ -> },
    isRo: Boolean = false
) {
    val context = LocalContext.current
    var layersExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    val colorHex  = layer["color"] as String
                    val ptsRaw    = layer["points"] as? List<List<Double>> ?: return@forEach
                    val innerRaw  = layer["inner_points"] as? List<List<Double>> ?: emptyList()
                    val isNvis    = layer["is_nvis"] as? Boolean ?: false
                    val color     = android.graphics.Color.parseColor(colorHex)

                    if (ptsRaw.isEmpty()) return@forEach

                    // Poligon exterior (raza maxima)
                    val outerPoly = Polygon().apply {
                        fillPaint.color = color
                        fillPaint.alpha = if (isNvis) 70 else 45
                        outlinePaint.color = color
                        outlinePaint.strokeWidth = 2f
                        outlinePaint.alpha = 200
                        setPoints(ptsRaw.map { GeoPoint(it[0], it[1]) })
                    }
                    mapView.overlays.add(outerPoly)

                    // Skip zone (zona moarta inelara) - hasurat cu negru semi-transparent
                    if (showSkipZones && innerRaw.size >= 3) {
                        // Shader de hasura diagonala
                        val hatchBitmap = android.graphics.Bitmap.createBitmap(16, 16,
                            android.graphics.Bitmap.Config.ARGB_8888)
                        val hatchCanvas = android.graphics.Canvas(hatchBitmap)
                        val hPaint = android.graphics.Paint().apply {
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 1.5f
                            setColor(android.graphics.Color.argb(60, 0, 0, 0))
                        }
                        hatchCanvas.drawLine(0f, 16f, 16f, 0f, hPaint)
                        hatchCanvas.drawLine(-8f, 16f, 8f, 0f, hPaint)
                        hatchCanvas.drawLine(8f, 16f, 24f, 0f, hPaint)

                        val skipPoly = Polygon().apply {
                            // Umplere haturata
                            fillPaint.shader = android.graphics.BitmapShader(
                                hatchBitmap,
                                android.graphics.Shader.TileMode.REPEAT,
                                android.graphics.Shader.TileMode.REPEAT
                            )
                            fillPaint.alpha = 180
                            // Contur punctat in culoarea benzii
                            outlinePaint.color = color
                            outlinePaint.strokeWidth = 2f
                            outlinePaint.alpha = 180
                            outlinePaint.pathEffect = android.graphics.DashPathEffect(
                                floatArrayOf(10f, 6f), 0f)
                            setPoints(innerRaw.map { GeoPoint(it[0], it[1]) })
                        }
                        mapView.overlays.add(skipPoly)
                    }
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- LAYERS FAB OVERLAY (top-right, ca in AVRT9) ---
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            SmallFloatingActionButton(
                onClick = { layersExpanded = true },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Layers", modifier = Modifier.size(20.dp))
            }

            DropdownMenu(
                expanded = layersExpanded,
                onDismissRequest = { layersExpanded = false }
            ) {
                // Titlu benzi
                Text(
                    text = if (isRo) "Benzi" else "Bands",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                // Checkbox per banda
                coverageData.forEach { layer ->
                    val band     = (layer["band"] as? String) ?: return@forEach
                    val colorHex = (layer["color"] as? String) ?: "#FFFFFF"
                    val isVisible = visibleBands[band] ?: false
                    DropdownMenuItem(
                        text = { Text(band, fontSize = 13.sp) },
                        onClick = { onBandToggled(band, !isVisible) },
                        leadingIcon = {
                            Box(modifier = Modifier
                                .size(12.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor(colorHex)),
                                    shape = RoundedCornerShape(2.dp)
                                )
                            )
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = isVisible,
                                onCheckedChange = { onBandToggled(band, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(android.graphics.Color.parseColor(colorHex))
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Toggle skip zones
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isRo) "Zone moarte (skip)" else "Skip zones",
                            fontSize = 13.sp
                        )
                    },
                    onClick = { onShowSkipZonesChanged(!showSkipZonesState) },
                    trailingIcon = {
                        Checkbox(
                            checked = showSkipZonesState,
                            onCheckedChange = { onShowSkipZonesChanged(it) },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Legenda
                Text(
                    text = if (isRo) "Legenda" else "Legend",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                // Zona acoperire
                DropdownMenuItem(
                    text = { Text(if (isRo) "Zona acoperire" else "Coverage area", fontSize = 12.sp) },
                    onClick = {},
                    leadingIcon = {
                        Box(modifier = Modifier.size(12.dp, 10.dp)
                            .background(Color(0xFF43A047).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(2.dp)))
                    }, enabled = false
                )
                // NVIS
                DropdownMenuItem(
                    text = { Text("NVIS (80m/40m)", fontSize = 12.sp) },
                    onClick = {},
                    leadingIcon = {
                        Box(modifier = Modifier.size(12.dp, 10.dp)
                            .background(Color(0xFFB71C1C).copy(alpha = 0.65f),
                                shape = RoundedCornerShape(2.dp)))
                    }, enabled = false
                )
                // Skip zone
                DropdownMenuItem(
                    text = { Text(if (isRo) "Zona moarta (hatur)" else "Skip zone (hatched)", fontSize = 12.sp) },
                    onClick = {},
                    leadingIcon = {
                        Box(modifier = Modifier.size(12.dp, 10.dp)
                            .background(Color.Black.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF43A047).copy(alpha = 0.7f),
                                shape = RoundedCornerShape(2.dp)))
                    }, enabled = false
                )
                // Banda inchisa
                DropdownMenuItem(
                    text = { Text(if (isRo) "Banda inchisa" else "Closed band", fontSize = 12.sp) },
                    onClick = {},
                    leadingIcon = {
                        Box(modifier = Modifier.size(12.dp, 10.dp)
                            .background(Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)))
                    }, enabled = false
                )
            }
        }
    } // end Box
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