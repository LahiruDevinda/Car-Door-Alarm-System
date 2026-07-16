package com.van.status

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    backEnabled: Boolean,
    chimeSelection: String,
    onToggleFl: (Boolean) -> Unit,
    onToggleFr: (Boolean) -> Unit,
    onToggleRl: (Boolean) -> Unit,
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
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

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
                    colors = listOf(Color(0xFF1E293B), Color(0xFF09090B))
                )
            )
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
                contentDescription = "Go Back",
                tint = Color.White
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
                text = "SYSTEM SETTINGS",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Door Telemetry Filters
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "DOOR ALARM CHANNELS",
                        color = Color(0xFF38BDF8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    DoorFilterRow("Front Left (FL)", flEnabled, onToggleFl)
                    DoorFilterRow("Front Right (FR)", frEnabled, onToggleFr)
                    DoorFilterRow("Rear Left (RL)", rlEnabled, onToggleRl)
                    DoorFilterRow("Back Hatch (BACK)", backEnabled, onToggleBack)
                }

                // Right Panel: Warning Chime Select & Previews
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "WARNING CHIME SOUND",
                        color = Color(0xFF38BDF8),
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

            // ── Custom Theme Builder Section (outside developer gate) ──
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
                            enteredPasscode = it
                            isPasscodeError = false
                        },
                        label = { Text("Passcode") },
                        isError = isPasscodeError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            focusedLabelColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color.Gray
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

                    // Exterior Factory Reset trigger: opens double-confirmation dialog
                    TextButton(
                        onClick = {
                            showPasscodeDialog = false
                            enteredPasscode = ""
                            isPasscodeError = false
                            showResetConfirmationDialog = true
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
                TextButton(
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
                    }
                ) {
                    Text("Verify", color = Color(0xFFEF4444))
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
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // ── Double-Confirmation Factory Reset Dialog ──
    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = {
                Text(text = "\u26A0\uFE0F Factory Reset Confirmation", color = Color(0xFFEF4444))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action will permanently:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\u2022 Erase all custom color wheel theme configurations\n" +
                                "\u2022 Reset the developer passcode to '1234'\n" +
                                "\u2022 Restore global volume to 100%\n" +
                                "\u2022 Clear all door channel preferences\n" +
                                "\u2022 Revert to the Default Red Warning theme\n" +
                                "\u2022 Restart the application",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Divider(color = Color(0xFFEF4444).copy(alpha = 0.4f))
                    Text(
                        text = "This action cannot be undone.",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmationDialog = false
                        // Deep system wipe
                        sharedPrefs.edit().clear().apply()
                        sharedPrefs.edit().putString("pref_dev_passcode", "1234").apply()
                        VehicleStatusManager.resetAllStates()
                        // Cold restart
                        val packageManager = context.packageManager
                        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                        val componentName = intent?.component
                        val restartIntent = android.content.Intent.makeRestartActivityTask(componentName)
                        context.startActivity(restartIntent)
                        Runtime.getRuntime().exit(0)
                    }
                ) {
                    Text("Confirm Full Reset", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ── Custom Theme Builder (accessible without passcode) ──
@Composable
fun CustomThemeBuilder(sharedPrefs: android.content.SharedPreferences) {
    val themeIndex by VehicleStatusManager.selectedThemeIndex.collectAsState()
    val isCustomActive = themeIndex >= VehicleStatusManager.ThemePalettes.size

    val customPrimary by VehicleStatusManager.customPrimaryColor.collectAsState()
    val customAlert by VehicleStatusManager.customAlertColor.collectAsState()
    val customBg by VehicleStatusManager.customBackgroundColor.collectAsState()
    val customSurface by VehicleStatusManager.customSurfaceColor.collectAsState()

    // Which color slot we are editing: 0=Primary, 1=Alert, 2=Surface, 3=Background
    var editingSlot by remember { mutableIntStateOf(0) }

    val currentEditColor = when (editingSlot) {
        0 -> customPrimary
        1 -> customAlert
        2 -> customSurface
        else -> customBg
    }

    // HSV state derived from current editing color
    var hue by remember { mutableFloatStateOf(200f) }
    var saturation by remember { mutableFloatStateOf(0.8f) }
    var brightness by remember { mutableFloatStateOf(0.9f) }

    // Sync HSV when slot changes
    LaunchedEffect(editingSlot) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentEditColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
    }

    fun applyColorChange(h: Float, s: Float, v: Float) {
        val color = Color.hsv(h, s, v)
        when (editingSlot) {
            0 -> {
                VehicleStatusManager.setCustomPrimaryColor(color)
                sharedPrefs.edit().putInt("custom_primary_color", color.toArgb()).apply()
            }
            1 -> {
                VehicleStatusManager.setCustomAlertColor(color)
                sharedPrefs.edit().putInt("custom_alert_color", color.toArgb()).apply()
            }
            2 -> {
                VehicleStatusManager.setCustomSurfaceColor(color)
                sharedPrefs.edit().putInt("custom_surface_color", color.toArgb()).apply()
            }
            3 -> {
                VehicleStatusManager.setCustomBackgroundColor(color)
                sharedPrefs.edit().putInt("custom_bg_color", color.toArgb()).apply()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row with title and activation radio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CUSTOM THEME BUILDER",
                color = Color(0xFF38BDF8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My Custom Theme",
                    color = if (isCustomActive) Color(0xFF38BDF8) else Color.LightGray,
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
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Color Wheel
            ColorWheelCanvas(
                hue = hue,
                saturation = saturation,
                onHueSatChanged = { h, s ->
                    hue = h
                    saturation = s
                    applyColorChange(h, s, brightness)
                },
                modifier = Modifier.size(150.dp)
            )

            // Right: Controls
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Editing:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                val slotLabels = listOf("Primary Accent", "Alert Color", "Surface Color", "Background")
                val slotColors = listOf(customPrimary, customAlert, customSurface, customBg)

                slotLabels.forEachIndexed { idx, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingSlot = idx }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = editingSlot == idx,
                            onClick = { editingSlot = idx },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8)),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (editingSlot == idx) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(slotColors[idx], CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Brightness slider
                Text(
                    text = "Brightness: ${(brightness * 100).toInt()}%",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        applyColorChange(hue, saturation, it)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.height(24.dp)
                )

                // Preview strip
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
        val pointerDist = saturation * radius
        val px = center.x + cos(pointerAngle) * pointerDist
        val py = center.y + sin(pointerAngle) * pointerDist
        val pointerCenter = Offset(px, py)

        drawCircle(color = Color.Black, radius = 9f, center = pointerCenter)
        drawCircle(color = Color.White, radius = 9f, center = pointerCenter, style = Stroke(width = 2.5f))
    }
}

private fun computeHueSat(x: Float, y: Float, width: Int, height: Int): Pair<Float, Float> {
    val cx = width / 2f
    val cy = height / 2f
    val dx = x - cx
    val dy = y - cy
    val radius = minOf(width, height) / 2f
    val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
    val dist = sqrt(dx * dx + dy * dy)
    val sat = (dist / radius).coerceIn(0f, 1f)
    return Pair(angle, sat)
}

@Composable
fun DoorFilterRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF23C8D8),
                checkedTrackColor = Color(0xFF251616)
            )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = { onSelect(value) },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF23C8D8))
            )
            Text(
                text = name,
                color = if (isPreviewPlaying) Color(0xFF23C8D8) else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isPreviewPlaying) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        IconButton(
            onClick = onPlayPreview,
            modifier = Modifier
                .background(Color(0x13FFFFFF), shape = CircleShape)
                .size(36.dp)
        ) {
            Icon(
                imageVector = if (isPreviewPlaying) CustomStopIcon else Icons.Default.PlayArrow,
                contentDescription = if (isPreviewPlaying) "Stop Preview" else "Play Preview",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
