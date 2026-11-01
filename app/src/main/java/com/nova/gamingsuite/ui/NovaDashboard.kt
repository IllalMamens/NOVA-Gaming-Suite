package com.nova.gamingsuite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.gamingsuite.device.DeviceInfoCard
import com.nova.gamingsuite.monitor.GamingMonitorCard
import com.nova.gamingsuite.games.GameSpaceCard
import com.nova.gamingsuite.detection.DetectionDashboardCard
import com.nova.gamingsuite.ui.RootModeCard

private val Background = Color(0xFF090B12)
private val Surface = Color(0xFF151925)
private val NeonRed = Color(0xFFFF405B)
private val NeonCyan = Color(0xFF00D9FF)
private val Muted = Color(0xFF9299AA)

private data class NovaFeature(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun NovaDashboard() {
    var selected by remember { mutableStateOf("Dashboard") }
    var message by remember { mutableStateOf("") }

    val features = listOf(
        NovaFeature("Game Space", "Game library & launcher", Icons.Default.SportsEsports),
        NovaFeature("Game Booster", "Gaming control center", Icons.Default.Bolt),
        NovaFeature("System Monitor", "CPU, GPU & temperature", Icons.Default.MonitorHeart),
        NovaFeature("Graphics Engine", "Graphics information", Icons.Default.Tune),
        NovaFeature("Gaming Profiles", "Personalized configurations", Icons.Default.Settings),
        NovaFeature("Root Bridge", "Root integration status", Icons.Default.Security)
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Surface,
            primary = NeonRed,
            secondary = NeonCyan
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Surface) {
                    val tabs = listOf(
                        "Dashboard" to Icons.Default.Dashboard,
                        "Games" to Icons.Default.SportsEsports,
                        "Settings" to Icons.Default.Settings
                    )

                    tabs.forEach { (name, icon) ->
                        NavigationBarItem(
                            selected = selected == name,
                            onClick = {
                                selected = name
                                message = if (name == "Dashboard") {
                                    ""
                                } else {
                                    "$name module is coming soon"
                                }
                            },
                            icon = { Icon(icon, contentDescription = name) },
                            label = { Text(name) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "N O V A",
                            color = NeonRed,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "GAMING SUITE / CORE 0.4.0",
                            color = Muted,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    Surface(
                        color = Color(0xFF202B35),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "● CORE READY",
                            color = NeonCyan,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Surface(
                    color = Surface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "WELCOME BACK",
                            color = Muted,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "LEVEL UP YOUR",
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "GAMING EXPERIENCE",
                            color = NeonRed,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Universal Android Gaming Suite",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                DeviceInfoCard()

                Spacer(Modifier.height(16.dp))
                GamingMonitorCard()

                Spacer(Modifier.height(16.dp))
                GameSpaceCard()

                Spacer(Modifier.height(16.dp))
                DetectionDashboardCard()

                Spacer(Modifier.height(16.dp))
                RootModeCard()

                Spacer(Modifier.height(20.dp))

                Text(
                    "NOVA MODULES",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                features.chunked(2).forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        group.forEach { feature ->
                            FeatureCard(
                                feature,
                                Modifier.weight(1f)
                            ) {
                                message = "${feature.title} module is coming soon"
                            }
                        }

                        if (group.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                if (message.isNotEmpty()) {
                    Text(
                        message,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Text(
                    "NOVA GAMING SUITE / UNIVERSAL EDITION",
                    color = Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: NovaFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                feature.icon,
                contentDescription = null,
                tint = NeonRed,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    feature.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    feature.description,
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
