package com.nova.gamingsuite.games

import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class LauncherApp(
    val label: String,
    val packageName: String
)

@Composable
fun GameSpaceCard() {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Memindai aplikasi...") }

    fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val found = packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { info: ResolveInfo ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                LauncherApp(
                    label = info.loadLabel(packageManager)?.toString()
                        ?: activityInfo.packageName,
                    packageName = activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

        apps = found
        message = if (found.isEmpty()) "Tidak ada aplikasi launcher yang ditemukan."
                  else "${found.size} aplikasi ditemukan"
    }

    LaunchedEffect(Unit) { loadApps() }

    val filtered = apps.filter {
        it.label.contains(search, ignoreCase = true) ||
        it.packageName.contains(search, ignoreCase = true)
    }

    Surface(
        color = Color(0xFF151925),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "NOVA GAME SPACE",
                        color = Color(0xFFFF405B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        message,
                        color = Color(0xFF9299AA),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { loadApps() }) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = "Refresh apps",
                        tint = Color(0xFF00D9FF)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Cari game atau aplikasi...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00D9FF),
                    unfocusedBorderColor = Color(0xFF394252),
                    focusedLabelColor = Color(0xFF00D9FF),
                    cursorColor = Color(0xFF00D9FF)
                )
            )

            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Text(
                    if (apps.isEmpty()) "Belum ada aplikasi untuk ditampilkan."
                    else "Aplikasi tidak ditemukan.",
                    color = Color(0xFF9299AA),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        Surface(
                            color = Color(0xFF202838),
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val launchIntent =
                                        packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        try {
                                            context.startActivity(launchIntent)
                                            message = "Membuka ${app.label}"
                                        } catch (_: Exception) {
                                            message = "Gagal membuka ${app.label}"
                                        }
                                    } else {
                                        message = "${app.label} tidak memiliki aktivitas launcher."
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = Color(0xFFFF405B),
                                    modifier = Modifier.size(27.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        app.label,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        app.packageName,
                                        color = Color(0xFF9299AA),
                                        fontSize = 9.sp
                                    )
                                }
                                Text(
                                    "OPEN",
                                    color = Color(0xFF00D9FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
