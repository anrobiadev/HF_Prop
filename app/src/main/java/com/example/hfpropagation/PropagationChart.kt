package com.example.hfpropagation

import androidx.compose.foundation.isSystemInDarkTheme
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
fun PropagationChart(
    mufData: List<Double>,
    lufData: List<Double>,
    fotData: List<Double>,
    onPointSelected: (hour: Int, muf: Double, luf: Double, fot: Double) -> Unit
) {


    // MODIFICĂ AICI CULORILE HEX PENTRU AXE
    val axisTextColorHex = "#C20A0A" // Exemplu: Verde Radar
    val gridLineColorHex = "#33FFFFFF" // Grilă gri/alb semi-transparentă

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)

                // Fundal grafic (opțional, poți seta TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // --- CONFIGURARE AXA X ---
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = android.graphics.Color.parseColor(axisTextColorHex)
                    setDrawGridLines(false)

                    // PASUL DIN 2 ÎN 2
                    granularity = 2f
                    isGranularityEnabled = true

                    axisMinimum = 0f
                    axisMaximum = 23f
                    labelCount = 12 // Forțează afișarea a 12 etichete (0, 2, 4... 22)
                }

                // --- CONFIGURARE AXA Y (STÂNGA) ---
                axisLeft.apply {
                    textColor = android.graphics.Color.parseColor(axisTextColorHex)
                    gridColor = android.graphics.Color.parseColor(gridLineColorHex)
                    setDrawGridLines(true)
                    axisMinimum = 0f
                }

                axisRight.isEnabled = false // Ascunde axa din dreapta

                // --- LEGENDĂ ---
                legend.apply {
                    textColor = android.graphics.Color.parseColor(axisTextColorHex)
                    isEnabled = true
                }

                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        e?.let {
                            val hour = it.x.toInt()
                            if (hour in mufData.indices) {
                                onPointSelected(hour, mufData[hour], lufData[hour], fotData[hour])
                            }
                        }
                    }
                    override fun onNothingSelected() {}
                })
            }
        },
        update = { chart ->
            val mufSet = LineDataSet(mufData.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }, "MUF").apply {
                color = android.graphics.Color.parseColor("#FF3131") // HEX MUF
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(android.graphics.Color.parseColor("#FF3131"))
                setDrawValues(false)
                lineWidth = 2f
            }

            val lufSet = LineDataSet(lufData.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }, "LUF").apply {
                color = android.graphics.Color.parseColor("#162DC4") // HEX LUF
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
            }

            val fotSet = LineDataSet(fotData.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }, "FOT").apply {
                color = android.graphics.Color.parseColor("#1C853D") // HEX FOT
                setDrawCircles(false)
                enableDashedLine(10f, 5f, 0f)
                setDrawValues(false)
                lineWidth = 1.5f
            }

            chart.data = LineData(mufSet, lufSet, fotSet)
            chart.invalidate()
        }
    )
}