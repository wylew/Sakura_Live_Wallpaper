package com.wylew.sakuralivewallpaper

import android.graphics.Color

object WallpaperConfig {
    // Petal Count
    const val PETAL_COUNT_MIN = 1
    const val PETAL_COUNT_MAX = 200
    const val PETAL_COUNT_DEFAULT = 50

    // Wind Strength
    const val WIND_STRENGTH_MIN = 0.0f
    const val WIND_STRENGTH_MAX = 1.0f
    const val WIND_STRENGTH_DEFAULT = 0.5f

    // Petal Size
    const val PETAL_SIZE_MIN = 5.0f
    const val PETAL_SIZE_MAX = 100.0f
    const val PETAL_SIZE_DEFAULT = 20.0f

    // Fall Speed
    const val FALL_SPEED_MIN = 0.0f
    const val FALL_SPEED_MAX = 1.0f
    const val FALL_SPEED_DEFAULT = 0.5f

    // Rotation Speed
    const val ROTATION_SPEED_MIN = 0.0f
    const val ROTATION_SPEED_MAX = 2.0f
    const val ROTATION_SPEED_DEFAULT = 1.0f

    // Transparency
    const val ALPHA_MIN = 0
    const val ALPHA_MAX = 255
    const val ALPHA_DEFAULT = 200

    // Colors
    val COLOR_START = Color.WHITE
    val COLOR_END = Color.rgb(255, 182, 193) // Light Pink
}
