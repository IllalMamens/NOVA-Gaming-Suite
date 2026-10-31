package com.nova.gamingsuite.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val NovaCyan = Color(0xFF00D9FF)
private val NovaWhite = Color(0xFFFFFFFF)
private val NovaMuted = Color(0xFF9299AA)

@Composable
fun DeviceInfoCard() {
    val context = LocalContext.current
    val profile = remember {
        DeviceDetector.detect(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151925)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "DEVICE PROFILE / AUTO DETECT",
                color = NovaCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Text(
                "${profile.manufacturer} ${profile.model}",
                color = NovaWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )

            Text(
                "Codename: ${profile.device}",
                color = NovaMuted,
                fontSize = 11.sp
            )

            HorizontalDivider(color = Color.DarkGray)

            InfoRow("Android", "${profile.androidVersion} (API ${profile.sdk})")
            InfoRow("CPU processors", "${profile.cpuCores} available")
            InfoRow("Architecture", profile.abis.joinToString(", "))
            InfoRow(
                "RAM",
                "${profile.availableRamMb} / ${profile.totalRamMb} MB available"
            )
            InfoRow(
                "Storage",
                String.format(
                    Locale.US,
                    "%.1f / %.1f GB available",
                    profile.availableStorageGb,
                    profile.totalStorageGb
                )
            )
            InfoRow("OpenGL ES", profile.openGlesVersion)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NovaMuted, fontSize = 11.sp)
        Text(
            value,
            color = NovaWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
