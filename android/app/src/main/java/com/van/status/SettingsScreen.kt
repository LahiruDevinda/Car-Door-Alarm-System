package com.van.status

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlin.math.*

// Custom Stop vector icon drawn programmatically for cross-platform dependency resilience
val CustomStopIcon: ImageVector = ImageVector.Builder(
    name = "Stop",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).path(
    fill = SolidColor(Color.White)
) {
    moveTo(6f, 6f)
    horizontalLineTo(18f)
    verticalLineTo(18f)
    horizontalLineTo(6f)
    close()
}.build()

@Composable
fun SettingsScreen(
    flEnabled: Boolean,
    frEnabled: Boolean,
    rlEnabled: Boolean,
    rrEnabled: Boolean,
    backEnabled: Boolean,
    chimeSelection: String,
    onToggleFl: (Boolean) -> Unit,
    onToggleFr: (Boolean) -> Unit,
    onToggleRl: (Boolean) -> Unit,
    onToggleRr: (Boolean) -> Unit,
    onToggleBack: (Boolean) -> Unit,
    onSelectChime: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("van_prefs", android.content.Context.MODE_PRIVATE) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingPreview by remember { mutableStateOf<String?>(null) }

    var showPasscodeDialog by remember { mutableStateOf(false) }
    var enteredPasscode by remember { mutableStateOf("") }
    var isPasscodeError by remember { mutableStateOf(false) }

    val vehicleType by VehicleStatusManager.selectedVehicleType.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    // Helper function to stop current playing audio and play new resource
    fun playChimePreview(resId: Int, chimeKey: String) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()

            if (currentlyPlayingPreview == chimeKey) {
                currentlyPlayingPreview = null
                mediaPlayer = null
                return
            }

            currentlyPlayingPreview = chimeKey
            mediaPlayer = MediaPlayer.create(context, resId).apply {
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == this) {
                        mediaPlayer = null
                        currentlyPlayingPreview = null
                    }
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentlyPlayingPreview = null
        }
    }

    // Clean up MediaPlayer on disposal
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            .padding(24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(primary.copy(alpha = 0.10f), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Go Back",
                tint = primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "SYSTEM CONFIGURATION",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Door Telemetry Filters
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "DOOR ALARM CHANNELS",
                        color = primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    DoorFilterRow("Front Left (FL)", flEnabled, onToggleFl)
                    DoorFilterRow("Front Right (FR)", frEnabled, onToggleFr)
                    DoorFilterRow("Rear Left (RL)", rlEnabled, onToggleRl)
                    
                    if (vehicleType == VehicleType.SUV_5_DOOR) {
                        DoorFilterRow("Rear Right (RR)", rrEnabled, onToggleRr)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().alpha(0.35f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Rear Right (RR)", color = Color.Gray, fontSize = 14.sp)
                            Text(text = "Not Available", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    DoorFilterRow("Back Hatch (BACK)", backEnabled, onToggleBack)
                }

                // Right Panel: Warning Chime Select & Previews
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "WARNING CHIME SOUND",
                        color = primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ChimeSelectRow(
                        name = "Alert Chime One",
                        value = "audi_chime",
                        selected = (chimeSelection == "audi_chime"),
                        isPreviewPlaying = (currentlyPlayingPreview == "audi_chime"),
                        onSelect = onSelectChime,
                        onPlayPreview = { playChimePreview(R.raw.audi_chime, "audi_chime") }
                    )

                    ChimeSelectRow(
                        name = "Alert Chime Two",
                        value = "chime_two",
                        selected = (chimeSelection == "chime_two"),
                        isPreviewPlaying = (currentlyPlayingPreview == "chime_two"),
                        onSelect = onSelectChime,
                        onPlayPreview = { playChimePreview(R.raw.chime_two, "chime_two") }
                    )

                    ChimeSelectRow(
                        name = "Alert Chime Three",
                        value = "chime_three",
                        selected = (chimeSelection == "chime_three"),
                        isPreviewPlaying = (currentlyPlayingPreview == "chime_three"),
                        onSelect = onSelectChime,
                        onPlayPreview = { playChimePreview(R.raw.chime_three, "chime_three") }
                    )

                    ChimeSelectRow(
                        name = "Alert Chime Four",
                        value = "chime_four",
                        selected = (chimeSelection == "chime_four"),
                        isPreviewPlaying = (currentlyPlayingPreview == "chime_four"),
                        onSelect = onSelectChime,
                        onPlayPreview = { playChimePreview(R.raw.alert_chime_4, "chime_four") }
                    )
                }
            }

            // ── Vehicle Type Selection Section (un-gated) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "VEHICLE TYPE",
                    color = primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Vehicle Type Card: VAN
                    VehicleTypeCard(
                        title = "4-Door Van",
                        doorsText = "4 Status Sensors (Rear-Right Door Disabled)",
                        isSelected = (vehicleType == VehicleType.VAN),
                        onClick = {
                            VehicleStatusManager.setVehicleType(VehicleType.VAN)
                            sharedPrefs.edit().putString("pref_vehicle_type", "VAN").apply()
                        },
                        imageResId = R.drawable.base,
                        primaryColor = primary,
                        modifier = Modifier.weight(1f)
                    )

                    // Vehicle Type Card: SUV_5_DOOR
                    VehicleTypeCard(
                        title = "5-Door SUV",
                        doorsText = "5 Status Sensors (Includes Tailgate)",
                        isSelected = (vehicleType == VehicleType.SUV_5_DOOR),
                        onClick = {
                            VehicleStatusManager.setVehicleType(VehicleType.SUV_5_DOOR)
                            sharedPrefs.edit().putString("pref_vehicle_type", "SUV_5_DOOR").apply()
                        },
                        imageResId = R.drawable.suv_base,
                        primaryColor = primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Theme Profiles Selection Section (un-gated) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "THEME PROFILES",
                    color = primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                val themeIndex by VehicleStatusManager.selectedThemeIndex.collectAsState()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..2) {
                            val palette = VehicleStatusManager.ThemePalettes[i]
                            val isSelected = (themeIndex == i)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) primary else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        VehicleStatusManager.setSelectedThemeIndex(i)
                                        sharedPrefs.edit().putInt("pref_theme_index", i).apply()
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(palette.primaryColor, shape = CircleShape)
                                            .border(0.5.dp, Color.White, CircleShape)
                                    )
                                    Text(
                                        text = palette.name.replace(" Warning", "").replace(" Neon", ""),
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 3..4) {
                            val palette = VehicleStatusManager.ThemePalettes[i]
                            val isSelected = (themeIndex == i)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) primary else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        VehicleStatusManager.setSelectedThemeIndex(i)
                                        sharedPrefs.edit().putInt("pref_theme_index", i).apply()
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(palette.primaryColor, shape = CircleShape)
                                            .border(0.5.dp, Color.White, CircleShape)
                                    )
                                    Text(
                                        text = palette.name.replace(" Warning", "").replace(" Neon", ""),
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Theme Builder Section (outside developer gate)
            CustomThemeBuilder(sharedPrefs = sharedPrefs)

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Options Diagnostics row
            Button(
                onClick = { showPasscodeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x13FFFFFF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "System Diagnostics Console (Developer Only)",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // ── Passcode Verification Challenge Dialog ──
    if (showPasscodeDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasscodeDialog = false
                enteredPasscode = ""
                isPasscodeError = false
            },
            title = {
                Text(text = "Developer Verification Challenge", color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the 4-digit security code to access diagnostics terminal logs:",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = enteredPasscode,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                enteredPasscode = it
                            }
                        },
                        label = { Text("Enter Passcode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = primary,
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    if (isPasscodeError) {
                        Text(
                            text = "Invalid passcode. Access Denied.",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Exterior Factory Reset: accessible without authentication
                    TextButton(
                        onClick = {
                            showPasscodeDialog = false
                            enteredPasscode = ""
                            isPasscodeError = false
                            // Wipe all preferences
                            sharedPrefs.edit().clear().apply()
                            // Revert passcode to default
                            sharedPrefs.edit().putString("pref_dev_passcode", "1234").apply()
                            // Reset all state flows
                            VehicleStatusManager.resetAllStates()
                            // Cold restart the app
                            val packageManager = context.packageManager
                            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                            val componentName = intent?.component
                            val restartIntent = android.content.Intent.makeRestartActivityTask(componentName)
                            context.startActivity(restartIntent)
                            Runtime.getRuntime().exit(0)
                        }
                    ) {
                        Text(
                            text = "Forgot Password? Reset App",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedPasscode = sharedPrefs.getString("pref_dev_passcode", "1234") ?: "1234"
                        if (enteredPasscode == savedPasscode) {
                            showPasscodeDialog = false
                            enteredPasscode = ""
                            isPasscodeError = false
                            onNavigateToDiagnostics()
                        } else {
                            enteredPasscode = ""
                            isPasscodeError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text("Verify Access", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasscodeDialog = false
                        enteredPasscode = ""
                        isPasscodeError = false
                    }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun CustomThemeBuilder(sharedPrefs: android.content.SharedPreferences) {
    val primary = MaterialTheme.colorScheme.primary
    val themeIndex by VehicleStatusManager.selectedThemeIndex.collectAsState()
    val isCustomActive = themeIndex >= VehicleStatusManager.ThemePalettes.size

    val customPrimary by VehicleStatusManager.customPrimaryColor.collectAsState()
    val customAlert   by VehicleStatusManager.customAlertColor.collectAsState()
    val customBg      by VehicleStatusManager.customBackgroundColor.collectAsState()
    val customSurface by VehicleStatusManager.customSurfaceColor.collectAsState()

    var editingSlot by remember { mutableIntStateOf(0) }

    val currentEditColor = when (editingSlot) {
        0 -> customPrimary
        1 -> customAlert
        2 -> customSurface
        else -> customBg
    }

    var hue        by remember { mutableFloatStateOf(200f) }
    var saturation by remember { mutableFloatStateOf(0.8f) }
    var brightness by remember { mutableFloatStateOf(0.9f) }

    LaunchedEffect(editingSlot) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentEditColor.toArgb(), hsv)
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
    }

    fun applyColorChange(h: Float, s: Float, v: Float) {
        val color = Color.hsv(h, s, v)
        when (editingSlot) {
            0 -> { VehicleStatusManager.setCustomPrimaryColor(color);    sharedPrefs.edit().putInt("custom_primary_color", color.toArgb()).apply() }
            1 -> { VehicleStatusManager.setCustomAlertColor(color);      sharedPrefs.edit().putInt("custom_alert_color",   color.toArgb()).apply() }
            2 -> { VehicleStatusManager.setCustomSurfaceColor(color);    sharedPrefs.edit().putInt("custom_surface_color", color.toArgb()).apply() }
            3 -> { VehicleStatusManager.setCustomBackgroundColor(color); sharedPrefs.edit().putInt("custom_bg_color",      color.toArgb()).apply() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
            .border(1.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "CUSTOM THEME BUILDER", color = primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My Custom Theme",
                    color = if (isCustomActive) primary else Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(4.dp))
                RadioButton(
                    selected = isCustomActive,
                    onClick = {
                        val customIndex = VehicleStatusManager.ThemePalettes.size
                        VehicleStatusManager.setSelectedThemeIndex(customIndex)
                        sharedPrefs.edit().putInt("pref_theme_index", customIndex).apply()
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = primary)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ColorWheelCanvas(
                hue = hue, saturation = saturation,
                onHueSatChanged = { h, s -> hue = h; saturation = s; applyColorChange(h, s, brightness) },
                modifier = Modifier.size(150.dp)
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Editing:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                val slotLabels = listOf("Primary Accent", "Alert Color", "Surface Color", "Background")
                val slotColors = listOf(customPrimary, customAlert, customSurface, customBg)

                slotLabels.forEachIndexed { idx, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { editingSlot = idx }.padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = editingSlot == idx,
                            onClick = { editingSlot = idx },
                            colors = RadioButtonDefaults.colors(selectedColor = primary),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (editingSlot == idx) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.size(18.dp).background(slotColors[idx], CircleShape).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Brightness: ${(brightness * 100).toInt()}%", color = Color.LightGray, fontSize = 11.sp)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it; applyColorChange(hue, saturation, it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                    modifier = Modifier.height(24.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preview:", color = Color.LightGray, fontSize = 10.sp)
                    slotColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(c, RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

// ── Interactive HSV Color Wheel ──
@Composable
fun ColorWheelCanvas(
    hue: Float,
    saturation: Float,
    onHueSatChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val (h, s) = computeHueSat(down.position.x, down.position.y, size.width, size.height)
                    onHueSatChanged(h, s)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val (h2, s2) = computeHueSat(change.position.x, change.position.y, size.width, size.height)
                        onHueSatChanged(h2, s2)
                        change.consume()
                    }
                }
            }
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Hue sweep gradient
        val hueColors = (0..360 step 10).map { Color.hsv(it.toFloat(), 1f, 1f) }
        drawCircle(
            brush = Brush.sweepGradient(colors = hueColors, center = center),
            radius = radius,
            center = center
        )

        // Saturation radial gradient (white center → transparent edge)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Selection pointer
        val pointerAngle = hue * PI.toFloat() / 180f
        val pointerDist  = saturation * radius
        val px = center.x + cos(pointerAngle) * pointerDist
        val py = center.y + sin(pointerAngle) * pointerDist
        val pointerCenter = Offset(px, py)

        drawCircle(color = Color.Black, radius = 9f, center = pointerCenter)
        drawCircle(color = Color.White, radius = 9f, center = pointerCenter, style = Stroke(width = 2.5f))
    }
}

private fun computeHueSat(x: Float, y: Float, width: Int, height: Int): Pair<Float, Float> {
    val cx = width / 2f; val cy = height / 2f
    val dx = x - cx;     val dy  = y - cy
    val radius = minOf(width, height) / 2f
    val angle  = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
    val dist   = sqrt(dx * dx + dy * dy)
    return Pair(angle, (dist / radius).coerceIn(0f, 1f))
}

@Composable
fun DoorFilterRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = primary, checkedTrackColor = primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun ChimeSelectRow(
    name: String,
    value: String,
    selected: Boolean,
    isPreviewPlaying: Boolean,
    onSelect: (String) -> Unit,
    onPlayPreview: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(
                selected = selected,
                onClick = { onSelect(value) },
                colors = RadioButtonDefaults.colors(selectedColor = primary)
            )
            Text(
                text = name,
                color = if (isPreviewPlaying) primary else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isPreviewPlaying) FontWeight.Bold else FontWeight.Normal
            )
        }
        IconButton(
            onClick = onPlayPreview,
            modifier = Modifier.background(primary.copy(alpha = 0.10f), shape = CircleShape).size(36.dp)
        ) {
            Icon(
                imageVector = if (isPreviewPlaying) CustomStopIcon else Icons.Default.PlayArrow,
                contentDescription = if (isPreviewPlaying) "Stop Preview" else "Play Preview",
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun VehicleTypeCard(
    title: String,
    doorsText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    imageResId: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val invertMatrix = remember {
        ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        ))
    }

    Column(
        modifier = modifier
            .background(
                if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) primaryColor else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Image(
            painter = painterResource(id = imageResId),
            contentDescription = title,
            colorFilter = ColorFilter.colorMatrix(invertMatrix),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(),
            alignment = Alignment.Center
        )

        Text(
            text = doorsText,
            color = if (isSelected) primaryColor else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
