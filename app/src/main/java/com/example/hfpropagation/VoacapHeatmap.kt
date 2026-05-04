package com.example.hfpropagation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (probability > 10) { // Only show text for significant numbers
                            Text(
                                text = probability.toString(),
                                fontSize = 8.sp,
                                color = if (probability > 50) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }

        // 3. Legend
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendItem("Good", Color(0xFF2E7D32))
            LegendItem("Fair", Color(0xFFFBC02D))
            LegendItem("Poor", Color(0xFFC62828))
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

fun getHeatmapColor(prob: Int): Color {
    return when {
        prob >= 75 -> Color(0xFF2E7D32) // Deep Green
        prob >= 50 -> Color(0xFF4CAF50) // Light Green
        prob >= 30 -> Color(0xFFFBC02D) // Yellow/Amber
        prob >= 15 -> Color(0xFFC62828) // Red
        else -> Color(0xFFE0E0E0)       // Grey (Closed)
    }
}