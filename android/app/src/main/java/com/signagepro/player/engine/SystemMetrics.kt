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

    fun cpuTemperature(): Float = try {
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
