package com.nova.gamingsuite.monitor

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cyan = Color(0xFF00D9FF)
private val Muted = Color(0xFF9299AA)
private val Panel = Color(0xFF151925)

private data class MonitorSample(
    val frequencies: List<String>,
    val cpuTemp: String,
    val gpuTemp: String,
    val batteryTemp: String,
    val ram: String,
    val timestamp: String
)

private fun readValue(path: String): String? =
    try {
        File(path).takeIf { it.canRead() }?.readText()?.trim()
    } catch (_: Exception) {
        null
    }

private fun thermalMax(pattern: Regex): String {
    var maximum = -1

    for (i in 0..60) {
        val folder = File("/sys/class/thermal/thermal_zone$i")
        val type = readValue("${folder.path}/type") ?: continue
        if (!pattern.matches(type)) continue

        val value = readValue("${folder.path}/temp")
            ?.toIntOrNull() ?: continue

        if (value > maximum) maximum = value
    }

    return if (maximum >= 0)
        String.format(Locale.US, "%.1f", maximum / 1000.0)
    else "NA"
}

private fun readSample(): MonitorSample {
    val frequencies = (0..7).map { cpu ->
        val value = readValue(
            "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq"
        )?.toLongOrNull()

        value?.let { (it / 1000).toString() } ?: "NA"
    }

    val available = try {
        File("/proc/meminfo").useLines { lines ->
            lines.firstOrNull { it.startsWith("MemAvailable:") }
                ?.split(Regex("\\s+"))
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?.let { "${it / 1024}" }
        } ?: "NA"
    } catch (_: Exception) {
        "NA"
    }

    return MonitorSample(
        frequencies = frequencies,
        cpuTemp = thermalMax(Regex("apc[0-9]+-cpu[0-9]+-usr")),
        gpuTemp = thermalMax(Regex("gpu[0-9]+-usr")),
        batteryTemp = thermalMax(Regex("battery|bms")),
        ram = available,
        timestamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.US
        ).format(Date())
    )
}

private fun appendSample(context: Context, sample: MonitorSample) {
    try {
        val folder = File(context.filesDir, "nova_logs")
        folder.mkdirs()

        val file = File(folder, "gaming_monitor.csv")
        if (!file.exists()) {
            file.writeText(
                "timestamp,cpu0_mhz,cpu1_mhz,cpu2_mhz,cpu3_mhz," +
                "cpu4_mhz,cpu5_mhz,cpu6_mhz,cpu7_mhz," +
                "cpu_temp_c,gpu_temp_c,battery_temp_c,ram_available_mb\n"
            )
        }

        val row = listOf(
            sample.timestamp,
            *sample.frequencies.toTypedArray(),
            sample.cpuTemp,
            sample.gpuTemp,
            sample.batteryTemp,
            sample.ram
        ).joinToString(",")

        file.appendText("$row\n")
    } catch (_: Exception) {
        // Logging failure must not crash the monitor.
    }
}

@Composable
fun GamingMonitorCard() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var sample by remember { mutableStateOf<MonitorSample?>(null) }
    var logMessage by remember { mutableStateOf("Logger belum aktif") }

    LaunchedEffect(running) {
        if (running) {
            logMessage = "Merekam data ke penyimpanan aplikasi"

            while (true) {
                val result = withContext(Dispatchers.IO) {
                    readSample().also { appendSample(context, it) }
                }
                sample = result
                delay(5000)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "NOVA GAMING MONITOR",
                        color = Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "LIVE SENSOR & SESSION LOGGER",
                        color = Muted,
                        fontSize = 9.sp
                    )
                }

                Text(
                    if (running) "● ON" else "● OFF",
                    color = if (running) Color(0xFF55DDAA)
                            else Color(0xFFFF7085),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            val current = sample

            if (current == null) {
                Text(
                    "Monitor belum berjalan. Tekan START untuk membaca sensor.",
                    color = Muted,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    "CPU FREQUENCY",
                    color = Muted,
                    fontSize = 10.sp
                )
                Text(
                    current.frequencies.mapIndexed { i, value ->
                        "C$i: $value MHz"
                    }.chunked(4).joinToString("   ") { it.joinToString("  ") },
                    color = Color.White,
                    fontSize = 10.sp
                )

                HorizontalDivider(color = Color(0xFF303746))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SensorValue("CPU TEMP", "${current.cpuTemp}°C")
                    SensorValue("GPU TEMP", "${current.gpuTemp}°C")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SensorValue("BATTERY", "${current.batteryTemp}°C")
                    SensorValue("FREE RAM", "${current.ram} MB")
                }

                Text(
                    "Updated: ${current.timestamp}",
                    color = Muted,
                    fontSize = 9.sp
                )
            }

            Text(logMessage, color = Muted, fontSize = 10.sp)

            Button(
                onClick = { running = !running },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running)
                        Color(0xFF49232D) else Color(0xFF174536)
                )
            ) {
                Text(
                    if (running) "STOP MONITOR & LOGGER"
                    else "START MONITOR & LOGGER",
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Sampling interval: 5 seconds • CSV stored privately in app storage",
                color = Muted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun SensorValue(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(
            value,
            color = Cyan,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
