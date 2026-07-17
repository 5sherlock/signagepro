package com.signagepro.player.engine

import android.app.ActivityManager
import android.content.Context
import java.io.File

/**
 * 하트비트에 실어 보낼 CPU/메모리 사용률 측정.
 * - CPU: /proc/stat 두 시점 비교 (델타). 첫 호출은 prime용으로 0 반환.
 * - Mem: ActivityManager.MemoryInfo
 */
class SystemMetrics(private val context: Context) {

    @Volatile private var prevTotal: Long = 0L
    @Volatile private var prevIdle: Long = 0L

    fun cpuUsage(): Float = try {
        val (total, idle) = readStat()
        if (prevTotal == 0L) {
            prevTotal = total
            prevIdle = idle
            0f
        } else {
            val totalDelta = total - prevTotal
            val idleDelta = idle - prevIdle
            prevTotal = total
            prevIdle = idle
            if (totalDelta <= 0) 0f
            else ((totalDelta - idleDelta).toFloat() / totalDelta.toFloat() * 100f)
                .coerceIn(0f, 100f)
        }
    } catch (_: Exception) { 0f }

    fun memUsage(): Float = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        if (info.totalMem <= 0L) 0f
        else ((info.totalMem - info.availMem).toFloat() / info.totalMem.toFloat() * 100f)
            .coerceIn(0f, 100f)
    } catch (_: Exception) { 0f }

    fun cpuTemperature(): Float {
        // Android 7+(API24): sysfs thermal이 SELinux(enforcing)로 untrusted_app에 막히는 기기(큐버 QR5G
        // 등 Android 11)에선 이 경로가 유일하다. HardwarePropertiesManager는 Device Owner면 호출 가능.
        // (크라이저 RK3229/Android 5.1은 API<24라 여기 안 들어가고 아래 sysfs 폴백으로 읽힌다)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                val hpm = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE)
                        as? android.os.HardwarePropertiesManager
                val temps = hpm?.getDeviceTemperatures(
                    android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                    android.os.HardwarePropertiesManager.TEMPERATURE_CURRENT
                )
                val t = temps?.firstOrNull { it in 10f..120f }
                if (t != null) return t
            } catch (_: Throwable) { /* DO 아님/미지원 → sysfs 폴백 */ }
        }
        // 폴백: sysfs (permissive ROM — 크라이저 Android 5.1 등)
        return try {
            val paths = arrayOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/class/hwmon/hwmon0/temp1_input"
            )
            var temp = 0f
            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    val raw = file.readText().trim().toFloatOrNull() ?: continue
                    temp = if (raw > 1000) raw / 1000f else raw
                    if (temp in 10f..120f) break
                }
            }
            temp
        } catch (_: Exception) { 0f }
    }

    fun diskSpace(): Pair<Long, Long> = try {
        val stat = android.os.StatFs(context.filesDir.absolutePath)
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val total = stat.blockCountLong * stat.blockSizeLong
        free to total
    } catch (_: Exception) { 0L to 0L }

    fun ramSpace(): Pair<Long, Long> = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        info.availMem to info.totalMem
    } catch (_: Exception) { 0L to 0L }

    private fun readStat(): Pair<Long, Long> {
        val line = File("/proc/stat").bufferedReader().use { it.readLine() }
        // 형식: "cpu user nice system idle iowait irq softirq steal ..."
        val values = line.split(Regex("\\s+")).drop(1).take(8).map { it.toLong() }
        val idle = values[3] + values.getOrElse(4) { 0L }   // idle + iowait
        val total = values.sum()
        return total to idle
    }
}
