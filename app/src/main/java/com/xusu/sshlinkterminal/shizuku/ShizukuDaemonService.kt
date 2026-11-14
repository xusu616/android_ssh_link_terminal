package com.xusu.sshlinkterminal.shizuku

import android.util.Log
import androidx.annotation.Keep
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Privileged Shizuku UserService running in the Shizuku shell process (daemon=true).
 * Applies Doze/AppOps/Standby Bucket exemptions and monitors app process heartbeat.
 */
@Keep
class ShizukuDaemonService : IShizukuDaemonService.Stub() {

    private val watchdogRunning = AtomicBoolean(false)
    private val lastHeartbeatMs = AtomicLong(System.currentTimeMillis())
    private val lastReviveAttemptMs = AtomicLong(0L)
    private var watchdogThread: Thread? = null

    @Volatile
    private var watchedPackage: String = ""

    @Volatile
    private var watchedServiceClass: String = ""

    override fun getVersion(): Int = SERVICE_VERSION

    override fun applyKeepAliveHardening(packageName: String): String {
        val safePkg = sanitizePackageName(packageName) ?: return JSONObject()
            .put("ok", false)
            .put("error", "Invalid package name")
            .toString()

        val commands = listOf(
            "deviceidle" to "cmd deviceidle whitelist +$safePkg",
            "bucket" to "am set-standby-bucket $safePkg active",
            "runInBg" to "cmd appops set $safePkg RUN_IN_BACKGROUND allow",
            "runAnyInBg" to "cmd appops set $safePkg RUN_ANY_IN_BACKGROUND allow",
            "wakeLock" to "cmd appops set $safePkg WAKE_LOCK allow"
        )
        val results = JSONObject()
        var allOk = true
        for ((key, cmd) in commands) {
            val out = runShell(cmd)
            results.put(key, out.exitCode == 0)
            results.put("${key}_msg", out.output.take(180))
            if (out.exitCode != 0 && key != "runInBg") {
                allOk = false
            }
        }
        results.put("ok", allOk)
        return results.toString()
    }

    override fun queryHardeningStatus(packageName: String): String {
        val safePkg = sanitizePackageName(packageName) ?: return JSONObject().toString()
        val whitelistOut = runShell("cmd deviceidle whitelist")
        val dozeWhitelisted = isPackageInWhitelistOutput(whitelistOut.output, safePkg)

        val bucketOut = runShell("am get-standby-bucket $safePkg")
        val bucketRaw = bucketOut.output.trim().lineSequence().lastOrNull()?.trim().orEmpty()
        val bucketCode = bucketRaw.toIntOrNull()
        val standbyActive = bucketCode != null && bucketCode <= 10

        val appOpsOut = runShell("cmd appops get $safePkg RUN_ANY_IN_BACKGROUND")
        val appOpsAllowed = appOpsOut.output.contains("allow", ignoreCase = true)

        return JSONObject()
            .put("dozeWhitelisted", dozeWhitelisted)
            .put("standbyActive", standbyActive)
            .put("standbyBucket", bucketRaw.ifBlank { "?" })
            .put("appOpsAllowed", appOpsAllowed)
            .put("watchdogActive", watchdogRunning.get())
            .toString()
    }

    override fun startWatchdog(packageName: String, serviceClassName: String) {
        val safePkg = sanitizePackageName(packageName) ?: return
        val safeSvc = sanitizeServiceClass(serviceClassName) ?: return
        watchedPackage = safePkg
        watchedServiceClass = safeSvc
        lastHeartbeatMs.set(System.currentTimeMillis())

        if (watchdogRunning.compareAndSet(false, true)) {
            watchdogThread = Thread({
                Log.i(TAG, "Shizuku watchdog started for $watchedPackage")
                while (watchdogRunning.get()) {
                    try {
                        Thread.sleep(WATCHDOG_POLL_MS)
                        if (!watchdogRunning.get()) break
                        val elapsedSinceBeat = System.currentTimeMillis() - lastHeartbeatMs.get()
                        val elapsedSinceRevive = System.currentTimeMillis() - lastReviveAttemptMs.get()
                        if (elapsedSinceBeat > HEARTBEAT_TIMEOUT_MS && elapsedSinceRevive > REVIVE_COOLDOWN_MS) {
                            val pidOut = runShell("pidof $watchedPackage")
                            if (pidOut.output.isBlank()) {
                                Log.w(TAG, "App process dead; reviving $watchedPackage/$watchedServiceClass")
                                lastReviveAttemptMs.set(System.currentTimeMillis())
                                runShell("am start-foreground-service -n $watchedPackage/$watchedServiceClass -a ACTION_WATCHDOG_REVIVE")
                            } else {
                                // Process is alive; extend timer gently
                                lastHeartbeatMs.set(System.currentTimeMillis())
                            }
                        }
                    } catch (_: InterruptedException) {
                        break
                    } catch (t: Throwable) {
                        Log.w(TAG, "Watchdog loop error", t)
                    }
                }
                Log.i(TAG, "Shizuku watchdog stopped")
            }, "LinkTerm-Shizuku-Watchdog").apply {
                isDaemon = true
                start()
            }
        }
    }

    override fun heartbeat() {
        lastHeartbeatMs.set(System.currentTimeMillis())
    }

    override fun stopWatchdog() {
        watchdogRunning.set(false)
        watchdogThread?.interrupt()
        watchdogThread = null
    }

    override fun destroy() {
        stopWatchdog()
        System.exit(0)
    }

    private data class ShellResult(val exitCode: Int, val output: String)

    private fun runShell(command: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            val merged = buildString {
                if (stdout.isNotBlank()) append(stdout.trim())
                if (stderr.isNotBlank()) {
                    if (isNotEmpty()) append('\n')
                    append(stderr.trim())
                }
            }
            ShellResult(exitCode, merged)
        } catch (e: Exception) {
            ShellResult(-1, e.message ?: "exec failed")
        }
    }

    private fun sanitizePackageName(pkg: String): String? =
        pkg.takeIf { Regex("""^[a-zA-Z0-9._]+$""").matches(it) }

    private fun sanitizeServiceClass(cls: String): String? =
        cls.takeIf { Regex("""^[a-zA-Z0-9._$]+$""").matches(it) }

    companion object {
        private const val TAG = "ShizukuDaemonService"
        private const val SERVICE_VERSION = 1
        private const val WATCHDOG_POLL_MS = 10_000L
        private const val HEARTBEAT_TIMEOUT_MS = 45_000L
        private const val REVIVE_COOLDOWN_MS = 30_000L

        /**
         * Matches Android `cmd deviceidle whitelist` output formats across API levels,
         * e.g. `user,com.example.app,10544`, `system,com.example.app`, or `com.example.app`.
         */
        internal fun isPackageInWhitelistOutput(output: String, packageName: String): Boolean {
            return output.lineSequence().any { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@any false
                line == packageName ||
                    line.split(',').any { it.trim() == packageName } ||
                    line.contains(":$packageName")
            }
        }
    }
}
