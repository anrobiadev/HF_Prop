package com.example.hfpropagation

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.compose.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.inset

// --- DATA MODEL ---
data class BandPrediction(val bandName: String, val hourlyProbabilities: List<Int>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // MUST call this BEFORE super.onCreate to swap the black theme
        // to your actual app colors.
        setTheme(R.style.Theme_HFPropagation)

        super.onCreate(savedInstanceState)

        // Ensure osmdroid works
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        setContent {
            HFPropagationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DroidPropMainScreen(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidPropMainScreen(context: Context) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5000) // Stay on black splash for 3.5 seconds
        showSplash = false
    }

    if (showSplash) {
        StartupSplashScreen()
    } else {
        MainAppContent(context)
    }
}

@Composable
fun StartupSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Rotation for the radar sweep (one full turn every 2 seconds)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Radar Canvas
        Canvas(modifier = Modifier.size(250.dp)) {
            val centerOffset = center

            // 1. Draw Static Background Circles (The 'Graticule')
            drawCircle(
                color = Color(0xFF003300),
                radius = size.minDimension / 2,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color(0xFF003300),
                radius = size.minDimension / 4,
                style = Stroke(width = 2f)
            )

            // 2. Draw the Sweeping Line
            rotate(rotation) {
                drawLine(
                    color = Color(0xFF00FF41), // Matrix Green
                    start = centerOffset,
                    end = centerOffset.copy(y = 0f),
                    strokeWidth = 6f
                )
            }

            // 3. Optional: Subtle Green Glow center
            drawCircle(
                color = Color(0xFF00FF41).copy(alpha = 0.2f),
                radius = 10f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 340.dp)
        ) {
            Text(
                "SCANNING HF BANDS...",
                color = Color(0xFF00FF41),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Text(
                "VOACAP ENGINE INITIALIZING",
                color = Color.DarkGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(context: Context) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Location", "Results", "Settings")
    val icons = listOf(Icons.Default.Place, Icons.Default.Assessment, Icons.Default.Settings)

    // Load Persistent Settings
    val savedTxGrid = remember { StorageUtils.loadTxGrid(context) }
    val savedRxGrid = remember { StorageUtils.loadRxGrid(context) }
    var currentSSN by remember { mutableIntStateOf(StorageUtils.loadSSN(context)) }
    var currentPower by remember { mutableIntStateOf(StorageUtils.loadPower(context)) }
    var currentAntenna by remember { mutableStateOf(StorageUtils.loadAntenna(context)) }
    var currentMode by remember { mutableStateOf(StorageUtils.loadMode(context)) }

    // Data States
    var txCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var rxCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var predictionResults by remember { mutableStateOf<List<BandPrediction>?>(null) }
    var coverageData by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("HF PROPAGATION", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 11.sp) },
                            icon = { Icon(icons[index], contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> LocationTab(
                    tx = txCoords, rx = rxCoords,
                    initialTxGrid = savedTxGrid, initialRxGrid = savedRxGrid,
                    loading = isCalculating,
                    onTxChanged = { txCoords = it },
                    onRxChanged = { rxCoords = it },
                    onRunPrediction = {
                        val txG = txCoords?.let { MaidenheadUtils.latLonToGrid(it.first, it.second) } ?: ""
                        val rxG = rxCoords?.let { MaidenheadUtils.latLonToGrid(it.first, it.second) } ?: ""
                        StorageUtils.saveLocations(context, txG, rxG)

                        isCalculating = true
                        coroutineScope.launch {
                            try {
                                // Run both Python engines in parallel
                                val p2p = runPrediction(context, txCoords!!, rxCoords!!, currentSSN, currentPower, currentMode)
                                val footprint = runCoverageCalculation(context, txCoords!!, currentSSN, currentPower, currentMode)

                                predictionResults = p2p
                                coverageData = footprint
                                isCalculating = false
                                selectedTabIndex = 1
                            } catch (e: Exception) {
                                isCalculating = false
                                Toast.makeText(context, "Engine Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                1 -> PropagationInfoTab(predictionResults, txCoords, coverageData)
                2 -> SettingsTab(
                    context = context, ssn = currentSSN, power = currentPower,
                    antenna = currentAntenna, mode = currentMode,
                    onSettingsChanged = { s, p, a, m ->
                        currentSSN = s; currentPower = p; currentAntenna = a; currentMode = m
                    }
                )
            }

            if (isCalculating) LoadingOverlay()
        }
    }
}

// --- TAB 2 UI COMPONENTS ---

@Composable
fun PropagationInfoTab(
    results: List<BandPrediction>?,
    txCoords: Pair<Double, Double>?,
    coverageData: List<Map<String, Any>>?
) {
    val visibleBands = remember {
        mutableStateMapOf<String, Boolean>().apply {
            listOf("80m", "40m", "20m", "15m", "10m").forEach { put(it, true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("24h Band Reliability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (results != null) {
            Card(modifier = Modifier.padding(vertical = 12.dp)) {
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    VoacapHeatmap(results)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Signal Footprint Map", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (txCoords != null && coverageData != null) {
            // Band Selection Checklist
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                coverageData.forEach { layer ->
                    val band = layer["band"] as String
                    val colorHex = layer["color"] as String
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Checkbox(
                            checked = visibleBands[band] ?: false,
                            onCheckedChange = { visibleBands[band] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(android.graphics.Color.parseColor(colorHex)))
                        )
                        Text(band, fontSize = 12.sp)
                    }
                }
            }

            // Map Footer
            Card(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                val activeLayers = coverageData.filter { visibleBands[it["band"] as String] == true }
                FootprintMapComponent(txCoords.first, txCoords.second, activeLayers)
            }
        } else {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Run Analysis to generate map.", color = Color.Gray)
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
                controller.setZoom(3.5)
                controller.setCenter(GeoPoint(txLat, txLon))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Add Station Marker
            val marker = Marker(mapView).apply {
                position = GeoPoint(txLat, txLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = context.getDrawable(android.R.drawable.btn_star_big_on)
            }
            mapView.overlays.add(marker)

            // Add Footprint Polygons
            activeLayers.forEach { layer ->
                val colorHex = layer["color"] as String
                val ptsRaw = layer["points"] as List<List<Double>>
                val geoPoints = ptsRaw.map { GeoPoint(it[0], it[1]) }
                val poly = Polygon().apply {
                    fillPaint.color = android.graphics.Color.parseColor(colorHex)
                    fillPaint.alpha = 45
                    outlinePaint.color = android.graphics.Color.parseColor(colorHex)
                    outlinePaint.strokeWidth = 2f
                    setPoints(geoPoints)
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

// --- PYTHON BRIDGES ---

suspend fun runPrediction(context: Context, tx: Pair<Double, Double>, rx: Pair<Double, Double>, ssn: Int, power: Int, mode: String): List<BandPrediction> = withContext(Dispatchers.IO) {
    if (!Python.isStarted()) Python.start(AndroidPlatform(context))
    val py = Python.getInstance()
    val module = py.getModule("voacap_engine")
    val res = module.callAttr("calculate_propagation", tx.first, tx.second, rx.first, rx.second, ssn, power, mode)
    res.asList().map { pyItem ->
        val map = pyItem.asMap() as Map<*, *>
        val probs = (map["probs"] as? com.chaquo.python.PyObject)?.asList()
        BandPrediction(map["band"]?.toString() ?: "??", probs?.map { it.toInt() } ?: List(24){0})
    }
}

suspend fun runCoverageCalculation(context: Context, tx: Pair<Double, Double>, ssn: Int, power: Int, mode: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
    if (!Python.isStarted()) Python.start(AndroidPlatform(context))
    val py = Python.getInstance()
    val module = py.getModule("coverage_engine")
    val res = module.callAttr("get_area_coverage", tx.first, tx.second, ssn, power, mode)
    res.asList().map { pyItem ->
        pyItem.asMap().mapKeys { it.key.toString() }.mapValues { entry ->
            if (entry.key == "points") {
                entry.value.asList().map { p -> listOf(p.asList()[0].toDouble(), p.asList()[1].toDouble()) }
            } else entry.value.toString()
        }
    }
}