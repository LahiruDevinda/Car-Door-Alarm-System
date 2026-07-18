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
    val tabTitles = listOf("⚙️ Engineering Defaults", "🛠️ Settings", "📊 Resource Monitor")
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && activeTab == 2) listState.animateScrollToItem(logs.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Settings",
                tint = MaterialTheme.colorScheme.primary)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "DEVELOPER ENGINE PORTAL",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = (activeTab == index),
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (activeTab == index) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (activeTab) {
                    0 -> EngineeringDefaultsTab(context, logs)
                    1 -> SystemSettingsTab(context)
                    2 -> ResourceMonitorTab(logs = logs, onClearLogs = onClearLogs, listState = listState, context = context)
                }
            }
        }
    }
}

@Composable
fun EngineeringDefaultsTab(context: Context, logs: List<String>) {
    val sharedPrefs = remember { context.getSharedPreferences("van_prefs", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()
    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "ENGINEERING SYSTEM RECOVERY", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Text(
            text = "Triggering a system reset will restore all color settings to default, clear door filters, and re-enable volume levels to 100%.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Button(
            onClick = {
                VehicleStatusManager.resetAllStates()
                sharedPrefs.edit().clear().apply()
                // Reset developer passcode default to 1234
                sharedPrefs.edit().putString("pref_dev_passcode", "1234").apply()
            },
            colors = ButtonDefaults.buttonColors(containerColor = errorColor.copy(alpha = 0.10f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, errorColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Reset to Defaults", color = errorColor, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color.White.copy(alpha = 0.08f))

        Text(text = "SYSTEM OVERRIDE LOGS", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "No system logs active.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    listState.animateScrollToItem(logs.size - 1)
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(logs) { log ->
                        Text(
                            text = ">>> $log",
                            color = if (log.contains("OPEN")) errorColor else Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, shape = CircleShape).border(0.5.dp, Color.White, CircleShape))
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSettingsTab(context: Context) {
    val sharedPrefs = remember { context.getSharedPreferences("van_prefs", Context.MODE_PRIVATE) }
    val volume by VehicleStatusManager.globalVolume.collectAsState()
    val vehicleType by VehicleStatusManager.selectedVehicleType.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    var currentPasscode  by remember { mutableStateOf("") }
    var newPasscode      by remember { mutableStateOf("") }
    var confirmPasscode  by remember { mutableStateOf("") }
    var passcodeError    by remember { mutableStateOf<String?>(null) }
    var passcodeSuccess  by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Volume Control
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "GLOBAL WARNING CHIME VOLUME: $volume%", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = volume.toFloat(),
                onValueChange = { VehicleStatusManager.setGlobalVolume(it.toInt()) },
                onValueChangeFinished = { sharedPrefs.edit().putInt("pref_global_volume", volume).apply() },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary, inactiveTrackColor = Color.Gray)
            )
        }

        Divider(color = Color.White.copy(alpha = 0.08f))

        // Vehicle Profile Switcher
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "VEHICLE CHASSIS PROFILE", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Select the vehicle body type to bind the correct door telemetry wireframe. SUV assets will be used once added to drawable-nodpi/.",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // VAN option
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (vehicleType == VehicleType.VAN) primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (vehicleType == VehicleType.VAN) primary else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            VehicleStatusManager.setVehicleType(VehicleType.VAN)
                            sharedPrefs.edit().putString("pref_vehicle_type", "VAN").apply()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = vehicleType == VehicleType.VAN,
                        onClick = {
                            VehicleStatusManager.setVehicleType(VehicleType.VAN)
                            sharedPrefs.edit().putString("pref_vehicle_type", "VAN").apply()
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = primary)
                    )
                    Column {
                        Text("Van", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Cargo / Minivan", color = Color.Gray, fontSize = 10.sp)
                    }
                }

                // SUV_5_DOOR option
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (vehicleType == VehicleType.SUV_5_DOOR) primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (vehicleType == VehicleType.SUV_5_DOOR) primary else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            VehicleStatusManager.setVehicleType(VehicleType.SUV_5_DOOR)
                            sharedPrefs.edit().putString("pref_vehicle_type", "SUV_5_DOOR").apply()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = vehicleType == VehicleType.SUV_5_DOOR,
                        onClick = {
                            VehicleStatusManager.setVehicleType(VehicleType.SUV_5_DOOR)
                            sharedPrefs.edit().putString("pref_vehicle_type", "SUV_5_DOOR").apply()
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = primary)
                    )
                    Column {
                        Text("5-Door SUV", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Assets pending", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f))

        // Passcode Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "DEVELOPER GATEWAY SECURITY", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = currentPasscode,
                onValueChange = { currentPasscode = it; passcodeError = null; passcodeSuccess = false },
                label = { Text("Current Passcode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, focusedLabelColor = primary)
            )
            OutlinedTextField(
                value = newPasscode,
                onValueChange = { newPasscode = it; passcodeError = null; passcodeSuccess = false },
                label = { Text("New Passcode (4-Digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, focusedLabelColor = primary)
            )
            OutlinedTextField(
                value = confirmPasscode,
                onValueChange = { confirmPasscode = it; passcodeError = null; passcodeSuccess = false },
                label = { Text("Confirm New Passcode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, focusedLabelColor = primary)
            )

            if (passcodeError != null) Text(text = passcodeError!!, color = errorColor, fontSize = 12.sp)
            if (passcodeSuccess) Text(text = "Passcode updated successfully!", color = Color(0xFF10B981), fontSize = 12.sp)

            Button(
                onClick = {
                    val saved = sharedPrefs.getString("pref_dev_passcode", "1234") ?: "1234"
                    when {
                        currentPasscode != saved                                  -> passcodeError = "Current passcode is incorrect."
                        newPasscode.length != 4 || newPasscode.any { !it.isDigit() } -> passcodeError = "New passcode must be exactly 4 digits."
                        newPasscode != confirmPasscode                            -> passcodeError = "New passcodes do not match."
                        else -> {
                            sharedPrefs.edit().putString("pref_dev_passcode", newPasscode).apply()
                            currentPasscode = ""; newPasscode = ""; confirmPasscode = ""
                            passcodeSuccess = true; passcodeError = null
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Passcode", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
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
    val primary    = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    var tempSimVal by remember { mutableStateOf(24f) }

    fun injectSimTelemetry(stream: String) {
        val intent = Intent("com.van.status.SIMULATE_TELEMETRY").apply { putExtra("DATA_STREAM", stream) }
        context.sendBroadcast(intent)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Raw Log Console
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.background(errorColor.copy(alpha = 0.10f), shape = CircleShape).size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs",
                        tint = errorColor, modifier = Modifier.size(18.dp))
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "Listening for live serial telemetry signals on USB link...\n(Use simulated panel below to mock signals)",
                    color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(logs) { log ->
                        Text(
                            text = ">>> $log",
                            color = if (log.contains("OPEN")) errorColor else Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Simulated Signal Injector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                .border(1.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "SIMULATED TELEMETRY SIGNAL INJECTOR", color = primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { injectSimTelemetry("FL_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp)) {
                    Text("Mock FL Open", fontSize = 11.sp, color = Color.White)
                }
                Button(onClick = { injectSimTelemetry("FR_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp)) {
                    Text("Mock FR Open", fontSize = 11.sp, color = Color.White)
                }
                Button(onClick = { injectSimTelemetry("RL_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp)) {
                    Text("Mock RL Open", fontSize = 11.sp, color = Color.White)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { injectSimTelemetry("BACK_OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp)) {
                    Text("Mock Back Open", fontSize = 11.sp, color = Color.White)
                }
                Button(onClick = { injectSimTelemetry("FL_CLOSED|FR_CLOSED|RL_CLOSED|BACK_CLOSED") },
                    colors = ButtonDefaults.buttonColors(containerColor = primary.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(2f), shape = RoundedCornerShape(6.dp)) {
                    Text("Mock All Closed (Secure State)", fontSize = 11.sp, color = Color.White)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Mock Temperature Shift: ${tempSimVal.toInt()}\u00b0C", color = Color.LightGray, fontSize = 11.sp)
                Slider(
                    value = tempSimVal,
                    onValueChange = { newValue -> tempSimVal = newValue; injectSimTelemetry("TEMP:${newValue.toInt()}") },
                    valueRange = 10f..50f,
                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary)
                )
            }
        }
    }
}
