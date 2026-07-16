package com.van.status

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemePalette(
    val name: String,
    val primaryColor: Color,
    val alertColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color
)

object VehicleStatusManager {
    // Theme presets
    val ThemePalettes = listOf(
        ThemePalette("Default Red Warning", Color(0xFF38BDF8), Color(0xFFEF4444), Color(0xFF0D0D0D), Color(0xFF1E293B)),
        ThemePalette("Cyberpunk Neon Orange", Color(0xFFF97316), Color(0xFFEAB308), Color(0xFF1E1B4B), Color(0xFF312E81)),
        ThemePalette("Electric Blue", Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF0F172A), Color(0xFF1E293B)),
        ThemePalette("Minimalist Green", Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF022C22), Color(0xFF064E3B)),
        ThemePalette("Dark Monochrome", Color(0xFFCCCCCC), Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF111111))
    )

    private val _selectedThemeIndex = MutableStateFlow(0)
    val selectedThemeIndex: StateFlow<Int> = _selectedThemeIndex.asStateFlow()

    // Custom user-defined palette colors (used when themeIndex == ThemePalettes.size)
    private val _customPrimaryColor = MutableStateFlow(Color(0xFF38BDF8))
    val customPrimaryColor: StateFlow<Color> = _customPrimaryColor.asStateFlow()

    private val _customAlertColor = MutableStateFlow(Color(0xFFEF4444))
    val customAlertColor: StateFlow<Color> = _customAlertColor.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(Color(0xFF0D0D0D))
    val customBackgroundColor: StateFlow<Color> = _customBackgroundColor.asStateFlow()

    private val _customSurfaceColor = MutableStateFlow(Color(0xFF1E293B))
    val customSurfaceColor: StateFlow<Color> = _customSurfaceColor.asStateFlow()

    private val _globalVolume = MutableStateFlow(100)
    val globalVolume: StateFlow<Int> = _globalVolume.asStateFlow()

    private val _isFlOpen = MutableStateFlow(false)
    val isFlOpen: StateFlow<Boolean> = _isFlOpen.asStateFlow()

    private val _isFrOpen = MutableStateFlow(false)
    val isFrOpen: StateFlow<Boolean> = _isFrOpen.asStateFlow()

    private val _isRlOpen = MutableStateFlow(false)
    val isRlOpen: StateFlow<Boolean> = _isRlOpen.asStateFlow()

    private val _isBackOpen = MutableStateFlow(false)
    val isBackOpen: StateFlow<Boolean> = _isBackOpen.asStateFlow()

    private val _isBuzzerEnabled = MutableStateFlow(true)
    val isBuzzerEnabled: StateFlow<Boolean> = _isBuzzerEnabled.asStateFlow()

    private val _cabinTemperature = MutableStateFlow(24)
    val cabinTemperature: StateFlow<Int> = _cabinTemperature.asStateFlow()

    // Diagnostic console logs
    private val _diagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    fun setSelectedThemeIndex(index: Int) {
        _selectedThemeIndex.value = index
    }

    fun setGlobalVolume(volume: Int) {
        _globalVolume.value = volume.coerceIn(0, 100)
    }

    fun setCustomPrimaryColor(color: Color) { _customPrimaryColor.value = color }
    fun setCustomAlertColor(color: Color) { _customAlertColor.value = color }
    fun setCustomBackgroundColor(color: Color) { _customBackgroundColor.value = color }
    fun setCustomSurfaceColor(color: Color) { _customSurfaceColor.value = color }

    fun updateDoorState(door: String, isOpen: Boolean) {
        when (door.uppercase().trim()) {
            "FL" -> _isFlOpen.value = isOpen
            "FR" -> _isFrOpen.value = isOpen
            "RL" -> _isRlOpen.value = isOpen
            "BACK", "RR" -> _isBackOpen.value = isOpen
        }
    }

    fun updateCabinTemperature(temp: Int) {
        _cabinTemperature.value = temp
    }

    fun setBuzzerEnabled(enabled: Boolean) {
        _isBuzzerEnabled.value = enabled
    }

    fun addDiagnosticLog(log: String) {
        val current = _diagnosticLogs.value.toMutableList()
        current.add(log)
        if (current.size > 100) {
            current.removeAt(0)
        }
        _diagnosticLogs.value = current
    }

    fun clearDiagnosticLogs() {
        _diagnosticLogs.value = emptyList()
    }

    fun resetAllStates() {
        _isFlOpen.value = false
        _isFrOpen.value = false
        _isRlOpen.value = false
        _isBackOpen.value = false
        _isBuzzerEnabled.value = true
        _cabinTemperature.value = 24
        _selectedThemeIndex.value = 0
        _globalVolume.value = 100
        _customPrimaryColor.value = Color(0xFF38BDF8)
        _customAlertColor.value = Color(0xFFEF4444)
        _customBackgroundColor.value = Color(0xFF0D0D0D)
        _customSurfaceColor.value = Color(0xFF1E293B)
        clearDiagnosticLogs()
    }
}
