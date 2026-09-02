package com.nightlynexus.touchblocker

import android.content.SharedPreferences
import androidx.core.content.edit

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

  // Persisted so that when the OS restarts the accessibility service (e.g. after the
  // process was reclaimed in the background), the floating lock is restored instead of
  // silently disappearing while the accessibility switch remains enabled.
  var added = sharedPreferences.getBoolean(addedKey, false)
    private set

  fun setAdded(added: Boolean, skip: Listener? = null) {
    check(this.added != added)
    this.added = added
    sharedPreferences.edit { putBoolean(addedKey, added) }
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

  fun addListener(listener: Listener) {
    listeners += listener
  }

  fun removeListener(listener: Listener) {
    listeners -= listener
  }

  private val listeners = mutableSetOf<Listener>()
}
