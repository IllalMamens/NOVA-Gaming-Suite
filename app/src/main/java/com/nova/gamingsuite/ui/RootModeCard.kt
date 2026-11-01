package com.nova.gamingsuite.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.gamingsuite.security.RootAccessManager
import com.nova.gamingsuite.security.RootCheckResult

private val RootCyan = Color(0xFF00D9FF)
private val RootMuted = Color(0xFF9299AA)
private val RootSurface = Color(0xFF151925)

@Composable
fun RootModeCard() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("nova_mode_preferences", Context.MODE_PRIVATE)
    }

    var activeMode by remember { mutableStateOf("NO ROOT") }
    var lastMode by remember {
        mutableStateOf(preferences.getString("last_mode", "NO ROOT") ?: "NO ROOT")
    }
    var rootStatus by remember { mutableStateOf("NOT CHECKED") }
    var rootDetail by remember {
        mutableStateOf("Root access has not been requested.")
    }
    var rootGranted by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RootSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "NOVA DUAL MODE",
                color = RootCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "ACTIVE MODE",
                        color = RootMuted,
                        fontSize = 9.sp
                    )
                    Text(
                        activeMode,
                        color = if (activeMode == "ROOT FULL")
                            Color(0xFF55DDAA) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column {
                    Text("ROOT STATUS", color = RootMuted, fontSize = 9.sp)
                    Text(
                        rootStatus,
                        color = when (rootStatus) {
                            "GRANTED" -> Color(0xFF55DDAA)
                            "DENIED" -> Color(0xFFFF7085)
                            else -> Color(0xFFFFC857)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                rootDetail,
                color = RootMuted,
                fontSize = 10.sp
            )

            Button(
                onClick = {
                    checking = true
                    rootDetail = "Waiting for Magisk authorization..."
                    RootAccessManager.requestAccess { result: RootCheckResult ->
                        checking = false
                        rootStatus = result.status
                        rootDetail = result.detail
                        rootGranted = result.status == "GRANTED"

                        if (!rootGranted && activeMode == "ROOT FULL") {
                            activeMode = "NO ROOT"
                            lastMode = "NO ROOT"
                            preferences.edit()
                                .putString("last_mode", "NO ROOT")
                                .apply()
                        }
                    }
                },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF263443)
                )
            ) {
                Text(
                    if (checking) "WAITING FOR ROOT..." else "REQUEST ROOT ACCESS",
                    color = RootCyan
                )
            }

            Button(
                onClick = {
                    activeMode = "ROOT FULL"
                    lastMode = "ROOT FULL"
                    preferences.edit()
                        .putString("last_mode", lastMode)
                        .apply()
                },
                enabled = rootGranted && !checking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF174536),
                    disabledContainerColor = Color(0xFF252A32)
                )
            ) {
                Text("ACTIVATE ROOT MODE")
            }

            Button(
                onClick = {
                    activeMode = "NO ROOT"
                    lastMode = "NO ROOT"
                    preferences.edit()
                        .putString("last_mode", lastMode)
                        .apply()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF263443)
                )
            ) {
                Text("USE NO ROOT MODE", color = Color.White)
            }

            Text(
                "Last selected: $lastMode. Root mode must be verified again after reopening NOVA.",
                color = RootMuted,
                fontSize = 9.sp
            )

            Text(
                "Root access is requested only after you press the button. No system changes are made by this check.",
                color = RootMuted,
                fontSize = 9.sp
            )
        }
    }
}
