package com.van.status

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class UsbBackgroundService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "VanTelemetryServiceChannel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_USB_PERMISSION = "com.van.status.USB_PERMISSION"
        private const val ACTION_SIMULATE_TELEMETRY = "com.van.status.SIMULATE_TELEMETRY"
    }

    private var usbSerialPort: UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var isReaderRunning = false
    private var readerThread: Thread? = null

    // Track whether the application is currently active due to an alert foreground trigger
    private var isAppBroughtToFront = false

    // Scope for observing telemetry and playing warning audio chimes
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var alarmPlayer: MediaPlayer? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SIMULATE_TELEMETRY -> {
                    val rawSimData = intent.getStringExtra("DATA_STREAM")
                    if (!rawSimData.isNullOrEmpty()) {
                        handleSerialLine(rawSimData.trim())
                    }
                }
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (granted && device != null) {
                            connectToDevice(device)
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { requestUsbPermission(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (device != null && device == usbSerialPort?.device) {
                        disconnect()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()

        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_SIMULATE_TELEMETRY)
        }

        androidx.core.content.ContextCompat.registerReceiver(
            this,
            usbReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        // Start reactive warning chime loop observer
        serviceScope.launch {
            combine(
                VehicleStatusManager.isFlOpen,
                VehicleStatusManager.isFrOpen,
                VehicleStatusManager.isRlOpen,
                VehicleStatusManager.isBackOpen,
                VehicleStatusManager.isBuzzerEnabled
            ) { fl, fr, rl, back, buzzer ->
                val prefs = getSharedPreferences("van_prefs", Context.MODE_PRIVATE)
                val flEnabled = prefs.getBoolean("pref_fl_enabled", true)
                val frEnabled = prefs.getBoolean("pref_fr_enabled", true)
                val rlEnabled = prefs.getBoolean("pref_rl_enabled", true)
                val backEnabled = prefs.getBoolean("pref_back_enabled", true)

                val isAnyActiveDoorOpen = (fl && flEnabled) ||
                                          (fr && frEnabled) ||
                                          (rl && rlEnabled) ||
                                          (back && backEnabled)
                isAnyActiveDoorOpen && buzzer
            }.collect { shouldPlay ->
                if (shouldPlay) {
                    startAlarmChime()
                } else {
                    stopAlarmChime()
                }
            }
        }

        // Dynamically adjust MediaPlayer volume in real-time when global volume changes
        serviceScope.launch {
            VehicleStatusManager.globalVolume.collect { volSetting ->
                val floatVol = volSetting / 100f
                alarmPlayer?.setVolume(floatVol, floatVol)
            }
        }

        scanAndConnect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        disconnect()
        serviceScope.cancel()
        stopAlarmChime()
    }

    private fun startAlarmChime() {
        if (alarmPlayer != null) return // Already playing warning loop

        val prefs = getSharedPreferences("van_prefs", Context.MODE_PRIVATE)
        val chimeSelection = prefs.getString("pref_chime_selection", "audi_chime") ?: "audi_chime"
        val resId = when (chimeSelection) {
            "audi_chime" -> R.raw.audi_chime
            "chime_two" -> R.raw.chime_two
            "chime_three" -> R.raw.chime_three
            "chime_four" -> R.raw.alert_chime_4
            else -> R.raw.audi_chime
        }

        try {
            alarmPlayer = MediaPlayer.create(this, resId).apply {
                isLooping = true
                val vol = VehicleStatusManager.globalVolume.value / 100f
                setVolume(vol, vol)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmChime() {
        try {
            alarmPlayer?.stop()
            alarmPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        alarmPlayer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Van Door Telemetry Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors vehicle telemetry over serial link"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Van Status Monitor")
            .setContentText("Monitoring USB telemetry...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun scanAndConnect() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) return

        val driver = availableDrivers[0]
        val device = driver.device
        if (manager.hasPermission(device)) {
            connectToDevice(device)
        } else {
            requestUsbPermission(device)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        manager.requestPermission(device, permissionIntent)
    }

    private fun connectToDevice(device: UsbDevice) {
        disconnect()
        try {
            val manager = getSystemService(Context.USB_SERVICE) as UsbManager
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
            val connection = manager.openDevice(device) ?: return
            val port = driver.ports[0]

            port.open(connection)
            port.setParameters(9600, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            
            try {
                port.setDTR(true)
                port.setRTS(true)
            } catch (ignored: Exception) {
            }

            usbSerialPort = port
            usbConnection = connection

            startReaderThread()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startReaderThread() {
        isReaderRunning = true
        readerThread = Thread {
            val port = usbSerialPort ?: return@Thread
            val buffer = ByteArray(1024)
            var accumulatedString = ""

            while (isReaderRunning && !Thread.currentThread().isInterrupted) {
                try {
                    val bytesRead = port.read(buffer, 1000)
                    if (bytesRead > 0) {
                        val newString = String(buffer, 0, bytesRead, Charsets.US_ASCII)
                        accumulatedString += newString

                        while (accumulatedString.contains("\n")) {
                            val index = accumulatedString.indexOf("\n")
                            val line = accumulatedString.substring(0, index).trim()
                            accumulatedString = accumulatedString.substring(index + 1)

                            if (line.isNotEmpty()) {
                                handleSerialLine(line)
                            }
                        }
                    }
                } catch (e: IOException) {
                    isReaderRunning = false
                    break
                }
            }
        }.apply {
            name = "UsbSerialReaderThread"
            start()
        }
    }

    private fun handleSerialLine(line: String) {
        // Broadcast raw incoming frame to UI thread for IPC logging
        try {
            val intent = Intent("com.van.status.UPDATE_UI_LOGS").apply {
                putExtra("LOG_LINE", line)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sanitizedLine = line.replace("\\", "")
                                .replace("'", "")
                                .replace("\"", "")
                                .trim()

        val payload = if (sanitizedLine.startsWith("DATA_STREAM:")) {
            sanitizedLine.substringAfter("DATA_STREAM:")
        } else {
            sanitizedLine
        }

        val prefs = getSharedPreferences("van_prefs", Context.MODE_PRIVATE)
        val flEnabled = prefs.getBoolean("pref_fl_enabled", true)
        val frEnabled = prefs.getBoolean("pref_fr_enabled", true)
        val rlEnabled = prefs.getBoolean("pref_rl_enabled", true)
        val backEnabled = prefs.getBoolean("pref_back_enabled", true)

        val tokens = payload.split("|")
        for (token in tokens) {
            val trimmedToken = token.trim()
            if (trimmedToken.startsWith("TEMP:")) {
                val tempStr = trimmedToken.substringAfter("TEMP:")
                val temp = tempStr.toIntOrNull()
                if (temp != null) {
                    VehicleStatusManager.updateCabinTemperature(temp)
                }
            } else {
                val parts = trimmedToken.split("_")
                if (parts.size == 2) {
                    val door = parts[0]
                    val state = parts[1]
                    val isOpen = state.uppercase().trim() == "OPEN"
                    
                    val doorCleaned = door.uppercase().trim()
                    val isAllowed = when (doorCleaned) {
                        "FL" -> flEnabled
                        "FR" -> frEnabled
                        "RL" -> rlEnabled
                        "BACK", "RR" -> backEnabled
                        else -> true
                    }

                    if (isAllowed) {
                        val wasOpen = when (doorCleaned) {
                            "FL" -> VehicleStatusManager.isFlOpen.value
                            "FR" -> VehicleStatusManager.isFrOpen.value
                            "RL" -> VehicleStatusManager.isRlOpen.value
                            "BACK", "RR" -> VehicleStatusManager.isBackOpen.value
                            else -> false
                        }

                        // Display Wake-Up: triggers when an allowed entry point transitions to OPEN
                        if (isOpen && !wasOpen) {
                            triggerHardwareDisplayWakeUp()
                        }

                        VehicleStatusManager.updateDoorState(doorCleaned, isOpen)
                    }
                }
            }
        }

        val isAnyActiveDoorOpen = (flEnabled && VehicleStatusManager.isFlOpen.value) ||
                                  (frEnabled && VehicleStatusManager.isFrOpen.value) ||
                                  (rlEnabled && VehicleStatusManager.isRlOpen.value) ||
                                  (backEnabled && VehicleStatusManager.isBackOpen.value)

        if (isAnyActiveDoorOpen) {
            bringMainActivityToFront()
            isAppBroughtToFront = true
        } else {
            // Auto-Close: shut down app task stack when all active doors transition back to closed
            if (isAppBroughtToFront) {
                isAppBroughtToFront = false
                val closeIntent = Intent("com.van.status.AUTO_CLOSE_APP")
                sendBroadcast(closeIntent)
            }
        }
    }

    private fun triggerHardwareDisplayWakeUp() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "VanStatus:DisplayWakeLock"
            )
            wl.acquire(5000) // Wakes screen for 5 seconds
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bringMainActivityToFront() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launchIntent)
    }

    private fun disconnect() {
        isReaderRunning = false
        readerThread?.interrupt()
        readerThread = null

        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        usbSerialPort = null

        usbConnection?.close()
        usbConnection = null
    }
}