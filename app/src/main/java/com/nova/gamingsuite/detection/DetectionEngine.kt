package com.nova.gamingsuite.detection

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.provider.Settings
import android.view.WindowManager
import java.io.File
import java.util.Locale

data class DetectionItem(
    val name: String,
    val value: String,
    val status: String,
    val detail: String = ""
)

data class DetectionSnapshot(
    val device: List<DetectionItem>,
    val hardware: List<DetectionItem>,
    val power: List<DetectionItem>,
    val display: List<DetectionItem>,
    val sensors: List<DetectionItem>,
    val network: List<DetectionItem>,
    val security: List<DetectionItem>,
    val gaming: List<DetectionItem>
)

object DetectionEngine {

    fun detect(context: Context): DetectionSnapshot {
        return DetectionSnapshot(
            device = detectDevice(context),
            hardware = detectHardware(context),
            power = detectPower(context),
            display = detectDisplay(context),
            sensors = detectSensors(context),
            network = detectNetwork(context),
            security = detectSecurity(),
            gaming = detectGames(context)
        )
    }

    private fun detectDevice(context: Context): List<DetectionItem> {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager

        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)

        val storage = StatFs(Environment.getDataDirectory().path)

        return listOf(
            DetectionItem(
                "Manufacturer",
                Build.MANUFACTURER,
                "Available"
            ),
            DetectionItem(
                "Brand / Model",
                "${Build.BRAND} ${Build.MODEL}",
                "Available"
            ),
            DetectionItem(
                "Device Codename",
                Build.DEVICE,
                "Available"
            ),
            DetectionItem(
                "Android",
                "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
                "Available"
            ),
            DetectionItem(
                "CPU ABI",
                Build.SUPPORTED_ABIS.joinToString(", "),
                "Available"
            ),
            DetectionItem(
                "Available CPU Processors",
                Runtime.getRuntime().availableProcessors().toString(),
                "Available",
                "Processors available to this application"
            ),
            DetectionItem(
                "Total RAM",
                "${memory.totalMem / 1048576} MB",
                "Available"
            ),
            DetectionItem(
                "Available RAM",
                "${memory.availMem / 1048576} MB",
                "Available"
            ),
            DetectionItem(
                "Total Storage",
                formatGb(storage.totalBytes),
                "Available"
            ),
            DetectionItem(
                "Free Storage",
                formatGb(storage.availableBytes),
                "Available"
            )
        )
    }

    private fun detectHardware(context: Context): List<DetectionItem> {
        val gpu = detectGpu()

        val vulkan = context.packageManager.hasSystemFeature(
            "android.hardware.vulkan.level"
        )

        return listOf(
            DetectionItem(
                "GPU Renderer",
                gpu.first,
                gpu.third
            ),
            DetectionItem(
                "GPU Vendor",
                gpu.second,
                gpu.third
            ),
            DetectionItem(
                "OpenGL ES",
                context.packageManager.systemAvailableFeatures
                    .firstOrNull { it.name == null }
                    ?.reqGlEsVersion
                    ?.let {
                        "${it shr 16}.${it and 0xffff}"
                    } ?: "Unavailable",
                "Available"
            ),
            DetectionItem(
                "Vulkan Hardware",
                if (vulkan) "Supported" else "Not detected",
                if (vulkan) "Available" else "Unsupported"
            )
        )
    }

    private fun detectGpu(): Triple<String, String, String> {
        var renderer = "Unavailable"
        var vendor = "Unavailable"

        var eglDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext = EGL14.EGL_NO_CONTEXT
        var eglSurface = EGL14.EGL_NO_SURFACE

        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                return Triple(renderer, vendor, "Unavailable")
            }

            val versions = IntArray(2)
            if (!EGL14.eglInitialize(
                    eglDisplay, versions, 0, versions, 1
                )
            ) {
                return Triple(renderer, vendor, "Unavailable")
            }

            val attributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)

            if (!EGL14.eglChooseConfig(
                    eglDisplay, attributes, 0,
                    configs, 0, 1, count, 0
                ) || count[0] == 0
            ) {
                return Triple(renderer, vendor, "Unavailable")
            }

            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                ),
                0
            )

            eglSurface = EGL14.eglCreatePbufferSurface(
                eglDisplay,
                configs[0],
                intArrayOf(
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
                ),
                0
            )

            if (eglContext == EGL14.EGL_NO_CONTEXT ||
                eglSurface == EGL14.EGL_NO_SURFACE
            ) {
                return Triple(renderer, vendor, "Unavailable")
            }

            if (!EGL14.eglMakeCurrent(
                    eglDisplay, eglSurface, eglSurface, eglContext
                )
            ) {
                return Triple(renderer, vendor, "Unavailable")
            }

            renderer = GLES20.glGetString(GLES20.GL_RENDERER)
                ?: "Unavailable"

            vendor = GLES20.glGetString(GLES20.GL_VENDOR)
                ?: "Unavailable"

            return Triple(renderer, vendor, "Available")

        } catch (_: Exception) {
            return Triple(renderer, vendor, "Unavailable")
        } finally {
            try {
                if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )

                    if (eglSurface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    }

                    if (eglContext != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(eglDisplay, eglContext)
                    }

                    EGL14.eglTerminate(eglDisplay)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun detectPower(context: Context): List<DetectionItem> {
        val battery = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = battery?.getIntExtra(
            BatteryManager.EXTRA_LEVEL, -1
        ) ?: -1

        val scale = battery?.getIntExtra(
            BatteryManager.EXTRA_SCALE, -1
        ) ?: -1

        val percent = if (level >= 0 && scale > 0) {
            "${level * 100 / scale}%"
        } else {
            "Unavailable"
        }

        val temperature = battery?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, -1
        ) ?: -1

        val tempValue = if (temperature >= 0) {
            String.format(Locale.US, "%.1f °C", temperature / 10f)
        } else {
            "Unavailable"
        }

        val status = battery?.getIntExtra(
            BatteryManager.EXTRA_STATUS, -1
        ) ?: -1

        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }

        val power = context.getSystemService(
            Context.POWER_SERVICE
        ) as PowerManager

        val thermal = if (Build.VERSION.SDK_INT >= 29) {
            when (power.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "None"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "Unknown"
            }
        } else {
            "Requires Android 10+"
        }

        return listOf(
            DetectionItem("Battery Level", percent, if (level >= 0) "Available" else "Unavailable"),
            DetectionItem("Battery Temperature", tempValue, if (temperature >= 0) "Available" else "Unavailable"),
            DetectionItem("Charging Status", charging, "Available"),
            DetectionItem("Thermal Status", thermal, if (Build.VERSION.SDK_INT >= 29) "Available" else "Restricted")
        )
    }

    private fun detectDisplay(context: Context): List<DetectionItem> {
        val manager = context.getSystemService(
            Context.WINDOW_SERVICE
        ) as WindowManager

        @Suppress("DEPRECATION")
        val display = manager.defaultDisplay

        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val refresh = display.refreshRate

        return listOf(
            DetectionItem(
                "Resolution",
                "${width} × ${height}",
                "Available"
            ),
            DetectionItem(
                "Density",
                "$density DPI",
                "Available"
            ),
            DetectionItem(
                "Refresh Rate",
                String.format(Locale.US, "%.1f Hz", refresh),
                "Available"
            )
        )
    }

    private fun detectSensors(context: Context): List<DetectionItem> {
        val manager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        val sensors = manager.getSensorList(
            android.hardware.Sensor.TYPE_ALL
        )

        val names = sensors.map {
            it.name
        }.distinct()

        return listOf(
            DetectionItem(
                "Sensor Count",
                sensors.size.toString(),
                "Available"
            ),
            DetectionItem(
                "Sensor List",
                if (names.isEmpty()) "No sensors detected"
                else names.joinToString(", "),
                if (sensors.isEmpty()) "Unsupported" else "Available"
            )
        )
    }

    private fun detectNetwork(context: Context): List<DetectionItem> {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork
        val capabilities = network?.let {
            manager.getNetworkCapabilities(it)
        }

        val type = when {
            capabilities == null -> "Disconnected"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
            else -> "Other"
        }

        val connected = capabilities != null

        return listOf(
            DetectionItem(
                "Connection",
                type,
                if (connected) "Available" else "Unavailable"
            ),
            DetectionItem(
                "Internet Capability",
                if (capabilities?.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ) == true
                ) "Available" else "Not detected",
                if (connected) "Available" else "Unavailable"
            )
        )
    }

    private fun detectSecurity(): List<DetectionItem> {
        val testKeys = Build.TAGS?.contains("test-keys") == true

        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/data/adb/magisk"
        )

        val suFound = suPaths.any { File(it).exists() }

        val magisk = File("/data/adb/magisk").exists()

        return listOf(
            DetectionItem(
                "Build Tags",
                if (testKeys) "Test Keys" else "Release / Unknown",
                "Available"
            ),
            DetectionItem(
                "Root Indicators",
                if (suFound) "Possible root indicators found"
                else "No visible indicators",
                "Available",
                "Heuristic check only; not definitive proof"
            ),
            DetectionItem(
                "Magisk Files",
                if (magisk) "Visible" else "Not visible",
                "Available",
                "Android app sandbox may restrict access"
            )
        )
    }

    private fun detectGames(context: Context): List<DetectionItem> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val activities = context.packageManager
                .queryIntentActivities(intent, 0)

            val games = activities.filter {
                it.activityInfo.applicationInfo.category ==
                    ApplicationInfo.CATEGORY_GAME
            }

            DetectionItem(
                "Visible Game Apps",
                games.size.toString(),
                "Available",
                "Only apps visible through Android package visibility"
            ).let { listOf(it) }

        } catch (_: Exception) {
            listOf(
                DetectionItem(
                    "Visible Game Apps",
                    "Unavailable",
                    "Restricted"
                )
            )
        }
    }

    private fun formatGb(bytes: Long): String {
        return String.format(
            Locale.US,
            "%.2f GB",
            bytes / (1024.0 * 1024.0 * 1024.0)
        )
    }
}
