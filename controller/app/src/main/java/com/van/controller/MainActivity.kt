package com.van.controller

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : ComponentActivity() {

    private lateinit var adbSocketManager: AdbSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adbSocketManager = AdbSocketManager()

        setContent {
            TelemetryControllerTheme {
                val connectionStatus by adbSocketManager.connectionStatus.collectAsState()
                val logs by adbSocketManager.logs.collectAsState()

                ControllerScreen(
                    connectionStatus = connectionStatus,
                    logs = logs,
                    onSendTelemetry = { data -> adbSocketManager.sendTelemetry(data) }
                )
            }
        }
    }
}

class AdbMessage(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val data: ByteArray = ByteArray(0),
    val dataLength: Int = data.size
) {
    fun serialize(): ByteArray {
        val size = 24 + data.size
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(data.size)
        buffer.putInt(checksum())
        buffer.putInt(command xor -0x1)
        if (data.isNotEmpty()) {
            buffer.put(data)
        }
        return buffer.array()
    }

    private fun checksum(): Int {
        var sum = 0
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    companion object {
        const val CMD_CNXN = 0x4e584e43 // "CNXN"
        const val CMD_AUTH = 0x48545541 // "AUTH"
        const val CMD_OPEN = 0x4e45504f // "OPEN"
        const val CMD_OKAY = 0x59414b4f // "OKAY"
        const val CMD_CLSE = 0x45534c43 // "CLSE"
        const val CMD_WRTE = 0x45545257 // "WRTE"

        fun deserialize(header: ByteArray): AdbMessage {
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val command = buffer.getInt()
            val arg0 = buffer.getInt()
            val arg1 = buffer.getInt()
            val dataLength = buffer.getInt()
            return AdbMessage(command, arg0, arg1, ByteArray(0), dataLength)
        }
    }
}

class AdbSocketManager {
    companion object {
        private const val TAG = "AdbSocketManager"
        private const val HOST = "10.241.164.156"
        private const val PORT = 5555
    }

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        addLog("AdbSocketManager initialized.")
        addLog("Direct ADB IP Target: $HOST:$PORT")
    }

    fun addLog(msg: String) {
        val current = _logs.value.toMutableList()
        current.add(msg)
        if (current.size > 100) {
            current.removeAt(0)
        }
        _logs.value = current
        Log.d(TAG, msg)
    }

    fun sendTelemetry(data: String) {
        val shellCommand = "am broadcast -a com.van.status.SIMULATE_TELEMETRY -n com.van.status/.UsbBackgroundService --es DATA_STREAM '$data'\n"
        
        ioScope.launch {
            _connectionStatus.value = "Sending..."
            val success = executeAdbShellCommand(shellCommand)
            if (success) {
                _connectionStatus.value = "Sent successfully"
            } else {
                _connectionStatus.value = "Transmission failed"
            }
        }
    }

    private fun executeAdbShellCommand(shellCommand: String): Boolean {
        var socket: Socket? = null
        var success = false
        try {
            addLog("Socket: Connecting to $HOST:$PORT...")
            socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 4000)
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            // 1. Send CNXN
            val cnxnData = "host::\u0000".toByteArray(Charsets.US_ASCII)
            val cnxnMsg = AdbMessage(AdbMessage.CMD_CNXN, 0x01000000, 262144, cnxnData)
            outputStream.write(cnxnMsg.serialize())
            outputStream.flush()
            addLog("TX -> CNXN (protocol=1.0, max_payload=262144)")

            // 2. Read CNXN response
            val header = ByteArray(24)
            readFully(inputStream, header)
            val respMsg = AdbMessage.deserialize(header)
            
            if (respMsg.command == AdbMessage.CMD_AUTH) {
                addLog("RX <- AUTH (ADB cryptographic signature required by target device!)")
                val tokenData = readPayload(inputStream, header)
                addLog("AUTH token size: ${tokenData.size} bytes. Connection aborted.")
                socket.close()
                return false
            }

            if (respMsg.command != AdbMessage.CMD_CNXN) {
                addLog("RX <- Unexpected command packet: ${respMsg.command.toString(16)}")
                socket.close()
                return false
            }

            val devInfo = readPayload(inputStream, header)
            addLog("RX <- CNXN: ${String(devInfo, Charsets.UTF_8).trim()}")

            // 3. Send OPEN shell command
            val cmdString = "shell:$shellCommand\u0000"
            val cmdData = cmdString.toByteArray(Charsets.US_ASCII)
            val localId = 1
            val openMsg = AdbMessage(AdbMessage.CMD_OPEN, localId, 0, cmdData)
            outputStream.write(openMsg.serialize())
            outputStream.flush()
            addLog("TX -> OPEN (local_id=$localId, command='$shellCommand')".trim())

            // 4. Read OPEN response
            val openRespHeader = ByteArray(24)
            readFully(inputStream, openRespHeader)
            val openResp = AdbMessage.deserialize(openRespHeader)
            
            if (openResp.command == AdbMessage.CMD_OKAY) {
                val remoteId = openResp.arg0
                addLog("RX <- OKAY (remote_id=$remoteId, local_id=${openResp.arg1})")
                success = true
                
                var running = true
                while (running) {
                    val msgHeader = ByteArray(24)
                    if (!readFullyOrNull(inputStream, msgHeader)) {
                        addLog("Socket: Stream closed by peer.")
                        break
                    }
                    
                    val msg = AdbMessage.deserialize(msgHeader)
                    val msgData = readPayload(inputStream, msgHeader)
                    
                    when (msg.command) {
                        AdbMessage.CMD_WRTE -> {
                            val output = String(msgData, Charsets.UTF_8).trim()
                            if (output.isNotEmpty()) {
                                addLog("ADB STDOUT: $output")
                            }
                            val okayMsg = AdbMessage(AdbMessage.CMD_OKAY, localId, remoteId)
                            outputStream.write(okayMsg.serialize())
                            outputStream.flush()
                        }
                        AdbMessage.CMD_CLSE -> {
                            addLog("RX <- CLSE (Remote channel closed)")
                            val clseMsg = AdbMessage(AdbMessage.CMD_CLSE, localId, remoteId)
                            outputStream.write(clseMsg.serialize())
                            outputStream.flush()
                            running = false
                        }
                        else -> {
                            addLog("RX <- Command packet: ${msg.command.toString(16)}")
                        }
                    }
                }
            } else if (openResp.command == AdbMessage.CMD_CLSE) {
                addLog("RX <- CLSE (Channel connection rejected by target)")
            } else {
                addLog("RX <- Unexpected response to OPEN: ${openResp.command.toString(16)}")
            }

        } catch (e: Exception) {
            addLog("Socket error: ${e.localizedMessage}")
            e.printStackTrace()
        } finally {
            try {
                socket?.close()
            } catch (ignored: Exception) {}
            addLog("Socket closed.")
        }
        return success
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray) {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val count = inputStream.read(buffer, bytesRead, buffer.size - bytesRead)
            if (count < 0) {
                throw EOFException("Connection closed prematurely.")
            }
            bytesRead += count
        }
    }

    private fun readFullyOrNull(inputStream: InputStream, buffer: ByteArray): Boolean {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val count = try {
                inputStream.read(buffer, bytesRead, buffer.size - bytesRead)
            } catch (e: Exception) {
                -1
            }
            if (count < 0) {
                return false
            }
            bytesRead += count
        }
        return true
    }

    private fun readPayload(inputStream: InputStream, header: ByteArray): ByteArray {
        val msg = AdbMessage.deserialize(header)
        val len = msg.dataLength
        if (len <= 0) return ByteArray(0)
        val data = ByteArray(len)
        readFully(inputStream, data)
        return data
    }
}

@Composable
fun ControllerScreen(
    connectionStatus: String,
    logs: List<String>,
    onSendTelemetry: (String) -> Unit
) {
    var isFlOpen by remember { mutableStateOf(false) }
    var isFrOpen by remember { mutableStateOf(false) }
    var isRlOpen by remember { mutableStateOf(false) }
    var isBackOpen by remember { mutableStateOf(false) }

    var sliderValue by remember { mutableStateOf(24f) }
    val currentTemp = sliderValue.toInt()
    var lastSentTemp by remember { mutableStateOf(24) }

    LaunchedEffect(currentTemp) {
        if (currentTemp != lastSentTemp) {
            lastSentTemp = currentTemp
            onSendTelemetry("TEMP:$currentTemp")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
            .padding(16.dp)
    ) {
        // --- Header Block ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "VAN TELEMETRY",
                    color = Color(0xFF38BDF8),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DIRECT ADB INJECTION OVER TCP",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val statusColor = when (connectionStatus) {
                "Sent successfully" -> Color(0xFF10B981)
                "Sending..." -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            val infiniteTransition = rememberInfiniteTransition(label = "BlinkStatus")
            val blinkAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Blinking"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = statusColor.copy(alpha = blinkAlpha),
                            shape = RoundedCornerShape(50)
                        )
                )
                Text(
                    text = connectionStatus.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Hardcoded Network Configuration Card ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "HARDCODED ADB TARGET CONNECTION",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ENDPOINT IP",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "10.241.164.156",
                        color = Color(0xFF38BDF8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PORT",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "5555",
                        color = Color(0xFF38BDF8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Door Simulation Grid ---
        Text(
            text = "DOOR ACTUATOR SIMULATORS",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DoorToggleCard(
                    label = "FL",
                    name = "Front Left",
                    isOpen = isFlOpen,
                    onToggle = {
                        isFlOpen = !isFlOpen
                        onSendTelemetry(if (isFlOpen) "FL_OPEN" else "FL_CLOSED")
                    }
                )
                DoorToggleCard(
                    label = "RL",
                    name = "Rear Left",
                    isOpen = isRlOpen,
                    onToggle = {
                        isRlOpen = !isRlOpen
                        onSendTelemetry(if (isRlOpen) "RL_OPEN" else "RL_CLOSED")
                    }
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DoorToggleCard(
                    label = "FR",
                    name = "Front Right",
                    isOpen = isFrOpen,
                    onToggle = {
                        isFrOpen = !isFrOpen
                        onSendTelemetry(if (isFrOpen) "FR_OPEN" else "FR_CLOSED")
                    }
                )
                DoorToggleCard(
                    label = "BACK",
                    name = "Back / Tailgate",
                    isOpen = isBackOpen,
                    onToggle = {
                        isBackOpen = !isBackOpen
                        onSendTelemetry(if (isBackOpen) "BACK_OPEN" else "BACK_CLOSED")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Climate Simulation Slider ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TEMPERATURE CONTROL NOZZLE",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Simulate cabin climate sensor value",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "$currentTemp°C",
                    color = if (currentTemp > 30) Color(0xFFF97316) else Color(0xFF38BDF8),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 0f..50f,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                    thumbColor = Color(0xFF38BDF8)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0°C", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                Text("25°C", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                Text("50°C", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Live Console Terminal Log ---
        Text(
            text = "LIVE TRANSMISSION TERMINAL LOG",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        val listState = rememberLazyListState()
        LaunchedEffect(logs.size) {
            if (logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF020617))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "No logs yet. Toggle actuators to initiate direct ADB socket injection.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("error") || log.contains("Error") || log.contains("failed") || log.contains("Signature required") || log.contains("failed due to secure ADB")) Color(0xFFEF4444)
                            else if (log.contains("TX ->")) Color(0xFF34D399)
                            else if (log.contains("RX <-")) Color(0xFF38BDF8)
                            else Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoorToggleCard(
    label: String,
    name: String,
    isOpen: Boolean,
    onToggle: () -> Unit
) {
    val activeBorderColor = Color(0xFF38BDF8)
    val inactiveBorderColor = Color.White.copy(alpha = 0.08f)
    val cardBackground = Color.White.copy(alpha = 0.03f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .border(
                width = 1.dp,
                color = if (isOpen) activeBorderColor else inactiveBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = if (isOpen) activeBorderColor else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOpen) activeBorderColor.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.1f)
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(if (isOpen) Alignment.CenterEnd else Alignment.CenterStart)
                            .background(
                                color = if (isOpen) activeBorderColor else Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isOpen) "OPEN" else "CLOSED",
                color = if (isOpen) activeBorderColor else Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TelemetryControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF38BDF8),
            background = Color(0xFF020617),
            surface = Color(0xFF0F172A)
        ),
        content = content
    )
}
