package com.van.status

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VehicleStatusManager {
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

    private val _cabinTemperature = MutableStateFlow(24) // Default cabin temperature
    val cabinTemperature: StateFlow<Int> = _cabinTemperature.asStateFlow()

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
}
