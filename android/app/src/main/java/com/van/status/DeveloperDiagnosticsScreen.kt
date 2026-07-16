package com.van.status

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDiagnosticsScreen(
    logs: List<String>,
    onClearLogs: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("van_prefs", Context.MODE_PRIVATE) }
    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("🎨 UI Theme", "⚙️ Settings", "📊 Resource Monitor")

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive in monitor tab
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && activeTab == 2) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)) // Dark cyber black backdrop
            .padding(24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color(0x13FFFFFF), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back to Settings",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "DEVELOPER ENGINE PORTAL",
                color = Color(0xFF10B981),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Tab Row Switcher
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(8.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = (activeTab == index),
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (activeTab == index) Color(0xFF38BDF8) else Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            // Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> ThemeCustomizationTab(context)
                    1 -> SystemSettingsTab(context)
                    2 -> ResourceMonitorTab(
                        logs = logs,
                        onClearLogs = onClearLogs,
                        listState = listState,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCustomizationTab(context: Context) {
    val sharedPrefs = remember { context.getSharedPreferences("van_prefs", Context.MODE_PRIVATE) }
    val themeIndex by VehicleStatusManager.selectedThemeIndex.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF020617), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PRE-DEFINED DESIGN PALETTES",
            color = Color(0xFF38BDF8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        VehicleStatusManager.ThemePalettes.forEachIndexed { index, palette ->
            val isSelected = (themeIndex == index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0x1F38BDF8) else Color(0x05FFFFFF),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        VehicleStatusManager.setSelectedThemeIndex(index)
                        sharedPrefs.edit().putInt("pref_theme_index", index).apply()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = palette.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorIndicator(palette.primaryColor, "Primary")
                        ColorIndicator(palette.alertColor, "Alert")
                        ColorIndicator(palette.backgroundColor, "BG")
                    }
                }
                RadioButton(
                    selected = isSelected,
                    onClick = {
                        VehicleStatusManager.setSelectedThemeIndex(index)
                        sharedPrefs.edit().putInt("pref_theme_index", index).apply()
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                )
            }
        }

        Button(
            onClick = {
                VehicleStatusManager.setSelectedThemeIndex(0)
                sharedPrefs.edit().putInt("pref_theme_index", 0).apply()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x13FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Reset to Defaults", color = Color.White)
        }
    }
}

@Composable
fun ColorIndicator(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
                .border(0.5.dp, Color.White, CircleShape)
        )
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSettingsTab(context: Context) {
    val sharedPrefs = remember { context.getSharedPreferences("van_prefs", Context.MODE_PRIVATE) }
    val volume by VehicleStatusManager.globalVolume.collectAsState()

    var currentPasscode by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf<String?>(null) }
    var passcodeSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF020617), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Volume Control
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "GLOBAL WARNING CHIME VOLUME: $volume%",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = volume.toFloat(),
                onValueChange = { newValue ->
                    VehicleStatusManager.setGlobalVolume(newValue.toInt())
                },
                onValueChangeFinished = {
                    sharedPrefs.edit().putInt("pref_global_volume", volume).apply()
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color.Gray
                )
            )
        }

        Divider(color = Color.White.copy(alpha = 0.08f))

        // Section: Passcode Modifier
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "DEVELOPER GATEWAY SECURITY",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = currentPasscode,
                onValueChange = {
                    currentPasscode = it
                    passcodeError = null
                    passcodeSuccess = false
                },
                label = { Text("Current Passcode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), focusedLabelColor = Color(0xFF38BDF8))
            )

            OutlinedTextField(
                value = newPasscode,
                onValueChange = {
                    newPasscode = it
                    passcodeError = null
                    passcodeSuccess = false
                },
                label = { Text("New Passcode (4-Digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), focusedLabelColor = Color(0xFF38BDF8))
            )

            OutlinedTextField(
                value = confirmPasscode,
                onValueChange = {
                    confirmPasscode = it
                    passcodeError = null
                    passcodeSuccess = false
                },
                label = { Text("Confirm New Passcode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), focusedLabelColor = Color(0xFF38BDF8))
            )

            if (passcodeError != null) {
                Text(text = passcodeError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
            }
            if (passcodeSuccess) {
                Text(text = "Passcode updated successfully!", color = Color(0xFF10B981), fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val savedPasscode = sharedPrefs.getString("pref_dev_passcode", "1234") ?: "1234"
                    if (currentPasscode != savedPasscode) {
                        passcodeError = "Current passcode is incorrect."
                    } else if (newPasscode.length != 4 || newPasscode.any { !it.isDigit() }) {
                        passcodeError = "New passcode must be exactly 4 digits."
                    } else if (newPasscode != confirmPasscode) {
                        passcodeError = "New passcodes do not match."
                    } else {
                        sharedPrefs.edit().putString("pref_dev_passcode", newPasscode).apply()
                        currentPasscode = ""
                        newPasscode = ""
                        confirmPasscode = ""
                        passcodeSuccess = true
                        passcodeError = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Passcode", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResourceMonitorTab(
    logs: List<String>,
    onClearLogs: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    context: Context
) {
    var tempSimVal by remember { mutableStateOf(24f) }

    fun injectSimTelemetry(stream: String) {
        val intent = Intent("com.van.status.SIMULATE_TELEMETRY").apply {
            putExtra("DATA_STREAM", stream)
        }
        context.sendBroadcast(intent)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Raw Ingest Log Console Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF020617), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .background(Color(0x13FFFFFF), shape = CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "Listening for live serial telemetry signals on USB link...\n(Use simulated panel below to mock signals)",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = ">>> $log",
                            color = if (log.contains("OPEN")) Color(0xFFF87171) else Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Simulated Signal Injector Matrix Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B).copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SIMULATED TELEMETRY SIGNAL INJECTOR",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { injectSimTelemetry("FL_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Mock FL Open", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { injectSimTelemetry("FR_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Mock FR Open", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { injectSimTelemetry("RL_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Mock RL Open", fontSize = 11.sp, color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { injectSimTelemetry("BACK_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Mock Back Open", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { injectSimTelemetry("FL_CLOSED|FR_CLOSED|RL_CLOSED|BACK_CLOSED") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Mock All Closed (Secure State)", fontSize = 11.sp, color = Color.White)
                }
            }

            // Temp mock slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Mock Temperature Shift: ${tempSimVal.toInt()}°C",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Slider(
                    value = tempSimVal,
                    onValueChange = { newValue ->
                        tempSimVal = newValue
                        injectSimTelemetry("TEMP:${newValue.toInt()}")
                    },
                    valueRange = 10f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8)
                    )
                )
            }
        }
    }
}
