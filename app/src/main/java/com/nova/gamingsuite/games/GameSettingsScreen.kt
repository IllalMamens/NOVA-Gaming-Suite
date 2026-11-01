package com.nova.gamingsuite.games

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GameRed = Color(0xFFFF405B)
private val GameCyan = Color(0xFF00D9FF)
private val GameMuted = Color(0xFF9299AA)
private val GameSurface = Color(0xFF151925)

private fun gamePrefs(context: Context, packageName: String) =
    context.getSharedPreferences("nova_game_$packageName", Context.MODE_PRIVATE)

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun csvEscape(value: String): String =
    "\"" + value.replace("\"", "\"\"") + "\""

@Composable
fun GameSettingsScreen(
    gameName: String,
    packageName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(packageName) { gamePrefs(context, packageName) }

    var profile by remember(packageName) {
        mutableStateOf(prefs.getString("profile", "Balanced") ?: "Balanced")
    }
    var resolution by remember(packageName) {
        mutableStateOf(prefs.getString("resolution", "Default") ?: "Default")
    }
    var sessionStart by remember(packageName) {
        mutableLongStateOf(prefs.getLong("active_start", 0L))
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var history by remember(packageName) {
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList()?.sortedDescending() ?: emptyList())
    }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(sessionStart) {
        while (sessionStart > 0L) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    fun saveHistory() {
        history = prefs.getStringSet("history", emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    fun startSession() {
        val start = System.currentTimeMillis()
        sessionStart = start
        prefs.edit().putLong("active_start", start).apply()
        status = "Sesi ${gameName} dimulai."
    }

    fun stopSession() {
        if (sessionStart <= 0L) return
        val end = System.currentTimeMillis()
        val record = "$sessionStart|$end"
        val updated = (prefs.getStringSet("history", emptySet()) ?: emptySet()) + record
        prefs.edit()
            .putStringSet("history", updated)
            .remove("active_start")
            .apply()
        sessionStart = 0L
        saveHistory()
        status = "Sesi berhasil disimpan."
    }

    fun exportHistory() {
        val monitorPrefs = context.getSharedPreferences("nova_monitor", Context.MODE_PRIVATE)
        val rawUri = monitorPrefs.getString("tree_uri", null)
        if (rawUri.isNullOrBlank()) {
            status = "Pilih folder CSV melalui tab Monitor terlebih dahulu."
            return
        }

        try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(rawUri))
            if (tree == null) {
                status = "Folder ekspor tidak dapat dibuka."
                return
            }

            val novaFolder = tree.findFile("NOVA") ?: tree.createDirectory("NOVA")
            val historyFolder = novaFolder?.findFile("GameHistory")
                ?: novaFolder?.createDirectory("GameHistory")
            val safeName = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val gameFolder = historyFolder?.findFile(safeName)
                ?: historyFolder?.createDirectory(safeName)

            if (gameFolder == null) {
                status = "Gagal membuat folder ekspor."
                return
            }

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = gameFolder.createFile("text/csv", "${safeName}_history_$stamp.csv")
            if (file == null) {
                status = "Gagal membuat file CSV."
                return
            }

            val rows = history.mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size != 2) null
                else {
                    val start = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val end = parts[1].toLongOrNull() ?: return@mapNotNull null
                    Triple(start, end, (end - start).coerceAtLeast(0))
                }
            }.sortedBy { it.first }

            context.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { writer ->
                writer.write("game_name,package_name,start_time,end_time,duration_seconds\n")
                rows.forEach { row ->
                    val startText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(row.first))
                    val endText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(row.second))
                    writer.write(
                        listOf(
                            csvEscape(gameName),
                            csvEscape(packageName),
                            csvEscape(startText),
                            csvEscape(endText),
                            (row.third / 1000).toString()
                        ).joinToString(",") + "\n"
                    )
                }
            }
            status = "CSV berhasil diekspor: NOVA/GameHistory/$safeName"
        } catch (e: Exception) {
            status = "Ekspor gagal: ${e.message ?: "kesalahan akses folder"}"
        }
    }

    val elapsed = if (sessionStart > 0L) (now - sessionStart).coerceAtLeast(0) else 0L

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Kembali", tint = GameCyan)
            }
            Column(Modifier.weight(1f)) {
                Text("GAME SETTINGS", color = GameRed, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(gameName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                GamePanel(title = "GRAPHICS PROFILE", subtitle = "Disimpan khusus untuk game ini") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Eco", "Balanced", "Performance").forEach { option ->
                            FilterChip(
                                selected = profile == option,
                                onClick = {
                                    profile = option
                                    prefs.edit().putString("profile", option).apply()
                                },
                                label = { Text(option, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GameRed.copy(alpha = 0.22f),
                                    selectedLabelColor = Color.White,
                                    labelColor = GameMuted
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (profile) {
                            "Eco" -> "Rekomendasi: prioritaskan efisiensi daya dan suhu."
                            "Performance" -> "Rekomendasi: prioritaskan kelancaran; pantau suhu perangkat."
                            else -> "Rekomendasi: gunakan keseimbangan performa dan efisiensi."
                        },
                        color = GameMuted,
                        fontSize = 11.sp
                    )
                    Text("Profil ini adalah preset rekomendasi, bukan pengubah paksa grafis game.", color = GameMuted, fontSize = 10.sp)
                }
            }

            item {
                GamePanel(title = "RESOLUTION PREFERENCE", subtitle = "Preferensi rekomendasi, tidak memaksa resolusi game") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Default", "Low", "Medium", "High").forEach { option ->
                            FilterChip(
                                selected = resolution == option,
                                onClick = {
                                    resolution = option
                                    prefs.edit().putString("resolution", option).apply()
                                },
                                label = { Text(option, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GameCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = Color.White,
                                    labelColor = GameMuted
                                )
                            )
                        }
                    }
                }
            }

            item {
                GamePanel(title = "SESSION MONITOR", subtitle = "Pencatatan sesi manual untuk game ini") {
                    Text(
                        if (sessionStart > 0L) "SESSION ACTIVE" else "SESSION STOPPED",
                        color = if (sessionStart > 0L) GameCyan else GameMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        formatDuration(elapsed),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Button(
                        onClick = { if (sessionStart > 0L) stopSession() else startSession() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionStart > 0L) GameRed else GameCyan,
                            contentColor = Color(0xFF090B12)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (sessionStart > 0L) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (sessionStart > 0L) "STOP & SAVE SESSION" else "START SESSION")
                    }
                    Text(
                        "Timer ini dimulai dan dihentikan secara manual. Belum mendeteksi game aktif otomatis.",
                        color = GameMuted,
                        fontSize = 10.sp
                    )
                }
            }

            item {
                GamePanel(title = "PERFORMANCE DATA", subtitle = "Status ketersediaan data") {
                    MetricLine("CPU temperature", "Tersedia melalui monitor sensor global")
                    MetricLine("GPU temperature", "Tersedia melalui monitor sensor global")
                    MetricLine("RAM available", "Tersedia melalui monitor sensor global")
                    MetricLine("CPU frequency", "Tersedia melalui monitor sensor global")
                    MetricLine("FPS", "Tidak tersedia melalui API Android standar")
                    Text(
                        "Grafik dan pengaitan sampel sensor ke sesi game akan ditambahkan setelah integrasi monitor per-game.",
                        color = GameMuted,
                        fontSize = 10.sp
                    )
                }
            }

            item {
                GamePanel(title = "GAME HISTORY", subtitle = "${history.size} sesi tersimpan untuk game ini") {
                    if (history.isEmpty()) {
                        Text("Belum ada riwayat. Mulai sesi untuk membuat catatan.", color = GameMuted, fontSize = 11.sp)
                    } else {
                        history.take(20).forEach { record ->
                            val parts = record.split("|")
                            if (parts.size == 2) {
                                val start = parts[0].toLongOrNull()
                                val end = parts[1].toLongOrNull()
                                if (start != null && end != null) {
                                    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                        Text(
                                            SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID")).format(Date(start)),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Durasi: ${formatDuration((end - start).coerceAtLeast(0))}",
                                            color = GameMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF30394A))
                                }
                            }
                        }
                        if (history.size > 20) {
                            Text("Menampilkan 20 sesi terbaru.", color = GameMuted, fontSize = 10.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = { exportHistory() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GameCyan)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("EXPORT GAME CSV")
                    }
                    if (status.isNotBlank()) {
                        Text(status, color = GameCyan, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePanel(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = GameSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = GameRed, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = GameMuted, fontSize = 10.sp)
            content()
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 11.sp)
        Text(value, color = GameMuted, fontSize = 9.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
    }
}
