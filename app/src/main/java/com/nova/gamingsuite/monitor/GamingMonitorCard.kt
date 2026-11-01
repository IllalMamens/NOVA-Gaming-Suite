
package com.nova.gamingsuite.monitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.delay

private val Cyan = Color(0xFF00D9FF)
private val Muted = Color(0xFF9299AA)
private val Panel = Color(0xFF151925)

@Composable
fun GamingMonitorCard() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            NovaMonitorService.PREFS,
            Context.MODE_PRIVATE
        )
    }

    var folder by remember {
        mutableStateOf(prefs.getString(NovaMonitorService.TREE_URI, null))
    }
    var running by remember {
        mutableStateOf(prefs.getBoolean("running", false))
    }
    var message by remember { mutableStateOf("Monitor siap digunakan") }

    fun launchService() {
        val intent = Intent(context, NovaMonitorService::class.java).apply {
            action = NovaMonitorService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            message = "Permintaan monitor dikirim"
        } catch (_: Exception) {
            message = "Service gagal dimulai"
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        launchService()
        if (!granted) {
            message = "Monitor dimulai; notifikasi drawer dibatasi sistem."
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit()
                    .putString(NovaMonitorService.TREE_URI, uri.toString())
                    .apply()
                folder = uri.toString()
                message = "Folder log berhasil dipilih"
            } catch (_: Exception) {
                message = "Gagal menyimpan izin folder"
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            running = prefs.getBoolean("running", false)
            delay(800)
        }
    }

    fun startMonitor() {
        if (folder == null) {
            message = "Pilih folder log terlebih dahulu"
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchService()
        }
    }

    fun stopMonitor() {
        try {
            context.startService(
                Intent(context, NovaMonitorService::class.java).apply {
                    action = NovaMonitorService.ACTION_STOP
                }
            )
            message = "Permintaan berhenti dikirim"
        } catch (_: Exception) {
            message = "Tidak dapat menghentikan service"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "NOVA PERSISTENCE MONITOR",
                color = Cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                "FOREGROUND SERVICE • EXTERNAL CSV",
                color = Muted,
                fontSize = 9.sp
            )
            Text(
                if (running) "● SERVICE ON" else "● SERVICE OFF",
                color = if (running) Color(0xFF55DDAA) else Color(0xFFFF7085),
                fontWeight = FontWeight.Bold
            )
            Text(
                if (folder == null) "Folder log belum dipilih"
                else "Folder log sudah dipilih",
                color = Muted,
                fontSize = 11.sp
            )

            OutlinedButton(
                onClick = { picker.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("PILIH FOLDER LOG")
            }

            Button(
                onClick = {
                    if (running) stopMonitor() else startMonitor()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running)
                        Color(0xFF49232D) else Color(0xFF174536)
                )
            ) {
                Text(
                    if (running) "STOP MONITOR" else "START MONITOR",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    } catch (_: Exception) {
                        message = "Pengaturan baterai tidak tersedia"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("PENGATURAN OPTIMASI BATERAI")
            }

            Text(message, color = Muted, fontSize = 10.sp)
            Text(
                "Interval 5 detik • CSV tersimpan pada folder pilihan",
                color = Muted,
                fontSize = 9.sp
            )
        }
    }
}
