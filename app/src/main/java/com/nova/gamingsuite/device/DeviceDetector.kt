package com.nova.gamingsuite.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.util.Locale

object DeviceDetector {

    fun detect(context: Context): DeviceProfile {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager

        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)

        val storage = StatFs(Environment.getDataDirectory().path)

        val totalStorage = storage.totalBytes
        val availableStorage = storage.availableBytes

        val glVersion = try {
            val feature = context.packageManager
                .systemAvailableFeatures
                .firstOrNull { it.name == null }

            feature?.reqGlEsVersion?.let { version ->
                String.format(
                    Locale.US,
                    "%d.%d",
                    (version shr 16) and 0xffff,
                    version and 0xffff
                )
            } ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }

        return DeviceProfile(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            sdk = Build.VERSION.SDK_INT,
            abis = Build.SUPPORTED_ABIS.toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = memory.totalMem / (1024 * 1024),
            availableRamMb = memory.availMem / (1024 * 1024),
            totalStorageGb = totalStorage / (1024.0 * 1024 * 1024),
            availableStorageGb = availableStorage / (1024.0 * 1024 * 1024),
            openGlesVersion = glVersion
        )
    }
}
