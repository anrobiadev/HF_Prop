package com.example.hfpropagation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import org.osmdroid.views.overlay.infowindow.InfoWindow

@Composable
fun PropagationMap(
    txLat: Double,
    txLon: Double,
    rxLat: Double,
    rxLon: Double,
    stationValues: Map<String, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var showGrid by remember { mutableStateOf(true) }
    var layersExpanded by remember { mutableStateOf(false) }
    var zoomTrigger by remember { mutableIntStateOf(0) }

    var currentTileSource by remember {
        mutableStateOf(
            when (StorageUtils.loadMapSource(context)) {
                "Topo" -> TileSourceFactory.OpenTopo
                else -> TileSourceFactory.MAPNIK
            }
        )
    }

    val tileOptions = listOf(
        "Standard" to TileSourceFactory.MAPNIK,
        "Topographic" to TileSourceFactory.OpenTopo,
        "Satellite" to TileSourceFactory.USGS_SAT
    )

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                }
            },
            update = { mapView ->
                // Închidem toate ferestrele deschise pentru a evita bug-uri vizuale
                InfoWindow.closeAllInfoWindowsOn(mapView)

                mapView.overlays.clear()
                mapView.setTileSource(currentTileSource)

                // 1. GESTIONARE LONG PRESS (Copy Locator)
                val mReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false
                    override fun longPressHelper(p: GeoPoint): Boolean {
                        val locator = MaidenheadUtils.latLonToGrid(p.latitude, p.longitude).take(6)
                        val clip = ClipData.newPlainText("Maidenhead Locator", locator)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Locator $locator copied to clipboard", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
                mapView.overlays.add(MapEventsOverlay(mReceiver))

                // 2. GRIDLINES
                if (showGrid) {
                    mapView.overlays.add(MaidenheadGridOverlay(context))
                }

                val txPoint = GeoPoint(txLat, txLon)
                val rxPoint = GeoPoint(rxLat, rxLon)

                // 3. IONOSONDE STATIONS (closest to TX, with live foF2 values)
                val activeStationCodes = SolarUtils.getClosestGlobalStations(txLat, txLon)
                if (activeStationCodes.isNotEmpty()) {
                    SolarUtils.globalStationDatabase.filter { it.code in activeStationCodes }.forEach { station ->
                        val stationPoint = GeoPoint(station.lat, station.lon)
                        val individualValue = stationValues[station.code]
                        val valueDisplay = if (individualValue != null && individualValue > 0) "$individualValue MHz" else "Waiting for Sync..."

                        // Marker Ionosondă
                        val stationMarker = Marker(mapView).apply {
                            position = stationPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = station.name
                            snippet = "Code: ${station.code}\nfoF2: $valueDisplay"
                            icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_my_ionosonde)
                            icon?.setTint(AndroidColor.MAGENTA)

                            setOnMarkerClickListener { marker, _ ->
                                marker.showInfoWindow()
                                true
                            }
                        }
                        mapView.overlays.add(stationMarker)

                        // --- LINIA ROZ (Sync) - MODIFICATĂ PENTRU A NU AFIȘA INFO TAB ---
                        val syncLine = Polyline(mapView).apply {
                            outlinePaint.color = AndroidColor.argb(100, 255, 0, 255)
                            outlinePaint.strokeWidth = 3f
                            setPoints(listOf(txPoint, stationPoint))

                            // Dezactivăm fereastra de informații pentru această linie
                            title = null
                            snippet = null
                            infoWindow = null

                            // Setăm un click listener gol care returnează false pentru a ignora atingerea
                            setOnClickListener { _, _, _ -> false }
                        }
                        mapView.overlays.add(syncLine)
                    }
                }

                // 4. TX MARKER
                val txMarker = Marker(mapView).apply {
                    position = txPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Source (TX)"
                    ContextCompat.getDrawable(mapView.context, R.drawable.ic_tx_tower)?.let {
                        it.setTint(AndroidColor.RED)
                        icon = it
                    }
                }

                // 5. RX MARKER
                val rxMarker = Marker(mapView).apply {
                    position = rxPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Target (RX)"
                    ContextCompat.getDrawable(mapView.context, R.drawable.ic_rx_target)?.let {
                        it.setTint(AndroidColor.BLUE)
                        icon = it
                    }
                }

                // 6. PATH LINE (Linia principală TX-RX)
                val pathLine = Polyline(mapView).apply {
                    outlinePaint.color = AndroidColor.argb(180, 255, 0, 0)
                    outlinePaint.strokeWidth = 6f
                    setPoints(listOf(txPoint, rxPoint))
                    title = "Signal Path"
                    // Dacă vrei să dezactivezi info tab și aici, pune infoWindow = null
                }

                mapView.overlays.add(pathLine)
                mapView.overlays.add(txMarker)
                mapView.overlays.add(rxMarker)

                // 7. SMART AUTO-ZOOM
                // Auto-zoom to fit TX, RX, and all ionosonde stations
                val allPoints = mutableListOf<GeoPoint>()
                allPoints.add(txPoint)
                allPoints.add(rxPoint)
                SolarUtils.globalStationDatabase.filter { it.code in activeStationCodes }.forEach {
                    allPoints.add(GeoPoint(it.lat, it.lon))
                }

                if (allPoints.size >= 2) {
                    val forceTrigger = zoomTrigger
                    mapView.post {
                        try {
                            val box = BoundingBox.fromGeoPoints(allPoints)
                            mapView.zoomToBoundingBox(box, true, 250)
                        } catch (e: Exception) {
                            mapView.controller.setCenter(txPoint)
                            mapView.controller.setZoom(3.0)
                        }
                    }
                }
                mapView.invalidate()
            }
        )

        // 8. BUTOANE OVERLAY
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            SmallFloatingActionButton(
                onClick = { layersExpanded = true },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Map Layers", modifier = Modifier.size(20.dp))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { zoomTrigger++ },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit All", modifier = Modifier.size(20.dp))
                }
            }

            DropdownMenu(expanded = layersExpanded, onDismissRequest = { layersExpanded = false }) {
                Text("Map Source", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                tileOptions.forEach { (name, source) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            currentTileSource = source
                            StorageUtils.saveMapSource(context, name)
                            layersExpanded = false
                        },
                        trailingIcon = { if (currentTileSource == source) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text("QTH Gridlines") },
                    onClick = { showGrid = !showGrid },
                    trailingIcon = { Checkbox(checked = showGrid, onCheckedChange = { showGrid = it }) }
                )
            }
        }
    }
}

class StyledGridlineOverlay : LatLonGridlineOverlay2() {
    fun setCustomStyle(color: Int, width: Float) {
        mLinePaint.color = color
        mLinePaint.strokeWidth = width
    }
}

class MaidenheadGridOverlay(val context: Context) : Overlay() {
    private val linePaint = Paint().apply {
        color = android.graphics.Color.argb(80, 6, 39, 168)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = android.graphics.Color.argb(120, 255, 0, 0)
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val zoom = mapView.zoomLevelDouble
        val boundingBox = mapView.boundingBox
        val projection = mapView.projection

        val (latStep, lonStep, precision) = when {
            zoom > 11.5 -> Triple(2.5 / 60.0, 5.0 / 60.0, 6)
            zoom > 7.5 -> Triple(1.0, 2.0, 4)
            else -> Triple(10.0, 20.0, 2)
        }

        val startLat = (Math.floor(boundingBox.latSouth / latStep) * latStep).coerceIn(-90.0, 90.0)
        val endLat = (Math.ceil(boundingBox.latNorth / latStep) * latStep).coerceIn(-90.0, 90.0)
        val startLon = (Math.floor(boundingBox.lonWest / lonStep) * lonStep).coerceIn(-180.0, 180.0)
        val endLon = (Math.ceil(boundingBox.lonEast / lonStep) * lonStep).coerceIn(-180.0, 180.0)

        var currLat = startLat
        while (currLat <= endLat) {
            var currLon = startLon
            while (currLon <= endLon) {
                val centerLat = currLat + (latStep / 2.0)
                val centerLon = currLon + (lonStep / 2.0)

                val screenPos = android.graphics.Point()
                projection.toPixels(GeoPoint(centerLat, centerLon), screenPos)

                val label = MaidenheadUtils.latLonToGrid(centerLat, centerLon).take(precision)

                if (screenPos.x in 0..mapView.width && screenPos.y in 0..mapView.height) {
                    canvas.drawText(label, screenPos.x.toFloat(), screenPos.y.toFloat(), textPaint)
                }

                val p1 = android.graphics.Point()
                val p2 = android.graphics.Point()

                projection.toPixels(GeoPoint(currLat, currLon), p1)
                projection.toPixels(GeoPoint(currLat, currLon + lonStep), p2)
                canvas.drawLine(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat(), linePaint)

                projection.toPixels(GeoPoint(currLat, currLon), p1)
                projection.toPixels(GeoPoint(currLat + latStep, currLon), p2)
                canvas.drawLine(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat(), linePaint)

                currLon += lonStep
            }
            currLat += latStep
        }
    }
}