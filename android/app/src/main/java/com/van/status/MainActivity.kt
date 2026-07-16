package com.van.status

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    private val appReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.van.status.UPDATE_UI_LOGS" -> {
                    val logLine = intent.getStringExtra("LOG_LINE")
                    if (logLine != null) {
                        VehicleStatusManager.addDiagnosticLog(logLine)
                    }
                }
                "com.van.status.AUTO_CLOSE_APP" -> {
                    // 1. Forcefully strip lock-screen window bypass parameters
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        setShowWhenLocked(false)
                        setTurnScreenOn(false)
                    } else {
                        @Suppress("DEPRECATION")
                        window.clearFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    }
                    
                    // 2. Kill the screen-awake hardware restriction layer
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    // 3. Terminate the activity task chain and return to OS security control
                    finishAndRemoveTask()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the local receiver securely using proper API security flags for Android 14+
        val filter = IntentFilter().apply {
            addAction("com.van.status.UPDATE_UI_LOGS")
            addAction("com.van.status.AUTO_CLOSE_APP")
        }
        
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            appReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        startTelemetryService()

        checkOverlayPermission()

        // Inject Lock-Screen Activity Display Flags to Force Automatic Screen Activation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("van_prefs", Context.MODE_PRIVATE) }
            val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

            LaunchedEffect(Unit) {
                val themeIndex = sharedPrefs.getInt("pref_theme_index", 0)
                val volume = sharedPrefs.getInt("pref_global_volume", 100)
                VehicleStatusManager.setSelectedThemeIndex(themeIndex)
                VehicleStatusManager.setGlobalVolume(volume)
                // Load custom user palette colors
                VehicleStatusManager.setCustomPrimaryColor(
                    androidx.compose.ui.graphics.Color(sharedPrefs.getInt("custom_primary_color", 0xFF38BDF8.toInt()))
                )
                VehicleStatusManager.setCustomAlertColor(
                    androidx.compose.ui.graphics.Color(sharedPrefs.getInt("custom_alert_color", 0xFFEF4444.toInt()))
                )
                VehicleStatusManager.setCustomBackgroundColor(
                    androidx.compose.ui.graphics.Color(sharedPrefs.getInt("custom_bg_color", 0xFF0D0D0D.toInt()))
                )
                VehicleStatusManager.setCustomSurfaceColor(
                    androidx.compose.ui.graphics.Color(sharedPrefs.getInt("custom_surface_color", 0xFF1E293B.toInt()))
                )
            }

            VanStatusTheme {

                // Persistent preference states
                var flEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("pref_fl_enabled", true)) }
                var frEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("pref_fr_enabled", true)) }
                var rlEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("pref_rl_enabled", true)) }
                var backEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("pref_back_enabled", true)) }
                var chimeSelection by remember { mutableStateOf(sharedPrefs.getString("pref_chime_selection", "audi_chime") ?: "audi_chime") }

                val isFlOpen by VehicleStatusManager.isFlOpen.collectAsState()
                val isFrOpen by VehicleStatusManager.isFrOpen.collectAsState()
                val isRlOpen by VehicleStatusManager.isRlOpen.collectAsState()
                val isRrOpen by VehicleStatusManager.isBackOpen.collectAsState()
                val isBuzzerEnabled by VehicleStatusManager.isBuzzerEnabled.collectAsState()
                val cabinTemperature by VehicleStatusManager.cabinTemperature.collectAsState()
                val diagnosticLogs by VehicleStatusManager.diagnosticLogs.collectAsState()

                // Navigation States
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isDiagnosticsOpen by remember { mutableStateOf(false) }

                val isAnyActiveDoorOpen = (isFlOpen && flEnabled) ||
                                          (isFrOpen && frEnabled) ||
                                          (isRlOpen && rlEnabled) ||
                                          (isRrOpen && backEnabled)

                // Screen Timeout Prevention: Keep LCD display awake while an active alarm event triggers
                LaunchedEffect(isAnyActiveDoorOpen) {
                    if (isAnyActiveDoorOpen) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                if (isDiagnosticsOpen) {
                    DeveloperDiagnosticsScreen(
                        logs = diagnosticLogs,
                        onClearLogs = { VehicleStatusManager.clearDiagnosticLogs() },
                        onBack = { isDiagnosticsOpen = false }
                    )
                } else if (isSettingsOpen) {
                    SettingsScreen(
                        flEnabled = flEnabled,
                        frEnabled = frEnabled,
                        rlEnabled = rlEnabled,
                        backEnabled = backEnabled,
                        chimeSelection = chimeSelection,
                        onToggleFl = { enabled ->
                            flEnabled = enabled
                            sharedPrefs.edit().putBoolean("pref_fl_enabled", enabled).apply()
                        },
                        onToggleFr = { enabled ->
                            frEnabled = enabled
                            sharedPrefs.edit().putBoolean("pref_fr_enabled", enabled).apply()
                        },
                        onToggleRl = { enabled ->
                            rlEnabled = enabled
                            sharedPrefs.edit().putBoolean("pref_rl_enabled", enabled).apply()
                        },
                        onToggleBack = { enabled ->
                            backEnabled = enabled
                            sharedPrefs.edit().putBoolean("pref_back_enabled", enabled).apply()
                        },
                        onSelectChime = { chime ->
                            chimeSelection = chime
                            sharedPrefs.edit().putString("pref_chime_selection", chime).apply()
                        },
                        onBack = { isSettingsOpen = false },
                        onNavigateToDiagnostics = { isDiagnosticsOpen = true }
                    )
                } else {
                    VanStatusScreen(
                        isFlOpen = isFlOpen && flEnabled,
                        isFrOpen = isFrOpen && frEnabled,
                        isRlOpen = isRlOpen && rlEnabled,
                        isRrOpen = isRrOpen && backEnabled,
                        cabinTemperature = cabinTemperature,
                        isBuzzerEnabled = isBuzzerEnabled,
                        onToggleBuzzer = { VehicleStatusManager.setBuzzerEnabled(!isBuzzerEnabled) },
                        onNavigateToSettings = {
                            if (keyguardManager.isKeyguardLocked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    keyguardManager.requestDismissKeyguard(this@MainActivity, object : KeyguardManager.KeyguardDismissCallback() {
                                        override fun onDismissSucceeded() {
                                            isSettingsOpen = true
                                        }
                                    })
                                }
                            } else {
                                isSettingsOpen = true
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(appReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startTelemetryService() {
        val serviceIntent = Intent(this, UsbBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}
