package com.example.hfpropagation

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun PropagationMap(
    txLat: Double,
    txLon: Double,
    rxLat: Double,
    rxLon: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Read the map source from our storage (OSM, Topo, or Offline)
    val mapSource = StorageUtils.loadMapSource(context)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // --- CRITICAL: Identify the app to OSM servers to allow downloads ---
            Configuration.getInstance().userAgentValue = ctx.packageName

            MapView(ctx).apply {
                setMultiTouchControls(true)
                // Hide default zoom buttons for a cleaner UI
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                // Set tiles based on Settings
                when (mapSource) {
                    "Topo" -> setTileSource(TileSourceFactory.OpenTopo)
                    "Offline" -> {
                        // Correct method to disable network tile fetching
                        setUseDataConnection(false)
                        setTileSource(TileSourceFactory.MAPNIK)
                    }
                    else -> setTileSource(TileSourceFactory.MAPNIK) // Standard OSM
                }
            }
        },
        update = { mapView ->
            // Clear old markers/lines before drawing new ones
            mapView.overlays.clear()

            val txPoint = GeoPoint(txLat, txLon)
            val rxPoint = GeoPoint(rxLat, rxLon)

            // --- 1. TX Marker (Radio Tower) ---
            val txMarker = Marker(mapView).apply {
                position = txPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Source (TX)"
                icon = mapView.context.getDrawable(R.drawable.ic_tx_tower)?.apply {
                    setTint(Color.RED)
                }
            }

            // --- 2. RX Marker (Target) ---
            val rxMarker = Marker(mapView).apply {
                position = rxPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Target (RX)"
                icon = mapView.context.getDrawable(R.drawable.ic_rx_target)?.apply {
                    setTint(Color.BLUE)
                }
            }

            // --- 3. Great Circle Path Line ---
            val pathLine = Polyline(mapView).apply {
                outlinePaint.color = Color.argb(180, 255, 0, 0) // Semi-transparent red
                outlinePaint.strokeWidth = 6f
                setPoints(listOf(txPoint, rxPoint))
            }

            // Add all elements back to the map
            mapView.overlays.add(pathLine)
            mapView.overlays.add(txMarker)
            mapView.overlays.add(rxMarker)

            // --- 4. Auto-Zoom to Path Extent ---
            if (txLat != 0.0 && rxLat != 0.0) {
                val box = BoundingBox.fromGeoPoints(listOf(txPoint, rxPoint))

                // We use post to ensure the MapView has measured its size before zooming
                mapView.post {
                    // Zoom to fit with 200 pixels of padding so icons aren't on the edge
                    mapView.zoomToBoundingBox(box, true, 200)
                }
            }

            mapView.invalidate() // Refresh the display
        }
    )
}