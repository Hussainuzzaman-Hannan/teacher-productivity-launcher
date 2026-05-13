package com.teacher.productivitylauncher.presentation.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockViewModel : ViewModel() {

    private val _currentTime = MutableStateFlow(Calendar.getInstance().time)
    val currentTime: StateFlow<Date> = _currentTime.asStateFlow()

    private val _timeFormat = MutableStateFlow(SimpleDateFormat("hh:mm a", Locale.getDefault()))
    val timeFormat: StateFlow<SimpleDateFormat> = _timeFormat.asStateFlow()

    private val _dateFormat = MutableStateFlow(SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()))
    val dateFormat: StateFlow<SimpleDateFormat> = _dateFormat.asStateFlow()

    // Clock style: 0 = Minimal Digital, 1 = Large Digital with Seconds, 2 = Analog, 3 = Gradient Digital, 4 = Split Neon
    private val _clockStyle = MutableStateFlow(0)
    val clockStyle: StateFlow<Int> = _clockStyle.asStateFlow()

    init {
        startClock()
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = Calendar.getInstance().time
                delay(1000)
            }
        }
    }

    fun nextClockStyle() {
        _clockStyle.value = (_clockStyle.value + 1) % 5
    }

    fun previousClockStyle() {
        _clockStyle.value = if (_clockStyle.value == 0) 4 else _clockStyle.value - 1
    }

    fun setClockStyle(style: Int) {
        _clockStyle.value = style.coerceIn(0, 4)
    }

    fun updateLocale() {
        _timeFormat.value = SimpleDateFormat("hh:mm a", Locale.getDefault())
        _dateFormat.value = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    }
}