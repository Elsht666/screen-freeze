package com.nightlynexus.touchblocker

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class FloatingViewStatus(
  private val sharedPreferences: SharedPreferences,
  permissionGranted: Boolean
) {
  interface Listener {
    fun onFloatingViewAdded()
    fun onFloatingViewRemoved()
    fun onFloatingViewLocked()
    fun onFloatingViewUnlocked()
    fun onFloatingViewPermissionGranted()
    fun onFloatingViewPermissionRevoked()
    fun onToggle()
  }

  private val addedKey = "floating_enabled"
  private val logKey = "floating_log"
  private val maxLogEntries = 40

  // Persisted so that when the OS restarts the accessibility service (e.g. after the
  // process was reclaimed in the background), the floating lock is restored instead of
  // silently disappearing while the accessibility switch remains enabled.
  var added = sharedPreferences.getBoolean(addedKey, false)
    private set

  init {
    // Diagnostic: record what persisted state we read at process start, so we can tell
    // whether the lock was actually cleared to false, or was still true but not restored.
    recordLog("启动(读取状态=$added)", "app_start")
  }

  fun setAdded(added: Boolean, skip: Listener? = null, source: String = "unknown") {
    check(this.added != added)
    this.added = added
    // Synchronous commit so the state survives process death (apply may be lost).
    sharedPreferences.edit().putBoolean(addedKey, added).commit()
    recordLog(if (added) "开启" else "关闭", source)
    for (listener in listeners) {
      if (listener != skip) {
        if (added) {
          listener.onFloatingViewAdded()
        } else {
          listener.onFloatingViewRemoved()
        }
      }
    }
  }

  var locked = false
    private set

  fun setLocked(locked: Boolean) {
    check(!locked || added)
    check(this.locked != locked)
    this.locked = locked
    for (listener in listeners) {
      if (locked) {
        listener.onFloatingViewLocked()
      } else {
        listener.onFloatingViewUnlocked()
      }
    }
  }

  var permissionGranted = permissionGranted
    private set

  fun setPermissionGranted(permissionGranted: Boolean) {
    // Allow redundant true setting because onServiceConnected gets called again on device startup.
    check(this.permissionGranted || permissionGranted)
    this.permissionGranted = permissionGranted
    for (listener in listeners) {
      if (permissionGranted) {
        listener.onFloatingViewPermissionGranted()
      } else {
        listener.onFloatingViewPermissionRevoked()
      }
    }
  }

  fun toggle() {
    for (listener in listeners) {
      listener.onToggle()
    }
  }

  // Diagnostic log: every enable/disable of the floating lock is recorded with a source
  // tag and a timestamp, so we can tell what cleared the lock when it disappears.
  private fun recordLog(event: String, source: String) {
    val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val existing = sharedPreferences.getString(logKey, "").orEmpty()
    val lines = existing.split("\n").filter { it.isNotBlank() }
    val newLog = (listOf("$time [$event] 来源:$source") + lines).take(maxLogEntries)
    sharedPreferences.edit().putString(logKey, newLog.joinToString("\n")).commit()
  }

  fun getLog(): String = sharedPreferences.getString(logKey, "").orEmpty()

  // Diagnostic helper for the service to record connection / restore results.
  fun recordDiagnostic(event: String, source: String) {
    recordLog(event, source)
  }

  fun addListener(listener: Listener) {
    listeners += listener
  }

  fun removeListener(listener: Listener) {
    listeners -= listener
  }

  private val listeners = mutableSetOf<Listener>()
}
