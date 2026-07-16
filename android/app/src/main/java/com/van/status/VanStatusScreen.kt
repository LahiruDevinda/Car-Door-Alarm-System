package com.van.status

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VanStatusScreen(
    isFlOpen: Boolean,
    isFrOpen: Boolean,
    isRlOpen: Boolean,
    isRrOpen: Boolean,
    cabinTemperature: Int,
    isBuzzerEnabled: Boolean,
    onToggleBuzzer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Blinking Alert Animations")
    
    // alertAlpha: Animate linearly from 0.2f to 1.0f inside a quick 400ms cycle for warnings
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Rapid Alert Alpha"
    )

    // Anchor variable initialized to 0 to capture the launch roll-up effect
    var currentTemperatureTarget by remember { mutableStateOf(0) }

    // Listen for incoming telemetry updates to advance the target forward
    LaunchedEffect(cabinTemperature) {
        currentTemperatureTarget = cabinTemperature
    }

    // rolling temperature transition animation
    val animatedTemperature by animateIntAsState(
        targetValue = currentTemperatureTarget,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "Temperature Roll Animation"
    )

    // Staggered Entrance Animations: triggered on initialization
    var startEntranceAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startEntranceAnimation = true
    }

    // 1. Base chassis scale & alpha (instantly on launch)
    val baseScale by animateFloatAsState(
        targetValue = if (startEntranceAnimation) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Base Scale"
    )
    val baseAlpha by animateFloatAsState(
        targetValue = if (startEntranceAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Base Alpha"
    )

    // 2. Doors scale & alpha (staggered with a 150ms delay)
    var startDoorsEntranceAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        startDoorsEntranceAnimation = true
    }

    val doorsScale by animateFloatAsState(
        targetValue = if (startDoorsEntranceAnimation) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Doors Scale"
    )
    val doorsAlpha by animateFloatAsState(
        targetValue = if (startDoorsEntranceAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Doors Alpha"
    )

    // Base Inversion Matrix: White background -> Black, dark lines -> Cyber white/gray lines
    val invertMatrix = remember {
        ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        ))
    }

    val alertColor = MaterialTheme.colorScheme.error
    val redVal = alertColor.red * 255f
    val greenVal = alertColor.green * 255f
    val blueVal = alertColor.blue * 255f

    // Red Glow Matrix: White background -> Black, dark lines -> Dynamic Alert Color
    val redGlowMatrix = remember(alertColor) {
        ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, redVal, // Red
             0f,  0f,  0f, 0f, greenVal, // Green
             0f,  0f,  0f, 0f, blueVal, // Blue
             0f,  0f,  0f, 1f,   0f  // Alpha
        ))
    }

    // Crimson Mesh Matrix: White background -> Black, dark lines -> Dynamic Alert Color
    val crimsonMeshMatrix = remember(alertColor) {
        ColorMatrix(floatArrayOf(
            -(redVal / 255f),  0f,  0f, 0f, redVal,
              0f,             0f,  0f, 0f, greenVal,
              0f,             0f,  0f, 0f, blueVal,
              0f,             0f,  0f, 1f,   0f
        ))
    }

    // Load the 3D local image bitmaps
    val baseImage = ImageBitmap.imageResource(id = R.drawable.base)
    val flClosedImage = ImageBitmap.imageResource(id = R.drawable.front_left_closed)
    val flOpenImage = ImageBitmap.imageResource(id = R.drawable.front_left_open)
    val frOpenImage = ImageBitmap.imageResource(id = R.drawable.front_right_open)
    val rlClosedImage = ImageBitmap.imageResource(id = R.drawable.rear_left_closed)
    val rlOpenImage = ImageBitmap.imageResource(id = R.drawable.rear_left_open)
    val backOpenImage = ImageBitmap.imageResource(id = R.drawable.back_open)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            ) // Minimal slate gray to absolute black gradient core directly behind the van
    ) {
        
        // (Floor grid entirely removed from background overlays)

        // 1. Dial and Chassis Stack Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dial Graphic Layer
            Image(
                painter = painterResource(id = R.drawable.ui_circular_dial),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            // Central 3D Chassis stack box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Base chassis foundation wireframe Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = baseScale
                            scaleY = baseScale
                            alpha = baseAlpha
                        }
                ) {
                    drawImage(
                        image = baseImage,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        colorFilter = ColorFilter.colorMatrix(invertMatrix),
                        blendMode = BlendMode.Screen
                    )
                }

                // --- FRONT LEFT DOOR (FL) ---
                if (isFlOpen) {
                    // Sub-Layer A: Aura Glow
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(8.dp)
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * 0.6f
                            }
                    ) {
                        drawImage(
                            image = flOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(redGlowMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                    // Sub-Layer B: Blinking Mesh Line Overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * alertAlpha
                            }
                    ) {
                        drawImage(
                            image = flOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                } else {
                    // Closed State Canvas layer
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha
                            }
                    ) {
                        drawImage(
                            image = flClosedImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(invertMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                }

                // --- FRONT RIGHT DOOR (FR) ---
                if (isFrOpen) {
                    // Sub-Layer A: Aura Glow
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(8.dp)
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * 0.6f
                            }
                    ) {
                        drawImage(
                            image = frOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(redGlowMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                    // Sub-Layer B: Blinking Mesh Line Overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * alertAlpha
                            }
                    ) {
                        drawImage(
                            image = frOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
                // (If closed, it lets BASE show through natively)

                // --- REAR LEFT DOOR (RL) ---
                if (isRlOpen) {
                    // Sub-Layer A: Aura Glow
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(8.dp)
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * 0.6f
                            }
                    ) {
                        drawImage(
                            image = rlOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(redGlowMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                    // Sub-Layer B: Blinking Mesh Line Overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * alertAlpha
                            }
                    ) {
                        drawImage(
                            image = rlOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                } else {
                    // Closed State Canvas layer
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha
                            }
                    ) {
                        drawImage(
                            image = rlClosedImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(invertMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                }

                // --- REAR RIGHT / BACK DOOR (RR) ---
                if (isRrOpen) {
                    // Sub-Layer A: Aura Glow
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(8.dp)
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * 0.6f
                            }
                    ) {
                        drawImage(
                            image = backOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(redGlowMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                    // Sub-Layer B: Blinking Mesh Line Overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = doorsScale
                                scaleY = doorsScale
                                alpha = doorsAlpha * alertAlpha
                            }
                    ) {
                        drawImage(
                            image = backOpenImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
                // (If closed, it lets BASE show through natively)
            }
        }

        // 2. Glassmorphic Telemetry Dashboard Card (Lower-Left Corner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .background(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_thermometer),
                    contentDescription = "Cabin Temperature",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "$animatedTemperature °C",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Interactive Control Overlay: Floating Top-Right Corner Volume Toggle Button
        IconButton(
            onClick = onToggleBuzzer,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Color(0x13FFFFFF), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isBuzzerEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off
                ),
                contentDescription = "Toggle Audio Warnings",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Floating Top-Left Corner Settings Button
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(Color(0x13FFFFFF), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "System Settings",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun VanStatusTheme(content: @Composable () -> Unit) {
    val themeIndex by VehicleStatusManager.selectedThemeIndex.collectAsState()
    val colorScheme = if (themeIndex >= VehicleStatusManager.ThemePalettes.size) {
        // Custom user palette
        val primary by VehicleStatusManager.customPrimaryColor.collectAsState()
        val alert by VehicleStatusManager.customAlertColor.collectAsState()
        val bg by VehicleStatusManager.customBackgroundColor.collectAsState()
        val surface by VehicleStatusManager.customSurfaceColor.collectAsState()
        darkColorScheme(primary = primary, background = bg, surface = surface, error = alert)
    } else {
        val theme = VehicleStatusManager.ThemePalettes[themeIndex]
        darkColorScheme(
            primary = theme.primaryColor,
            background = theme.backgroundColor,
            surface = theme.surfaceColor,
            error = theme.alertColor
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
