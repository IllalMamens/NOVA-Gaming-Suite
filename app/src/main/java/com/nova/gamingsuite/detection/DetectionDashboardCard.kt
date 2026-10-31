package com.nova.gamingsuite.detection

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cyan = Color(0xFF00D9FF)
private val White = Color.White
private val Muted = Color(0xFF9299AA)

@Composable
fun DetectionDashboardCard() {
    val context = LocalContext.current
    var snapshot by remember {
        mutableStateOf(DetectionEngine.detect(context))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151925)
        )
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
                        "NOVA DETECTION ENGINE",
                        color = Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "CORE 0.1.2 / HARDWARE SCAN",
                        color = Muted,
                        fontSize = 9.sp
                    )
                }

                Button(
                    onClick = {
                        snapshot = DetectionEngine.detect(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF263443)
                    )
                ) {
                    Text("REFRESH", color = Cyan, fontSize = 10.sp)
                }
            }

            DetectionSection("DEVICE", snapshot.device)
            DetectionSection("CPU & GPU", snapshot.hardware)
            DetectionSection("BATTERY & THERMAL", snapshot.power)
            DetectionSection("DISPLAY", snapshot.display)
            DetectionSection("SENSORS", snapshot.sensors)
            DetectionSection("NETWORK", snapshot.network)
            DetectionSection("ROOT & SECURITY", snapshot.security)
            DetectionSection("GAMING", snapshot.gaming)
        }
    }
}

@Composable
private fun DetectionSection(
    title: String,
    items: List<DetectionItem>
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        HorizontalDivider(color = Color(0xFF303747))

        Text(
            title,
            color = Cyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        item.name,
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        item.value,
                        color = White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "STATUS: ${item.status.uppercase()}",
                    color = when (item.status) {
                        "Available" -> Color(0xFF55DDAA)
                        "Restricted" -> Color(0xFFFFC857)
                        "Unsupported" -> Color(0xFFFF7085)
                        else -> Muted
                    },
                    fontSize = 8.sp
                )

                if (item.detail.isNotBlank()) {
                    Text(
                        item.detail,
                        color = Muted,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
