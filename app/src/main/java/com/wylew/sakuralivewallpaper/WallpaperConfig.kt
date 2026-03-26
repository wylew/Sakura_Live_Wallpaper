package com.wylew.sakuralivewallpaper

import android.graphics.Color

object WallpaperConfig {
    // Petal Count
    const val PETAL_COUNT_MIN = 50
    const val PETAL_COUNT_MAX = 500
    const val PETAL_COUNT_DEFAULT = 150

    // Wind Strength
    const val WIND_STRENGTH_MIN = 0.0f
    const val WIND_STRENGTH_MAX = 1.0f
    const val WIND_STRENGTH_DEFAULT = 0.5f

    // Petal Size
    const val PETAL_SIZE_MIN = 5.0f
    const val PETAL_SIZE_MAX = 30.0f
    const val PETAL_SIZE_DEFAULT = 15.0f

    // Fall Speed
    const val FALL_SPEED_MIN = 0.0f
    const val FALL_SPEED_MAX = 0.5f
    const val FALL_SPEED_DEFAULT = 0.15f

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

    // Settle Settings
    const val SETTLE_PROBABILITY_MIN = 0.0f
    const val SETTLE_PROBABILITY_MAX = 1.0f
    const val SETTLE_PROBABILITY_DEFAULT = 0.15f

    const val PILE_HEIGHT_MIN = 0.0f
    const val PILE_HEIGHT_MAX = 1.0f
    const val PILE_HEIGHT_DEFAULT = 0.10f

    // Internal Logic (Not user facing)
    const val SHAKE_THRESHOLD = 12.0f // Sensitivity for shake detection
    const val MAX_GROUNDED_PETALS = 750 // Maximum number of petals that can settle at the bottom
}
