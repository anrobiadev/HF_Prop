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
    ssn: Int,
    power: Int,
    antenna: String,
    mode: String,
    onSettingsChanged: (Int, Int, String, String) -> Unit
) {
    // Local state for smooth text input
    var powerText by remember { mutableStateOf(power.toString()) }
    var manualSsnText by remember { mutableStateOf(ssn.toString()) }

    // Dropdown expanded states
    var antExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    // Options
    val antennaOptions = listOf("Dipole", "Vertical", "Yagi (3-el)", "End-Fed Wire", "Yagi (5-el)")
    val modeOptions = listOf("SSB", "CW", "FT8", "AM")

    val scope = rememberCoroutineScope()
    var isFetchingSolar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Station Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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
                                onSettingsChanged(ssn, p, antenna, mode)
                                StorageUtils.saveSettings(context, p, antenna)
                            }
                        }
                    },
                    label = { Text("TX Power (Watts)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))


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
                        label = { Text("Operating Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        leadingIcon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        modeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSettingsChanged(ssn, power, antenna, option)
                                    StorageUtils.saveMode(context, option)
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
        Text("Solar Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Sunspot Number", style = MaterialTheme.typography.labelMedium)
                        Text("$ssn", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            isFetchingSolar = true
                            scope.launch {
                                try {
                                    val fresh = SolarUtils.fetchCurrentSSN()
                                    onSettingsChanged(fresh, power, antenna, mode)
                                    manualSsnText = fresh.toString()
                                    StorageUtils.saveSSN(context, fresh)
                                } catch (e: Exception) { /* Handle error */ }
                                finally { isFetchingSolar = false }
                            }
                        },
                        enabled = !isFetchingSolar
                    ) {
                        if (isFetchingSolar) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Auto-Fetch")
                    }
                }

                OutlinedTextField(
                    value = manualSsnText,
                    onValueChange = {
                        manualSsnText = it
                        it.toIntOrNull()?.let { s ->
                            onSettingsChanged(s, power, antenna, mode)
                            StorageUtils.saveSSN(context, s)
                        }
                    },
                    label = { Text("Manual SSN Entry") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}