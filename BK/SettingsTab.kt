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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    context: Context,
    s: AppStrings,
    selectedLang: String, // NOU
    selectedTheme: String, // Thema select
    onThemeChanged: (String) -> Unit, // Thema load
    onLangChanged: (String) -> Unit,
    ssn: Int,
    sfi: Int,
    kIndex: Int,
    power: Int,
    antenna: String,
    mode: String,
    onSettingsChanged: (Int, Int, Int, Int, String, String) -> Unit
) {
    // Folosim remember(cheie) ca s? se actualizeze UI-ul automat când "Auto-Fetch" aduce date noi
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
    val langOptions = listOf("English", "Român?", "Magyar")

    val scope = rememberCoroutineScope()
    var isFetchingSolar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Station Configuration",
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
                        label = { Text(s.langLabel) }, // Folose?te dic?ionarul
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

                // Rândul cu butonul de Auto-Fetch
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
                                val data = SolarUtils.fetchSolarData(context)
                                if (data != null) {
                                    onSettingsChanged(data.ssn, data.sfi, data.kIndex, power, antenna, mode)
                                    // Nu mai este nevoie s? actualiz?m manual manualSsnText etc.,
                                    // deoarece am folosit remember(cheie) sus.
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

                // Rândul cu cele 3 câmpuri editabile (SSN, SFI, K-Index)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualSsnText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                manualSsnText = it
                                it.toIntOrNull()?.let { s ->
                                    onSettingsChanged(s, sfi, kIndex, power, antenna, mode)
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
                        // Înlocuie?te linia veche cu aceasta:
                        text = "${s.txtKinfo1}$kIndex${s.txtKinfo2}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}