package com.nova.gamingsuite.monitor

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class NovaMonitorService : Service() {
    companion object {
        const val ACTION_START = "com.nova.gamingsuite.MONITOR_START"
        const val ACTION_STOP = "com.nova.gamingsuite.MONITOR_STOP"
        const val CHANNEL = "nova_monitor_channel"
        const val NOTIFICATION_ID = 3030
        const val PREFS = "nova_monitor"
        const val TREE_URI = "tree_uri"
    }

    private val active = AtomicBoolean(false)
    private var worker: Thread? = null
    private var logFile: DocumentFile? = null\n    private var gameLogFile: DocumentFile? = null\n    private var gameLogPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "NOVA Gaming Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Status pemantauan sensor NOVA" }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitor()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, makeNotification())
                getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit().putBoolean("running", true).apply()
                if (active.compareAndSet(false, true)) {
                    prepareLog()
                    worker = Thread { monitorLoop() }.also { it.start() }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun makeNotification(): Notification {
        val stopIntent = Intent(this, NovaMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val pending = PendingIntent.getService(
            this, 3031, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("NOVA Gaming Monitor")
            .setContentText("Pemantauan sensor sedang berjalan")
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null, "STOP", pending
                ).build()
            )
            .build()
    }

    private fun prepareLog() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val raw = prefs.getString(TREE_URI, null) ?: return
        try {
            val tree = DocumentFile.fromTreeUri(this, Uri.parse(raw)) ?: return
            val folder = tree.findFile("NOVA")?.let {
                it.findFile("Logs") ?: it.createDirectory("Logs")
            } ?: tree.createDirectory("NOVA")?.createDirectory("Logs")

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            logFile = folder?.createFile("text/csv", "NOVA_Monitor_$stamp.csv")
            logFile?.uri?.let { uri ->
                contentResolver.openOutputStream(uri, "wa")?.bufferedWriter()?.use {
                    it.write("timestamp,cpu0_mhz,cpu1_mhz,cpu2_mhz,cpu3_mhz,cpu4_mhz,cpu5_mhz,cpu6_mhz,cpu7_mhz,cpu_temp_c,gpu_temp_c,battery_temp_c,ram_available_mb\n")
                }
            }
        } catch (_: Exception) {
            logFile = null
        }
    }

    private fun read(path: String): String? = try {
        File(path).takeIf { it.canRead() }?.readText()?.trim()
    } catch (_: Exception) { null }

    private fun maxThermal(pattern: Regex): String {
        var max = -1
        for (i in 0..60) {
            val base = "/sys/class/thermal/thermal_zone$i"
            val type = read("$base/type") ?: continue
            if (!pattern.matches(type)) continue
            val value = read("$base/temp")?.toIntOrNull() ?: continue
            if (value > max) max = value
        }
        return if (max >= 0) String.format(Locale.US, "%.1f", max / 1000.0) else "NA"
    }

    private fun sample(): String {
        val frequencies = (0..7).map { cpu ->
            val value = read("/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq")
                ?.toLongOrNull()
            value?.let { (it / 1000).toString() } ?: "NA"
        }
        val ram = try {
            File("/proc/meminfo").useLines { lines ->
                lines.firstOrNull { it.startsWith("MemAvailable:") }
                    ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull()
                    ?.div(1024)?.toString()
            } ?: "NA"
        } catch (_: Exception) { "NA" }

        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val values = listOf(
            time, *frequencies.toTypedArray(),
            maxThermal(Regex("apc[0-9]+-cpu[0-9]+-usr")),
            maxThermal(Regex("gpu[0-9]+-usr")),
            maxThermal(Regex("battery|bms")), ram
        )
        return values.joinToString(",")
    }

    private fun appendGameSample(row: String) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val packageName = prefs.getString("active_game_package", null) ?: return

        try {
            if (gameLogPackage != packageName || gameLogFile == null) {
                val raw = prefs.getString(TREE_URI, null) ?: return
                val tree = DocumentFile.fromTreeUri(this, Uri.parse(raw)) ?: return
                val nova = tree.findFile("NOVA") ?: tree.createDirectory("NOVA") ?: return
                val history = nova.findFile("GameHistory")
                    ?: nova.createDirectory("GameHistory") ?: return
                val safe = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val folder = history.findFile(safe) ?: history.createDirectory(safe) ?: return
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = folder.createFile("text/csv", "${safe}_sensors_$stamp.csv") ?: return

                contentResolver.openOutputStream(file.uri, "wa")?.bufferedWriter()?.use {
                    it.write("timestamp,cpu0_mhz,cpu1_mhz,cpu2_mhz,cpu3_mhz,cpu4_mhz,cpu5_mhz,cpu6_mhz,cpu7_mhz,cpu_temp_c,gpu_temp_c,battery_temp_c,ram_available_mb\n")
                }
                gameLogFile = file
                gameLogPackage = packageName
            }

            gameLogFile?.uri?.let { uri ->
                contentResolver.openOutputStream(uri, "wa")?.bufferedWriter()?.use {
                    it.append(row).append('\n')
                }
            }
        } catch (_: Exception) {
            gameLogFile = null
            gameLogPackage = null
        }
    }

    private fun monitorLoop() {
        while (active.get()) {
            try {
                val row = sample()
                logFile?.uri?.let { uri ->
                    contentResolver.openOutputStream(uri, "wa")?.bufferedWriter()?.use {
                        it.append(row).append('\n')
                    }
                }
                appendGameSample(row)
                Thread.sleep(5000)
            } catch (_: InterruptedException) {
                break
            } catch (_: Exception) {
                Thread.sleep(5000)
            }
        }
    }

    private fun stopMonitor() {
        active.set(false)
        worker?.interrupt()
        worker = null
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putBoolean("running", false).apply()
    }

    override fun onDestroy() {
        stopMonitor()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
