package com.example.hfpropagation

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun LocationTab(
    s: AppStrings,
    tx: Pair<Double, Double>?,
    rx: Pair<Double, Double>?,
    txGridText: String,
    rxGridText: String,
    stationValues: Map<String, Double>,
    onTxGridTextChanged: (String) -> Unit,
    onRxGridTextChanged: (String) -> Unit,
    loading: Boolean,
    onTxChanged: (Pair<Double, Double>?) -> Unit,
    onRxChanged: (Pair<Double, Double>?) -> Unit,
    onRunPrediction: () -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (tx == null && txGridText.isNotEmpty()) {
            onTxChanged(MaidenheadUtils.gridToLatLon(txGridText))
        }
        if (rx == null && rxGridText.isNotEmpty()) {
            onRxChanged(MaidenheadUtils.gridToLatLon(rxGridText))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        LocationInputCard(
            title = s.locationSource,
            value = txGridText,
            onValueChange = onTxGridTextChanged,
            showGps = true,
            onResolved = onTxChanged
        )

        Spacer(modifier = Modifier.height(8.dp))

        LocationInputCard(
            title = s.locationTarget,
            value = rxGridText,
            onValueChange = onRxGridTextChanged,
            showGps = false,
            onResolved = onRxChanged
        )

        if (tx != null && rx != null) {
            val dist = MaidenheadUtils.PathMath.calculateDistance(tx.first, tx.second, rx.first, rx.second)
            val bear = MaidenheadUtils.PathMath.calculateBearing(tx.first, tx.second, rx.first, rx.second)

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(s.patGeom, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${"%.1f".format(dist)} km | " + s.beringLabel + ": ${"%.1f".format(bear)}°",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Button(
            onClick = onRunPrediction,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = tx != null && rx != null && !loading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(s.analyzeBtn, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(s.pathPrevLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().height(350.dp).padding(top = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            if (tx != null && rx != null) {
                PropagationMap(
                    txLat = tx.first,
                    txLon = tx.second,
                    rxLat = rx.first,
                    rxLon = rx.second,
                    stationValues = stationValues
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.msgErorLocator, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun LocationInputCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    showGps: Boolean,
    onResolved: (Pair<Double, Double>?) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    val upper = input.uppercase()
                    onValueChange(upper)
                    if (upper.length >= 4) {
                        val coords = MaidenheadUtils.gridToLatLon(upper)
                        onResolved(coords)
                    } else {
                        onResolved(null)
                    }
                },
                label = { Text("Maidenhead Grid (e.g., JO32)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (showGps) {
                        IconButton(onClick = {
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                            try {
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { location ->
                                        location?.let {
                                            val grid = MaidenheadUtils.latLonToGrid(it.latitude, it.longitude)
                                            onValueChange(grid)
                                            onResolved(Pair(it.latitude, it.longitude))
                                        }
                                    }
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Get GPS")
                        }
                    }
                }
            )
        }
    }
}