package com.nova.gamingsuite.security

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit

data class RootCheckResult(
    val status: String,
    val detail: String
)

object RootAccessManager {

    fun requestAccess(callback: (RootCheckResult) -> Unit) {
        Thread {
            val result = try {
                val process = ProcessBuilder("su", "-c", "id")
                    .redirectErrorStream(true)
                    .start()

                val finished = process.waitFor(60, TimeUnit.SECONDS)

                if (!finished) {
                    process.destroyForcibly()
                    RootCheckResult(
                        "TIMEOUT",
                        "Root authorization timed out. You can try again."
                    )
                } else {
                    val output = process.inputStream
                        .bufferedReader()
                        .use { it.readText() }
                        .trim()

                    val granted = process.exitValue() == 0 &&
                        output.contains("uid=0")

                    when {
                        granted -> RootCheckResult(
                            "GRANTED",
                            "Root access verified successfully."
                        )
                        output.contains("denied", ignoreCase = true) ||
                            output.contains("拒绝") -> RootCheckResult(
                            "DENIED",
                            "Root permission was denied by the user."
                        )
                        else -> RootCheckResult(
                            "UNAVAILABLE",
                            if (output.isBlank())
                                "Root access is unavailable or was not granted."
                            else output.take(180)
                        )
                    }
                }
            } catch (_: Exception) {
                RootCheckResult(
                    "UNAVAILABLE",
                    "The su command is unavailable or could not be started."
                )
            }

            Handler(Looper.getMainLooper()).post {
                callback(result)
            }
        }.apply {
            name = "NOVA-Root-Authorization"
            isDaemon = true
            start()
        }
    }
}
