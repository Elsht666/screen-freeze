package com.nightlynexus.touchblocker

import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit

internal class FloatingLockBackgroundColorStatus(private val sharedPreferences: SharedPreferences) {
  companion object {
    const val colorPink = 0xFFF06292.toInt()
    const val colorRed = 0xFFE53935.toInt()
    const val colorGreen = 0xFF43A047.toInt()
    const val colorBlue = 0xFF1E88E5.toInt()
    const val colorPurple = 0xFF8E24AA.toInt()
    const val colorYellow = 0xFFFDD835.toInt()
    const val colorOrange = 0xFFFB8C00.toInt()
    const val colorCyan = 0xFF00ACC1.toInt()

    val presetColors = intArrayOf(
      colorPink, colorRed, colorGreen, colorBlue, colorPurple, colorYellow, colorOrange, colorCyan
    )
  }

  interface Listener {
    fun update(color: Int)
  }

  private val key = "floating_lock_background_color"
  private val listeners = mutableSetOf<Listener>()

  fun setColor(color: Int) {
    sharedPreferences.edit { putInt(key, color) }
    for (listener in listeners) {
      listener.update(color)
    }
  }

  fun getColor(): Int {
    return sharedPreferences.getInt(key, Color.WHITE)
  }

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }
}
