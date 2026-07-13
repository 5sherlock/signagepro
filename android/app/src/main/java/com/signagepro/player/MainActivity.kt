package com.signagepro.player

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import androidx.appcompat.app.AppCompatActivity
import com.signagepro.player.databinding.ActivityMainBinding
import com.signagepro.player.engine.PlayerCoordinator
import com.signagepro.player.render.MediaRenderer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var coordinator: PlayerCoordinator? = null

    private var lastHotspotClickTime = 0L

    private var autoReturnTimer: CountDownTimer? = null
    private var idleTimer: CountDownTimer? = null
    private var countdownOverlay: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 프로세스 보호용 FG 서비스 — OOM/백그라운드 회수 시 자동 부활
        PlayerForegroundService.start(this)

        setupAdminHotspot()

        val config = PlayerApp.instance.config
        if (config.isConfigured()) startKiosk() else showSetup()
    }

    // ── 관리자 진입 핫스팟 ──────────────────────────────────────────────────
    /**
     * 좌상단 투명 영역(80×80dp)을 [DOUBLE_CLICK_INTERVAL_MS] 이내로 2회 클릭하면 관리자 메뉴.
     * 마우스 클릭 / 터치 모두 동작.
     */
    private fun setupAdminHotspot() {
        binding.adminHotspot.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastHotspotClickTime <= DOUBLE_CLICK_INTERVAL_MS) {
                lastHotspotClickTime = 0L
                showAdminDialog()
            } else {
                lastHotspotClickTime = now
            }
        }
    }

    private fun getLocalIpAddress(): String = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "알 수 없음"
    } catch (e: Exception) { "알 수 없음" }

    private fun showAdminDialog() {
        AlertDialog.Builder(this)
            .setTitle("관리자 메뉴  ·  SignagePro")
            .setItems(arrayOf(
                "📁  파일 탐색기",
                "⚙️  설정 변경",
                "📋  기기 정보",
                "🔄  앱 재시작",
                "📱  안드로이드 설정",
                "✕  취소"
            )) { _, which ->
                when (which) {
                    0 -> openFileManager()
                    1 -> { coordinator?.stop(); coordinator = null; showSetup() }
                    2 -> showDeviceInfoDialog()
                    3 -> { finish(); startActivity(intent) }
                    4 -> goToLauncher()
                }
            }
            .show()
    }

    private fun showDeviceInfoDialog() {
        val config = PlayerApp.instance.config
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
        }.getOrDefault("-")
        val msg = "기기 ID  :  ${config.deviceId ?: "-"}\n" +
                  "앱 버전  :  v$version\n" +
                  "서버 URL :  ${config.serverUrl ?: "-"}\n" +
                  "IP 주소  :  ${getLocalIpAddress()}"
        AlertDialog.Builder(this)
            .setTitle("기기 정보")
            .setMessage(msg)
            .setPositiveButton("확인", null)
            .show()
    }

    /**
     * 안드로이드 설정 앱을 연다(개발자옵션·무선디버깅·앱관리 등 현장 접근용).
     * ⚠️ 기본 홈(SignagePro)은 절대 바꾸지 않는다 — 홈 설정을 열면 사용자가 런처를 바꿔
     * 키오스크 자동복귀가 깨진다. 설정에서 작업 후 홈 버튼만 누르면 SignagePro 로 복귀.
     */
    private fun goToLauncher() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(this, "설정을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /** USB 스토리지 목록 → 파일 탐색 → APK 설치 */
    private fun openFileManager() {
        val roots = mutableListOf<File>()

        // /storage, /mnt 직접 포함 (항상 탐색 가능한 루트)
        listOf("/storage", "/mnt", "/sdcard").forEach { path ->
            val f = File(path)
            if (f.exists() && f.canRead()) roots.add(f)
        }

        // /storage, /mnt 하위 디렉토리도 직접 노출
        val skipNames = setOf("self", "emulated", "user", "runtime", "shell", "asec", "obb", "secure")
        listOf("/storage", "/mnt").forEach { base ->
            File(base).listFiles()?.filter { f ->
                f.isDirectory && f.canRead() && f.name !in skipNames
            }?.forEach { if (!roots.contains(it)) roots.add(it) }
        }

        // 알려진 USB 경로 보조 추가
        listOf(
            "/storage/udisk0", "/storage/usb_storage", "/mnt/usb_storage",
            "/storage/external_storage/sda1", "/mnt/media_rw/udisk0",
            "/mnt/external_sd", "/storage/usbotg",
        ).map(::File).filter { it.exists() && it.isDirectory && !roots.contains(it) }
         .forEach { roots.add(it) }

        val sorted = roots.sortedBy { it.path }
        val options = (sorted.map { "📂  ${it.path}" } + "📱  시스템 파일 탐색기").toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("저장소 선택 (${sorted.size}개)")
            .setItems(options) { _, which ->
                if (which < sorted.size) browseDirectory(sorted[which])
                else openSystemFileManager()
            }
            .show()
    }

    private fun browseDirectory(dir: File) {
        val files = dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

        if (files.isEmpty()) {
            Toast.makeText(this, "비어있음: ${dir.path}", Toast.LENGTH_SHORT).show()
            return
        }

        val names = files.map {
            when {
                it.isDirectory -> "📁  ${it.name}/"
                it.name.endsWith(".apk", ignoreCase = true) -> "📦  ${it.name}  (${humanSize(it.length())})"
                else -> "📄  ${it.name}  (${humanSize(it.length())})"
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(dir.name.ifBlank { dir.path })
            .setItems(names) { _, which ->
                val sel = files[which]
                when {
                    sel.isDirectory -> browseDirectory(sel)
                    sel.name.endsWith(".apk", ignoreCase = true) -> installApkFromFile(sel)
                    else -> Toast.makeText(this, "APK 파일을 선택하세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("↑ 상위") { _, _ ->
                val parent = dir.parentFile
                if (parent != null && parent.canRead()) browseDirectory(parent)
            }
            .show()
    }

    private fun humanSize(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }

    private fun installApkFromFile(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "설치 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openSystemFileManager() {
        val intents = listOf(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            packageIntent("com.estrongs.android.pop"),
            packageIntent("com.speedsoftware.rootexplorer"),
            packageIntent("com.mixplorer.silver"),
            packageIntent("com.android.documentsui"),
        )
        for (i in intents) {
            if (i == null) continue
            try { startActivity(i); return } catch (_: Exception) {}
        }
        Toast.makeText(this, "파일 탐색기를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun packageIntent(pkg: String): Intent? {
        return packageManager.getLaunchIntentForPackage(pkg)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    // ── 마우스/입력 이벤트 차단 ─────────────────────────────────────────────

    /**
     * 마우스 우클릭(BUTTON_SECONDARY) 차단.
     * 차단하지 않으면 일부 STB ROM에서 파일 탐색기 등 시스템 앱이 열림.
     */
    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_BUTTON_PRESS &&
            (ev.buttonState and MotionEvent.BUTTON_SECONDARY) != 0
        ) {
            return true  // 이벤트 소비 — 시스템에 전달하지 않음
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    /**
     * 컨텍스트 메뉴(길게 누르기 → 시스템 팝업) 비활성화.
     * 우클릭 외에도 롱탭으로 시스템 메뉴가 열리는 경우 방지.
     */
    override fun openContextMenu(view: View) {
        // 의도적으로 비활성화 — 키오스크 모드에서 시스템 컨텍스트 메뉴 차단
    }

    /**
     * 키오스크 모드 중 MENU 키 차단 (일부 STB 리모컨의 MENU 버튼이 시스템 메뉴를 여는 경우).
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (coordinator != null && keyCode == KeyEvent.KEYCODE_MENU) {
            return true  // 키오스크 모드에서 MENU 키 소비
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 뒤로가기(리모컨 BACK / 마우스 우클릭) → 바탕화면(런처)으로 이동.
     * SignagePro 가 기본 홈이라 뒤로가기가 그냥 무시되므로 런처를 명시적으로 띄운다.
     * 이후 [onStop] 이 [AUTO_RETURN_MS](30초) 타이머를 걸어 자동으로 플레이어가 복귀한다.
     * → 관리자는 잠깐 바탕화면에서 작업, 방치되면 콘텐츠로 자동 복귀(무인 안전).
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (coordinator != null) {
            // launcher3 는 LAUNCHER 아이콘이 없고 HOME 카테고리만 있으므로
            // getLaunchIntentForPackage 로는 못 얻는다 → MAIN+HOME+setPackage 로 명시 실행.
            try {
                startActivity(Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    setPackage("com.android.launcher3")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            } catch (_: Exception) { /* 런처 없으면 아래 기본 동작 */ }
        }
        super.onBackPressed()
    }

    // ── 기존 로직 ────────────────────────────────────────────────────────────

    private fun showSetup() {
        binding.setupContainer.visibility = View.VISIBLE
        binding.kioskContainer.visibility = View.GONE

        val config = PlayerApp.instance.config
        binding.deviceIdInput.setText(config.deviceId ?: "")
        binding.serverUrlInput.setText(config.serverUrl ?: "http://192.168.0.25:3001")
        binding.deviceSecretInput.setText(config.deviceSecret ?: "")

        binding.saveButton.setOnClickListener {
            val deviceId = binding.deviceIdInput.text.toString().trim()
            val serverUrl = binding.serverUrlInput.text.toString().trim().trimEnd('/')
            val secret = binding.deviceSecretInput.text.toString().trim()

            if (deviceId.isEmpty() || serverUrl.isEmpty() || secret.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            config.deviceId = deviceId
            config.serverUrl = serverUrl
            config.deviceSecret = secret

            startKiosk()
        }
    }

    private fun requestDeviceAdminIfNeeded() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, SignageDeviceAdmin::class.java)
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "화면 자동 켜기/끄기 스케줄 기능을 사용하려면 기기 관리자 권한이 필요합니다.")
            }
            startActivity(intent)
        }
    }

    /** Device Owner면 Tailscale를 always-on VPN으로 강제 → 재부팅 후 자동 기동(상시연결). */
    private fun enforceTailscaleAlwaysOn() {
        if (Build.VERSION.SDK_INT < 24) return
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(packageName)) return
        val adminComponent = ComponentName(this, SignageDeviceAdmin::class.java)
        val tsPkg = "com.tailscale.ipn"
        try {
            packageManager.getPackageInfo(tsPkg, 0)  // 설치돼 있을 때만
            // lockdown=false: Tailscale 미기동 시에도 일반 네트워크(LAN) 폴백 유지
            dpm.setAlwaysOnVpnPackage(adminComponent, tsPkg, false)
            android.util.Log.i("MainActivity", "Tailscale always-on VPN 설정 완료")
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.i("MainActivity", "Tailscale 미설치 — always-on 스킵")
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "always-on VPN 설정 실패: ${e.message}")
        }
    }

    private fun startKiosk() {
        binding.setupContainer.visibility = View.GONE
        binding.kioskContainer.visibility = View.VISIBLE
        applyImmersiveMode()
        requestDeviceAdminIfNeeded()
        enforceTailscaleAlwaysOn()

        val renderer = MediaRenderer(this, binding.layerA, binding.layerB)
        coordinator = PlayerCoordinator(
            context = applicationContext,
            config = PlayerApp.instance.config,
            renderer = renderer,
            onStatus = { msg ->
                runOnUiThread {
                    binding.statusText.text = msg
                    binding.statusText.visibility = if (msg.isBlank()) View.GONE else View.VISIBLE
                }
            },
            onDebug = { msg ->
                runOnUiThread {
                    binding.debugText.text = msg
                    binding.debugText.visibility = if (msg.isBlank()) View.GONE else View.VISIBLE
                }
            },
            onTicker = { cfg ->
                runOnUiThread {
                    if (cfg != null && cfg.enabled && cfg.text.isNotBlank()) {
                        binding.tickerView.configure(cfg, PlayerApp.instance.ntp)
                    } else {
                        binding.tickerView.clear()
                    }
                }
            },
            tickerCycleMsProvider = { binding.tickerView.lastCycleMs }
        )
        coordinator!!.start()
    }

    override fun onStop() {
        super.onStop()
        // 키오스크 중 바탕화면(런처)으로 나가면 카운트다운 오버레이 표시 → 0초에 자동 복귀.
        if (coordinator != null) startAutoReturnCountdown()
    }

    override fun onStart() {
        super.onStart()
        cancelAutoReturn()
    }

    override fun onDestroy() {
        cancelAutoReturn()
        coordinator?.stop()
        super.onDestroy()
    }

    /**
     * 바탕화면(런처/설정)으로 나갔을 때 자동 복귀.
     * - 유휴(무조작)면 [AUTO_RETURN_MS](30초) 카운트다운 오버레이를 표시하고 0초에 복귀.
     * - 사용자가 조작하면(오버레이 밖 터치=ACTION_OUTSIDE) 카운트 취소·오버레이 숨김,
     *   [IDLE_BEFORE_COUNTDOWN_MS](2.5초) 무조작 후 다시 카운트 시작.
     *   → 설정 등을 만지는 동안엔 튀어나오지 않고, 손을 떼면 곧 복귀 카운트가 재개된다.
     * SYSTEM_ALERT_WINDOW 권한이 있어야 오버레이 + 백그라운드 액티비티 시작이 동작한다.
     */
    private fun startAutoReturnCountdown() {
        cancelAutoReturn()
        val overlay = TextView(this).apply {
            setBackgroundColor(0xCC000000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(48, 28, 48, 28)
            visibility = View.GONE
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_OUTSIDE || e.action == MotionEvent.ACTION_DOWN) {
                    onUserActivity()
                }
                false
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        // Activity 가 stop 중이면 Activity WindowManager 는 토큰 문제로 실패 → applicationContext 사용.
        val wm = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            wm.addView(overlay, lp)
            countdownOverlay = overlay
        } catch (e: Exception) {
            android.util.Log.w("AutoReturn", "오버레이 표시 실패: ${e.message}")
        }

        startCountdown()  // 나가자마자(무조작) 카운트 시작
    }

    /** 사용자 조작 감지 → 카운트 중단·오버레이 숨김 후 2.5초 무조작 시 재개. */
    private fun onUserActivity() {
        autoReturnTimer?.cancel(); autoReturnTimer = null
        countdownOverlay?.apply { text = ""; visibility = View.GONE }
        idleTimer?.cancel()
        idleTimer = object : CountDownTimer(IDLE_BEFORE_COUNTDOWN_MS, IDLE_BEFORE_COUNTDOWN_MS) {
            override fun onTick(msLeft: Long) {}
            override fun onFinish() { startCountdown() }
        }.start()
    }

    /** 30초 복귀 카운트다운 시작(오버레이 표시). */
    private fun startCountdown() {
        autoReturnTimer?.cancel()
        countdownOverlay?.visibility = View.VISIBLE
        autoReturnTimer = object : CountDownTimer(AUTO_RETURN_MS, 1000L) {
            override fun onTick(msLeft: Long) {
                countdownOverlay?.text = "${msLeft / 1000 + 1}초 후 콘텐츠로 돌아갑니다"
            }
            override fun onFinish() {
                cancelAutoReturn()
                startActivity(Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            }
        }.start()
    }

    private fun cancelAutoReturn() {
        autoReturnTimer?.cancel(); autoReturnTimer = null
        idleTimer?.cancel(); idleTimer = null
        removeCountdownOverlay()
    }

    private fun removeCountdownOverlay() {
        countdownOverlay?.let {
            try { (applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {}
        }
        countdownOverlay = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && coordinator != null) applyImmersiveMode()
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    companion object {
        /** 우상단 핫스팟 더블클릭 인정 간격 (ms) */
        private const val DOUBLE_CLICK_INTERVAL_MS = 600L
        /** 키오스크 이탈 후 자동 복귀 대기 시간 (ms) */
        private const val AUTO_RETURN_MS = 30_000L
        private const val IDLE_BEFORE_COUNTDOWN_MS = 2_500L  // 조작 후 이만큼 무조작이면 카운트 재개
    }
}
