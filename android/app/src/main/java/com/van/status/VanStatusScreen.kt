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
    isBackOpen: Boolean,
    cabinTemperature: Int,
    isBuzzerEnabled: Boolean,
    onToggleBuzzer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Blinking Alert Animations")
    val vehicleType by VehicleStatusManager.selectedVehicleType.collectAsState()

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

    var currentTemperatureTarget by remember { mutableStateOf(0) }
    LaunchedEffect(cabinTemperature) { currentTemperatureTarget = cabinTemperature }

    val animatedTemperature by animateIntAsState(
        targetValue = currentTemperatureTarget,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "Temperature Roll Animation"
    )

    var startEntranceAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startEntranceAnimation = true }

    val baseScale by animateFloatAsState(
        targetValue = if (startEntranceAnimation) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "Base Scale"
    )
    val baseAlpha by animateFloatAsState(
        targetValue = if (startEntranceAnimation) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "Base Alpha"
    )

    var startDoorsEntranceAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        startDoorsEntranceAnimation = true
    }

    val doorsScale by animateFloatAsState(
        targetValue = if (startDoorsEntranceAnimation) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "Doors Scale"
    )
    val doorsAlpha by animateFloatAsState(
        targetValue = if (startDoorsEntranceAnimation) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "Doors Alpha"
    )

    // Base Inversion Matrix
    val invertMatrix = remember {
        ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        ))
    }

    val darkSuvMatrix = remember {
        ColorMatrix(floatArrayOf(
            -0.25f,  0f,     0f,   0f,  255f * 0.25f,
              0f,    -0.25f,  0f,   0f,  255f * 0.25f,
              0f,     0f,    -0.25f,0f,  255f * 0.25f,
              0f,     0f,     0f,   1f,    0f
        ))
    }

    val alertColor = MaterialTheme.colorScheme.error
    val redVal   = alertColor.red   * 255f
    val greenVal = alertColor.green * 255f
    val blueVal  = alertColor.blue  * 255f

    val redGlowMatrix = remember(alertColor) {
        ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, redVal,
             0f, 0f, 0f, 0f, greenVal,
             0f, 0f, 0f, 0f, blueVal,
             0f, 0f, 0f, 1f, 0f
        ))
    }

    val crimsonMeshMatrix = remember(alertColor) {
        ColorMatrix(floatArrayOf(
            -(redVal / 255f), 0f, 0f, 0f, redVal,
              0f,             0f, 0f, 0f, greenVal,
              0f,             0f, 0f, 0f, blueVal,
              0f,             0f, 0f, 1f, 0f
        ))
    }

    val isSuv = (vehicleType == VehicleType.SUV_5_DOOR)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
    ) {

        // 1. Dial and Chassis Stack Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ui_circular_dial),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (isSuv) {
                    // ─── FORCE EXCLUSIVE LOADING OF SUV GRAPHICS ───
                    val suvBaseImg = ImageBitmap.imageResource(id = R.drawable.suv_base)
                    
                    val suvFlImg   = ImageBitmap.imageResource(id = if (isFlOpen) R.drawable.suv_fl_open   else R.drawable.suv_fl_closed)
                    val suvFrImg   = ImageBitmap.imageResource(id = if (isFrOpen) R.drawable.suv_fr_open   else R.drawable.suv_fr_closed)
                    val suvRlImg   = ImageBitmap.imageResource(id = if (isRlOpen) R.drawable.suv_rl_open   else R.drawable.suv_rl_closed)
                    val suvRrImg   = ImageBitmap.imageResource(id = if (isRrOpen) R.drawable.suv_rr_open   else R.drawable.suv_rr_closed)
                    val suvBackImg = ImageBitmap.imageResource(id = if (isBackOpen) R.drawable.suv_back_open else R.drawable.suv_back_closed)

                    // 1. Base Chassis
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = baseScale; scaleY = baseScale; alpha = baseAlpha }
                    ) {
                        drawImage(
                            image = suvBaseImg,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.colorMatrix(darkSuvMatrix),
                            blendMode = BlendMode.Screen
                        )
                    }

                    // 2. FL Door
                    if (isFlOpen) {
                        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp).graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }) {
                            drawImage(image = suvFlImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }) {
                            drawImage(image = suvFlImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }) {
                            drawImage(image = suvFlImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(darkSuvMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // 3. FR Door
                    if (isFrOpen) {
                        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp).graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }) {
                            drawImage(image = suvFrImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }) {
                            drawImage(image = suvFrImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }) {
                            drawImage(image = suvFrImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(darkSuvMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // 4. RL Door
                    if (isRlOpen) {
                        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp).graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }) {
                            drawImage(image = suvRlImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }) {
                            drawImage(image = suvRlImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }) {
                            drawImage(image = suvRlImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(darkSuvMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // 5. RR Door
                    if (isRrOpen) {
                        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp).graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }) {
                            drawImage(image = suvRrImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }) {
                            drawImage(image = suvRrImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }) {
                            drawImage(image = suvRrImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(darkSuvMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // 6. BACK Door
                    if (isBackOpen) {
                        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp).graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }) {
                            drawImage(image = suvBackImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }) {
                            drawImage(image = suvBackImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }) {
                            drawImage(image = suvBackImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = ColorFilter.colorMatrix(darkSuvMatrix), blendMode = BlendMode.Screen)
                        }
                    }
                } else {
                    // ─── LEGACY VAN DRAWING PATH ───
                    val baseImage = ImageBitmap.imageResource(id = R.drawable.base)
                    val flOpenImg = ImageBitmap.imageResource(id = R.drawable.front_left_open)
                    val flClosedImg = ImageBitmap.imageResource(id = R.drawable.front_left_closed)
                    val frOpenImg = ImageBitmap.imageResource(id = R.drawable.front_right_open)
                    val rlOpenImg = ImageBitmap.imageResource(id = R.drawable.rear_left_open)
                    val rlClosedImg = ImageBitmap.imageResource(id = R.drawable.rear_left_closed)
                    val backOpenImg = ImageBitmap.imageResource(id = R.drawable.back_open)

                    // Base chassis foundation wireframe
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = baseScale; scaleY = baseScale; alpha = baseAlpha }
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
                        Canvas(
                            modifier = Modifier.fillMaxSize().blur(8.dp)
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }
                        ) {
                            drawImage(image = flOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }
                        ) {
                            drawImage(image = flOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }
                        ) {
                            drawImage(image = flClosedImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(invertMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // --- FRONT RIGHT DOOR (FR) ---
                    if (isFrOpen) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().blur(8.dp)
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }
                        ) {
                            drawImage(image = frOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }
                        ) {
                            drawImage(image = frOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // --- REAR LEFT DOOR (RL) ---
                    if (isRlOpen) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().blur(8.dp)
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }
                        ) {
                            drawImage(image = rlOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }
                        ) {
                            drawImage(image = rlOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    } else {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha }
                        ) {
                            drawImage(image = rlClosedImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(invertMatrix), blendMode = BlendMode.Screen)
                        }
                    }

                    // --- BACK DOOR (BACK / TAILGATE) ---
                    if (isBackOpen) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().blur(8.dp)
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * 0.6f }
                        ) {
                            drawImage(image = backOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(redGlowMatrix), blendMode = BlendMode.Screen)
                        }
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = doorsScale; scaleY = doorsScale; alpha = doorsAlpha * alertAlpha }
                        ) {
                            drawImage(image = backOpenImg, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.colorMatrix(crimsonMeshMatrix), blendMode = BlendMode.Screen)
                        }
                    }
                }
            }
        }

        // 2. Glassmorphic Telemetry Dashboard Card (Lower-Left Corner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .background(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
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
                    text = "$animatedTemperature \u00b0C",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Buzzer Toggle Button (Top-Right)
        IconButton(
            onClick = onToggleBuzzer,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isBuzzerEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off
                ),
                contentDescription = "Toggle Audio Warnings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // 4. Settings Button (Top-Left)
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "System Settings",
                tint = MaterialTheme.colorScheme.primary,
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
        val primary  by VehicleStatusManager.customPrimaryColor.collectAsState()
        val alert    by VehicleStatusManager.customAlertColor.collectAsState()
        val bg       by VehicleStatusManager.customBackgroundColor.collectAsState()
        val surface  by VehicleStatusManager.customSurfaceColor.collectAsState()
        darkColorScheme(primary = primary, background = bg, surface = surface, error = alert)
    } else {
        val theme = VehicleStatusManager.ThemePalettes[themeIndex]
        darkColorScheme(
            primary    = theme.primaryColor,
            background = theme.backgroundColor,
            surface    = theme.surfaceColor,
            error      = theme.alertColor
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
