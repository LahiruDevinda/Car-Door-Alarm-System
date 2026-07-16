package com.van.status

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingPreview by remember { mutableStateOf<String?>(null) }

    var showPasscodeDialog by remember { mutableStateOf(false) }
    var enteredPasscode by remember { mutableStateOf("") }
    var isPasscodeError by remember { mutableStateOf(false) }

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

    // Passcode Verification Challenge Dialog
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (enteredPasscode == "1234") {
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
