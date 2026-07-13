package com.signagepro.player

import android.util.Log

/**
 * 기기마다 다른 su 문법을 흡수하는 root 실행 헬퍼.
 *
 * - Magisk/전통 su (예: dev-204 Ultracube U4X): `su -c "cmd"`
 * - toybox/AOSP userdebug su (예: 큐버 QR5G-M110S): `su 0 sh -c "cmd"`
 *   ↳ 이 su 는 `su -c` 를 "invalid uid/gid '-c'" 로 거부하므로 UID 를 먼저 준다.
 *
 * 여러 문법을 순서대로 시도해 하나라도 exit 0 이면 성공으로 본다.
 * 모두 실패하면 비루팅(또는 su 미지원)으로 간주.
 */
object RootUtil {
    private const val TAG = "RootUtil"

    /**
     * ADB over TCP 5555 를 영구(persist) 활성화.
     *
     * `service.adb.tcp.port`(비영구)만으로는 RK3566/Android11 에서 무선 디버깅(TLS)과 충돌해
     * 재부팅 후 5555 가 안 열린다. `persist.adb.tcp.port` 를 심으면 재부팅 시 무선 디버깅이
     * 꺼지면서 adbd 가 평문 5555 로 자동 기동한다. 한 번 심으면 이후 부팅마다 유지.
     */
    fun enableAdbTcp5555(): Boolean =
        runAsRoot("setprop persist.adb.tcp.port 5555; setprop service.adb.tcp.port 5555; stop adbd; start adbd")

    private fun variants(cmd: String) = listOf(
        arrayOf("su", "-c", cmd),            // Magisk/전통 su
        arrayOf("su", "0", "sh", "-c", cmd), // toybox/AOSP userdebug su
        arrayOf("su", "root", "sh", "-c", cmd),
    )

    /** su 가 문법(-c) 자체를 거부한 흔적인지 판별 → 다음 variant 로 넘어갈 신호. */
    private fun isSuSyntaxReject(stderr: String) =
        stderr.contains("invalid uid", true) || stderr.contains("inaccessible", true) ||
            stderr.contains("not found", true)

    fun runAsRoot(cmd: String): Boolean {
        for (v in variants(cmd)) {
            try {
                if (Runtime.getRuntime().exec(v).waitFor() == 0) {
                    Log.i(TAG, "root 실행 성공: ${v[1]}")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "su 시도 실패(${v[1]}): ${e.message}")
            }
        }
        Log.w(TAG, "모든 su 문법 실패 — 비루팅이거나 su 미지원")
        return false
    }

    /**
     * root 로 실행하고 STDOUT/STDERR 를 반환(대시보드 run_cmd 등 출력이 필요한 경우).
     * su 문법 거부(invalid uid 등)면 다음 variant 로 넘어간다.
     */
    fun runAsRootWithOutput(cmd: String): String {
        for (v in variants(cmd)) {
            try {
                val proc = Runtime.getRuntime().exec(v)
                val out = proc.inputStream.bufferedReader().readText()
                val err = proc.errorStream.bufferedReader().readText()
                proc.waitFor()
                if (!isSuSyntaxReject(err)) return "STDOUT:\n$out\nSTDERR:\n$err"
            } catch (e: Exception) {
                Log.w(TAG, "su 출력 시도 실패(${v[1]}): ${e.message}")
            }
        }
        return "Error: root 실행 실패(su 문법/권한)"
    }
}
