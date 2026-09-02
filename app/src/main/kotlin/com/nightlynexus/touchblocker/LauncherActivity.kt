package com.nightlynexus.touchblocker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class LauncherActivity :
  Activity(),
  FloatingViewStatus.Listener {
  private lateinit var floatingViewStatus: FloatingViewStatus
  private lateinit var floatingLockViewSizeStatus: FloatingLockViewSizeStatus
  private lateinit var accessibilityPermissionRequestTracker: AccessibilityPermissionRequestTracker
  private lateinit var brandIcon: View
  private lateinit var buttonsContainerView: View
  private lateinit var enableButton: TextView
  private lateinit var sizeControls: View
  private lateinit var sizeSmallButton: View
  private lateinit var sizeNormalButton: View
  private lateinit var sizeLargeButton: View
  private lateinit var aboutButton: View
  private lateinit var launcherRoot: View
  private var permissionDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    val application = application as TouchBlockerApplication
    floatingViewStatus = application.floatingViewStatus
    floatingLockViewSizeStatus = application.floatingLockViewSizeStatus
    accessibilityPermissionRequestTracker = application.accessibilityPermissionRequestTracker

    installSplashScreen()

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launcher)
    brandIcon = findViewById(R.id.brand_icon)
    enableButton = findViewById(R.id.enable)
    buttonsContainerView = findViewById(R.id.buttons_container)
    sizeControls = findViewById(R.id.size_controls)
    sizeSmallButton = findViewById(R.id.size_small)
    sizeNormalButton = findViewById(R.id.size_normal)
    sizeLargeButton = findViewById(R.id.size_large)
    aboutButton = findViewById(R.id.about_button)
    launcherRoot = findViewById(R.id.launcher_root)

    if (floatingViewStatus.added) {
      onFloatingViewAdded()
    } else if (floatingViewStatus.permissionGranted) {
      onFloatingViewRemoved()
    } else {
      onFloatingViewPermissionRevoked()
    }

    sizeSmallButton.setOnClickListener {
      setSize(FloatingLockViewSizeStatus.sizeMultiplierMin)
    }
    sizeNormalButton.setOnClickListener {
      setSize(1f)
    }
    sizeLargeButton.setOnClickListener {
      setSize(FloatingLockViewSizeStatus.sizeMultiplierMax)
    }

    aboutButton.setOnClickListener {
      showAboutDialog()
    }

    floatingViewStatus.addListener(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    permissionDialog?.dismiss()
    floatingViewStatus.removeListener(this)
  }

  private fun setSize(sizeMultiplier: Float) {
    floatingLockViewSizeStatus.setSizeMultiplier(sizeMultiplier)
  }

  override fun onFloatingViewAdded() {
    launcherRoot.setBackgroundResource(R.color.window_background_white)
    buttonsContainerView.setBackgroundResource(R.color.window_background_white)
    enableButton.setText(R.string.enable_button_remove_floating)
    enableButton.setOnClickListener {
      floatingViewStatus.setAdded(false)
    }
    sizeControls.visibility = View.VISIBLE
  }

  override fun onFloatingViewRemoved() {
    launcherRoot.setBackgroundResource(R.color.window_background)
    buttonsContainerView.setBackgroundResource(R.color.window_background)
    enableButton.setText(R.string.enable_button_add_floating)
    enableButton.setOnClickListener {
      floatingViewStatus.setAdded(true)
    }
    sizeControls.visibility = View.VISIBLE
  }

  override fun onFloatingViewLocked() {
    // No-op.
  }

  override fun onFloatingViewUnlocked() {
    // No-op.
  }

  override fun onFloatingViewPermissionGranted() {
    permissionDialog?.dismiss()
    onFloatingViewRemoved()
  }

  override fun onFloatingViewPermissionRevoked() {
    launcherRoot.setBackgroundResource(R.color.window_background)
    buttonsContainerView.setBackgroundResource(R.color.window_background)
    enableButton.setText(R.string.enable_button_accessibility_service)
    enableButton.setOnClickListener {
      showPermissionDialog()
    }
    sizeControls.visibility = View.GONE
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
