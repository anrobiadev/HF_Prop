package com.example.hfpropagation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoacapHeatmap(results: List<BandPrediction>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Time Header (00 to 23 UTC)
        Row(modifier = Modifier.padding(start = 50.dp)) {
            (0..23).forEach { hour ->
                Box(
                    modifier = Modifier.width(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = hour.toString().padStart(2, '0'), fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        // 2. Band Rows
        results.forEach { band ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Band Label (e.g., 20m)
                Text(
                    text = band.bandName,
                    modifier = Modifier.width(50.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // The Cells
                band.hourlyProbabilities.forEach { probability ->
                    val color = getHeatmapColor(probability)
                    Box(
                        modifier = Modifier
                            .size(width = 30.dp, height = 30.dp)
                            .padding(1.dp)
                            .background(color, shape = MaterialTheme.shapes.extraSmall)
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        if (probability > 10) {
                            Text(
                                text = probability.toString(),
                                fontSize = 8.sp,
                                color = if (probability > 60) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }

        // 3. Legend actualizată pe baza noilor culori
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            LegendItem("Exc", Color(0xFF1F5922))
            LegendItem("Good", Color(0xFF4CAF50))
            LegendItem("Fair", Color(0xFFFBC02D))
            LegendItem("Poor", Color(0xFFC62828))
            LegendItem("Closed", Color(0xFF850808))
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, shape = MaterialTheme.shapes.extraSmall))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

// Logica de culori rafinată (păstrând stilul tău)
fun getHeatmapColor(prob: Int): Color {
    return when {
        prob >= 80 -> Color(0xFF1F5922) // Deep Green
        prob >= 70 -> Color(0xFF4CAF50) // Light Green
        prob >= 50 -> Color(0xFFFBC02D) // Yellow/Amber
        prob >= 30 -> Color(0xFFC66728) // Orange
        prob >= 15 -> Color(0xFFC62828) // Red
        prob > 0   -> Color(0xFF850808) // Deep Dark Red
        else       -> Color(0xFF212121) // Black/Dark Grey
    }
}