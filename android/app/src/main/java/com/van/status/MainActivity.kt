package com.van.status

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    private lateinit var buzzerController: BuzzerController
    private var hasBeenActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buzzerController = BuzzerController()

        startTelemetryService()

        checkOverlayPermission()

        setContent {
            VanStatusTheme {
                val isFlOpen by VehicleStatusManager.isFlOpen.collectAsState()
                val isFrOpen by VehicleStatusManager.isFrOpen.collectAsState()
                val isRlOpen by VehicleStatusManager.isRlOpen.collectAsState()
                val isRrOpen by VehicleStatusManager.isBackOpen.collectAsState()
                val isBuzzerEnabled by VehicleStatusManager.isBuzzerEnabled.collectAsState()
                val cabinTemperature by VehicleStatusManager.cabinTemperature.collectAsState()

                val isAnyDoorOpen = isFlOpen || isFrOpen || isRlOpen || isRrOpen

                // Audio Telemetry Warning Loop
                LaunchedEffect(isAnyDoorOpen, isBuzzerEnabled) {
                    if (isAnyDoorOpen && isBuzzerEnabled) {
                        buzzerController.start()
                    } else {
                        buzzerController.stop()
                    }
                }

                // Smart 5-Second Delayed Auto-Dismiss Routine
                LaunchedEffect(isAnyDoorOpen) {
                    if (isAnyDoorOpen) {
                        hasBeenActive = true
                    } else if (hasBeenActive) {
                        // All doors just closed. Wait for 5 seconds before executing finish()
                        kotlinx.coroutines.delay(5000)
                        
                        // If the coroutine wasn't cancelled by a door opening event, close the app safely
                        finish()
                    }
                }

                VanStatusScreen(
                    isFlOpen = isFlOpen,
                    isFrOpen = isFrOpen,
                    isRlOpen = isRlOpen,
                    isRrOpen = isRrOpen,
                    cabinTemperature = cabinTemperature,
                    isBuzzerEnabled = isBuzzerEnabled,
                    onToggleBuzzer = { VehicleStatusManager.setBuzzerEnabled(!isBuzzerEnabled) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        buzzerController.release()
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

class BuzzerController {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    private var buzzerThread: Thread? = null
    @Volatile private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        buzzerThread = Thread {
            try {
                while (isRunning) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    Thread.sleep(900)
                }
            } catch (e: InterruptedException) {
                // Interrupted
            }
        }.apply {
            name = "BuzzerThread"
            start()
        }
    }

    fun stop() {
        isRunning = false
        buzzerThread?.interrupt()
        buzzerThread = null
    }

    fun release() {
        stop()
        toneGenerator.release()
    }
}
