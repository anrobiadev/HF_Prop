package com.example.hfpropagation

import android.content.Context
import android.content.SharedPreferences

object StorageUtils {
    private const val PREF_NAME = "hf_prop_prefs"

    // Keys
    private const val KEY_TX_GRID = "tx_grid"
    private const val KEY_RX_GRID = "rx_grid"
    private const val KEY_SSN = "ssn"
    private const val KEY_POWER = "tx_power"
    private const val KEY_ANTENNA = "tx_antenna"
    private const val KEY_MAP_SOURCE = "map_source"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- LOCATION SAVING ---
    fun saveLocations(context: Context, txGrid: String, rxGrid: String) {
        getPrefs(context).edit()
            .putString(KEY_TX_GRID, txGrid)
            .putString(KEY_RX_GRID, rxGrid)
            .apply()
    }

    fun loadTxGrid(context: Context): String = getPrefs(context).getString(KEY_TX_GRID, "") ?: ""
    fun loadRxGrid(context: Context): String = getPrefs(context).getString(KEY_RX_GRID, "") ?: ""

    // --- STATION SETTINGS ---
    fun saveSettings(context: Context, power: Int, antenna: String) {
        getPrefs(context).edit()
            .putInt(KEY_POWER, power)
            .putString(KEY_ANTENNA, antenna)
            .apply()
    }

    fun loadPower(context: Context): Int = getPrefs(context).getInt(KEY_POWER, 100)
    fun loadAntenna(context: Context): String = getPrefs(context).getString(KEY_ANTENNA, "Dipole") ?: "Dipole"

    // --- SOLAR WEATHER ---
    fun saveSSN(context: Context, ssn: Int) {
        getPrefs(context).edit().putInt(KEY_SSN, ssn).apply()
    }

    fun loadSSN(context: Context): Int = getPrefs(context).getInt(KEY_SSN, 70) // Default to 70

    // --- MAP SETTINGS ---
    fun saveMapSource(context: Context, source: String) {
        getPrefs(context).edit().putString(KEY_MAP_SOURCE, source).apply()
    }

    private const val KEY_MODE = "prop_mode"

    fun saveMode(context: Context, mode: String) {
        context.getSharedPreferences("hf_prop_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode).apply()
    }

    fun loadMode(context: Context): String =
        context.getSharedPreferences("hf_prop_prefs", Context.MODE_PRIVATE)
            .getString(KEY_MODE, "SSB") ?: "SSB"
    fun loadMapSource(context: Context): String = getPrefs(context).getString(KEY_MAP_SOURCE, "OSM") ?: "OSM"

}