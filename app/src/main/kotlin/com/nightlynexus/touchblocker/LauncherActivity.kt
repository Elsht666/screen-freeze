package com.nightlynexus.touchblocker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class LauncherActivity :
  Activity(),
  FloatingViewStatus.Listener {
  private lateinit var floatingViewStatus: FloatingViewStatus
  private lateinit var floatingLockBackgroundColorStatus: FloatingLockBackgroundColorStatus
  private lateinit var accessibilityPermissionRequestTracker: AccessibilityPermissionRequestTracker
  private lateinit var brandIcon: View
  private lateinit var buttonsContainerView: View
  private lateinit var enableButton: TextView
  private lateinit var aboutButton: View
  private lateinit var launcherRoot: View
  private lateinit var serviceDownBanner: TextView
  private lateinit var colorPreview: View
  private lateinit var colorPickerDialog: AlertDialog
  private var permissionDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    val application = application as TouchBlockerApplication
    floatingViewStatus = application.floatingViewStatus
    floatingLockBackgroundColorStatus = application.floatingLockBackgroundColorStatus
    accessibilityPermissionRequestTracker = application.accessibilityPermissionRequestTracker

    installSplashScreen()

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launcher)
    brandIcon = findViewById(R.id.brand_icon)
    enableButton = findViewById(R.id.enable)
    buttonsContainerView = findViewById(R.id.buttons_container)
    aboutButton = findViewById(R.id.about_button)
    launcherRoot = findViewById(R.id.launcher_root)
    serviceDownBanner = findViewById(R.id.service_down_banner)
    colorPreview = findViewById(R.id.color_preview)

    if (floatingViewStatus.added) {
      setupStartScreen()
      refreshServiceDownBanner()
      // On some OEM ROMs the accessibility service reconnects with a long delay
      // (tens of seconds) after the app restarts. Poll until it comes back so the
      // floating lock is restored automatically; only if it never comes back within
      // a long timeout do we reset the stale "enabled" state.
      pollServiceConnection(0)
    } else if (floatingViewStatus.permissionGranted) {
      setupStartScreen()
    } else {
      onFloatingViewPermissionRevoked()
    }

    aboutButton.setOnClickListener {
      showAboutDialog()
    }

    serviceDownBanner.setOnClickListener {
      startActivity(accessibilityServicesSettingsIntent())
    }

    colorPreview.background = ovalDrawable(floatingLockBackgroundColorStatus.getColor())
    colorPreview.setOnClickListener {
      showColorPickerDialog()
    }

    floatingViewStatus.addListener(this)
  }

  private fun ovalDrawable(color: Int): GradientDrawable {
    val d = GradientDrawable()
    d.shape = GradientDrawable.OVAL
    d.setColor(color)
    d.setStroke((2 * resources.displayMetrics.density).toInt(), Color.LTGRAY)
    return d
  }

  private fun showColorPickerDialog() {
    val grid = GridLayout(this)
    grid.columnCount = 4
    val sizePx = (48 * resources.displayMetrics.density).toInt()
    val gapPx = (16 * resources.displayMetrics.density).toInt()
    for (color in FloatingLockBackgroundColorStatus.presetColors) {
      val circle = View(this)
      circle.background = ovalDrawable(color)
      val lp = GridLayout.LayoutParams()
      lp.width = sizePx
      lp.height = sizePx
      lp.setMargins(gapPx / 2, gapPx / 2, gapPx / 2, gapPx / 2)
      circle.layoutParams = lp
      circle.setOnClickListener {
        floatingLockBackgroundColorStatus.setColor(color)
        colorPreview.background = ovalDrawable(color)
        colorPickerDialog.dismiss()
      }
      grid.addView(circle)
    }
    // Center the color grid inside the dialog.
    val container = LinearLayout(this)
    container.orientation = LinearLayout.VERTICAL
    container.gravity = Gravity.CENTER_HORIZONTAL
    container.setPadding(0, gapPx / 2, 0, gapPx)

    // Custom centered title (the system setTitle is left-aligned in this theme).
    // Its color follows the currently selected lock-screen background color.
    val titleTv = TextView(this)
    titleTv.text = getString(R.string.color_picker_title)
    titleTv.setTextColor(floatingLockBackgroundColorStatus.getColor())
    titleTv.textSize = 20f
    titleTv.gravity = Gravity.CENTER
    titleTv.layoutParams = LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    )
    (titleTv.layoutParams as LinearLayout.LayoutParams).bottomMargin = gapPx
    container.addView(titleTv)

    grid.layoutParams = LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    )
    container.addView(grid)

    val dialog = AlertDialog.Builder(this, R.style.DialogPermissionStyle)
      .setView(container)
      .create()
    colorPickerDialog = dialog
    dialog.show()
  }

  private fun setupStartScreen() {
    launcherRoot.setBackgroundResource(R.color.window_background)
    buttonsContainerView.setBackgroundResource(R.color.window_background)
    enableButton.setText(R.string.enable_button_add_floating)
    enableButton.setOnClickListener {
      if (!floatingViewStatus.added) {
        floatingViewStatus.setAdded(true, source = "launcher_add")
      }
      // Tapping Enable goes straight into film-protection (locked) mode.
      requestLockSoon()
    }
  }

  private fun requestLockSoon() {
    val r = object : Runnable {
      var attempts = 0
      override fun run() {
        if (!floatingViewStatus.added) return
        val running = isAccessibilityServiceRunning()
        if (running) {
          floatingViewStatus.recordDiagnostic("点击开启 服务在运行 发送锁定广播", "launcher")
          sendBroadcast(Intent("com.nightlynexus.touchblocker.ACTION_LOCK"))
        } else if (attempts < 30) {
          attempts++
          floatingViewStatus.recordDiagnostic("点击开启 服务未运行 第${attempts}次等待", "launcher")
          launcherRoot.postDelayed(this, 2000)
        } else {
          floatingViewStatus.recordDiagnostic("点击开启 等待服务超时(60s)", "launcher")
        }
      }
    }
    // Try immediately so the film-protection (locked) screen appears right away when
    // the accessibility service is already connected; only fall back to polling if the
    // service is not connected yet.
    r.run()
  }

  private fun refreshServiceDownBanner() {
    val running = isAccessibilityServiceRunning()
    if (floatingViewStatus.added && !running) {
      serviceDownBanner.visibility = View.VISIBLE
    } else {
      serviceDownBanner.visibility = View.GONE
    }
  }

  private fun pollServiceConnection(attempt: Int) {
    if (!floatingViewStatus.added) return
    if (isAccessibilityServiceRunning()) {
      // Service is up now: make sure the floating lock is shown and UI is correct.
      onFloatingViewAdded()
      requestOverlayRestore()
      refreshServiceDownBanner()
      return
    }
    // ~2s interval; give up after ~90s and reset the stale enabled state.
    if (attempt >= 45) {
      if (floatingViewStatus.added) {
        if (floatingViewStatus.locked) {
          floatingViewStatus.setLocked(false)
        }
        floatingViewStatus.setAdded(false, source = "auto_reset_stale")
      }
      return
    }
    launcherRoot.postDelayed({ pollServiceConnection(attempt + 1) }, 2000)
  }

  private fun isAccessibilityServiceRunning(): Boolean {
    val am = getSystemService(AccessibilityManager::class.java)
    val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabled.any {
      it.resolveInfo.serviceInfo.packageName == packageName &&
        it.resolveInfo.serviceInfo.name == TouchBlockerAccessibilityService::class.java.name
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    permissionDialog?.dismiss()
    // If the app is closed while the floating lock is still "added", reset it to
    // disabled so the next launch shows a clean "Enable" state. Without this, a stale
    // "added" state leaves the floating lock invisible (the OS may not reconnect the
    // accessibility service automatically). Rotation / configuration changes are excluded.
    if (!isChangingConfigurations && floatingViewStatus.added) {
      floatingViewStatus.recordDiagnostic("检测到退出，自动归置为关闭", "auto_reset_exit")
      // Also clear the in-memory "locked" flag: it survives in the still-alive
      // accessibility service, and a stale locked=true blocks the next "Enable" tap
      // (lockBroadcastReceiver only locks when !locked).
      if (floatingViewStatus.locked) {
        floatingViewStatus.setLocked(false)
      }
      floatingViewStatus.setAdded(false, source = "auto_reset_exit")
    }
    floatingViewStatus.removeListener(this)
  }

  private fun requestOverlayRestore() {
    sendBroadcast(
      Intent("com.nightlynexus.touchblocker.ACTION_REQUEST_RESTORE_OVERLAY")
    )
  }

  override fun onFloatingViewAdded() {
    // The floating lock is on screen (overlay). The launcher stays on the dark start
    // screen with the "Enable" button — there is no separate "temporarily removed"
    // screen anymore. Tapping Enable goes straight into locked film mode.
  }

  override fun onFloatingViewRemoved() {
    // Back to the start screen: floating lock removed, launcher stays dark "Enable".
  }

  override fun onFloatingViewLocked() {
    // No-op.
  }

  override fun onFloatingViewUnlocked() {
    // No-op.
  }

  override fun onFloatingViewPermissionGranted() {
    permissionDialog?.dismiss()
    setupStartScreen()
  }

  override fun onFloatingViewPermissionRevoked() {
    launcherRoot.setBackgroundResource(R.color.window_background)
    buttonsContainerView.setBackgroundResource(R.color.window_background)
    enableButton.setText(R.string.enable_button_accessibility_service)
    enableButton.setOnClickListener {
      showPermissionDialog()
    }
  }

  private fun showPermissionDialog() {
    val permissionDialog = AlertDialog.Builder(this, R.style.DialogPermissionStyle)
      .setView(R.layout.dialog_permission)
      .show()
    permissionDialog.findViewById<View>(R.id.dialog_permission_button_confirm)!!.setOnClickListener {
      permissionDialog.dismiss()
      requestPermission()
    }
    permissionDialog.findViewById<View>(R.id.dialog_permission_button_cancel)!!.setOnClickListener {
      permissionDialog.cancel()
    }
    this.permissionDialog = permissionDialog
  }

  private fun showAboutDialog() {
    val aboutDialog = AlertDialog.Builder(this, R.style.DialogPermissionStyle)
      .setView(R.layout.dialog_about)
      .show()
    aboutDialog.findViewById<View>(R.id.about_repo)!!.setOnClickListener {
      val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_repo_url))).addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK
        )
      startActivity(intent)
    }
    aboutDialog.findViewById<View>(R.id.about_button_close)!!.setOnClickListener {
      aboutDialog.dismiss()
    }
  }

  private fun requestPermission() {
    accessibilityPermissionRequestTracker.recordAccessibilityPermissionRequest()
    startActivity(
      accessibilityServicesSettingsIntent().addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
          Intent.FLAG_ACTIVITY_SINGLE_TOP
      )
    )
  }

  override fun onToggle() {
    // No-op.
  }
}
