package com.example.hfpropagation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    context: Context,
    s: AppStrings,
    selectedLang: String,
    selectedTheme: String,
    onThemeChanged: (String) -> Unit,
    onLangChanged: (String) -> Unit,
    ssn: Int,
    sfi: Int,
    kIndex: Int,
    power: Int,
    antenna: String,
    mode: String,
    // Parameters for foF2 logic and correction
    avgFoF2: Double,
    ssnEffective: Int,
    useCorrection: Boolean,
    txLat: Double?,
    txLon: Double?,
    stationValues: Map<String, Double> = emptyMap(),
    stationMuf: Map<String, Double> = emptyMap(),
    solarLastUpdate: Long = 0L,
    onCorrectionToggled: (Boolean) -> Unit,
    onIonosondeDataReceived: (SolarData) -> Unit, // --- NEW: Callback for complete data ---
    onSettingsChanged: (Int, Int, Int, Int, String, String) -> Unit
) {
    // Use remember(key) to automatically update UI when "Auto-Fetch" brings new data
    var powerText by remember(power) { mutableStateOf(power.toString()) }
    var manualSsnText by remember(ssn) { mutableStateOf(ssn.toString()) }
    var manualSfiText by remember(sfi) { mutableStateOf(sfi.toString()) }
    var manualKText by remember(kIndex) { mutableStateOf(kIndex.toString()) }

    // Dropdown expanded states
    var modeExpanded by remember { mutableStateOf(false) }
    var antExpanded by remember { mutableStateOf(false) }

    // Options
    val modeOptions = listOf("SSB", "CW", "FT8", "AM")
    val antennaOptions = listOf("Dipole", "Vertical", "Yagi (3-el)", "End-Fed Wire")

    var themeExpanded by remember { mutableStateOf(false) }
    val themeOptions = listOf("System Default", "Light", "Dark")
    var langExpanded by remember { mutableStateOf(false) }
    val langOptions = listOf("English", "Română", "Magyar")

    val scope = rememberCoroutineScope()
    var isFetchingSolar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = s.appearance, // Use variable from AppStrings
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- LANGUAGE DROPDOWN ---
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLang,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.langLabel) }, // Use the dictionary
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        leadingIcon = { Icon(Icons.Default.Language, null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                        langOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onLangChanged(option)
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- THEME DROPDOWN ---
                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = !themeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTheme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.themeLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                        themeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onThemeChanged(option)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = s.stationConfig,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- POWER INPUT ---
                OutlinedTextField(
                    value = powerText,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            powerText = it
                            it.toIntOrNull()?.let { p ->
                                onSettingsChanged(ssn, sfi, kIndex, p, antenna, mode)
                            }
                        }
                    },
                    label = { Text(s.pwrLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- ANTENNA DROPDOWN ---
                ExposedDropdownMenuBox(
                    expanded = antExpanded,
                    onExpandedChange = { antExpanded = !antExpanded }
                ) {
                    OutlinedTextField(
                        value = antenna,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.antLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = antExpanded) },
                        leadingIcon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = antExpanded, onDismissRequest = { antExpanded = false }) {
                        antennaOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSettingsChanged(ssn, sfi, kIndex, power, option, mode)
                                    antExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- MODE DROPDOWN ---
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded }
                ) {
                    OutlinedTextField(
                        value = mode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.modeLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        modeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSettingsChanged(ssn, sfi, kIndex, power, antenna, option)
                                    modeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SPACE WEATHER SECTION ---
        Text(
            text = s.solarLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Row with Auto-Fetch button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = s.nooaLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            isFetchingSolar = true
                            scope.launch {
                                val data = SolarUtils.fetchSolarData(context, s, ssn, txLat, txLon)

                                if (data != null) {
                                    // --- MODIFICATION: Send complete data back to MainActivity ---
                                    onIonosondeDataReceived(data)
                                    // Keep the old call for consistency
                                    onSettingsChanged(data.ssn, data.sfi, data.kIndex, power, antenna, mode)
                                }
                                isFetchingSolar = false
                            }
                        },
                        enabled = !isFetchingSolar
                    ) {
                        if (isFetchingSolar) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(s.autoFetchBtn)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- foF2 DATA AND CORRECTION CHECKBOX ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = s.fof2Label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = if (avgFoF2 > 0) "${"%.2f".format(avgFoF2)} MHz" else "---",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = s.useCorrectionLabel, style = MaterialTheme.typography.labelSmall)
                        Checkbox(
                            checked = useCorrection,
                            onCheckedChange = { onCorrectionToggled(it) }
                        )
                    }
                }

                // --- NEW: DISPLAY LIST OF ACTIVE STATIONS AND VALUES ---
                if (stationValues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = s.activeReferenceStations,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    stationValues.forEach { (code, value) ->
                        // Look up the station name in the global database using the code
                        val stationName = SolarUtils.globalStationDatabase.find { it.code == code }?.name ?: "Unknown Station"
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "• $stationName ($code)", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(text = "${"%.2f".format(value)} MHz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // DISPLAY EFFECTIVE SSN ONLY IF CORRECTION IS ENABLED
                if (useCorrection && avgFoF2 > 0) {
                    val delta = ssnEffective - ssn
                    val color = if (delta >= 0) Color(0xFF4CAF50) else Color(0xFFF44336) // Green vs Red
                    val sign = if (delta > 0) "+" else ""

                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.efectiveSSNeLabel,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$ssnEffective ($sign$delta)",
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Row with the 3 editable fields (SSN, SFI, K-Index)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualSsnText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                manualSsnText = it
                                it.toIntOrNull()?.let { ssnVal ->
                                    onSettingsChanged(ssnVal, sfi, kIndex, power, antenna, mode)
                                }
                            }
                        },
                        label = { Text(s.ssnLabel) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = manualSfiText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                manualSfiText = it
                                it.toIntOrNull()?.let { f ->
                                    onSettingsChanged(ssn, f, kIndex, power, antenna, mode)
                                }
                            }
                        },
                        label = { Text(s.sfiLabel) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = manualKText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                manualKText = it
                                it.toIntOrNull()?.let { k ->
                                    onSettingsChanged(ssn, sfi, k, power, antenna, mode)
                                }
                            }
                        },
                        label = { Text(s.kIndexLabel) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                if (kIndex >= 4) {
                    Text(
                        text = "${s.txtKinfo1}$kIndex${s.txtKinfo2}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ─── SOLAR DATA STATUS PANEL ────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Titlu + Last Update
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.solarDataTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (solarLastUpdate > 0L)
                            StorageUtils.formatSolarUpdateTime(solarLastUpdate)
                        else s.noDataLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (solarLastUpdate > 0L)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // SSN / SFI / K-Index
                @Composable
                fun StatusRow(label: String, value: String, ok: Boolean) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (ok) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }

                StatusRow("SSN (SILSO)",
                    if (ssn > 0) "$ssn" else "No data",
                    ssn > 0)
                StatusRow("SFI (DRAO)",
                    if (sfi > 0) "$sfi" else "No data",
                    sfi > 0)
                StatusRow("K-Index (NOAA)",
                    if (kIndex >= 0) "$kIndex" else "No data",
                    kIndex >= 0)

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Ionosonde stations
                Text(
                    text = "${s.ionosondeTitle}  [${s.lastIonoUpdate} ${StorageUtils.formatSolarUpdateTime(solarLastUpdate)}]",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                val allStations = SolarUtils.globalStationDatabase
                    .filter { it.code in (stationValues.keys + stationMuf.keys) ||
                            stationValues.containsKey(it.code) }

                if (stationValues.isEmpty()) {
                    Text(
                        text = s.noIonoData,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    stationValues.forEach { (code, fof2) ->
                        val stName = SolarUtils.globalStationDatabase
                            .find { it.code == code }?.name ?: code
                        if (fof2 < 0) {
                            // Station unavailable (fetch failed)
                            StatusRow(stName, s.stationUnavailable, false)
                        } else {
                            val muf = stationMuf[code]
                            val valueStr = if (muf != null && muf > 0)
                                "foF2 ${String.format("%.2f", fof2)} MHz  |  MUF ${String.format("%.1f", muf)} MHz"
                            else
                                "foF2 ${String.format("%.2f", fof2)} MHz"
                            StatusRow(stName, valueStr, true)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SolarDataDisplay(solarData: SolarData) {
    Column {
        // Standard SSN
        Text(text = "SSN (SILSO): ${solarData.ssn}")

        // Effective SSN (Modified)
        if (solarData.ssnEffective != solarData.ssn) {
            val delta = solarData.ssnEffective - solarData.ssn
            val color = if (delta > 0) Color.Green else Color.Red
            val sign = if (delta > 0) "+" else ""

            Row {
                Text(text = "SSNe (Effective): ")
                Text(
                    text = "${solarData.ssnEffective} ($sign$delta)",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}