package com.wylew.sakuralivewallpaper

import android.graphics.Color

object WallpaperConfig {
    // Petal Count
    const val PETAL_COUNT_MIN = 50
    const val PETAL_COUNT_MAX = 500
    const val PETAL_COUNT_DEFAULT = 246

    // Wind Strength (Directional: -1.0 Left to 1.0 Right)
    const val WIND_STRENGTH_MIN = -1.0f
    const val WIND_STRENGTH_MAX = 1.0f
    const val WIND_STRENGTH_DEFAULT = 0.10f

    // Turbulence Speed (Linear path traversal speed)
    const val TURBULENCE_SPEED_MIN = 0.0f
    const val TURBULENCE_SPEED_MAX = 1.0f
    const val TURBULENCE_SPEED_DEFAULT = 0.08f

    // Turbulence Radius (Swirl size)
    const val TURBULENCE_RADIUS_MIN = 0.0f
    const val TURBULENCE_RADIUS_MAX = 1.0f
    const val TURBULENCE_RADIUS_DEFAULT = 0.82f

    // Wind Turbulence (Legacy - keeping for transition)
    const val WIND_TURBULENCE_DEFAULT = 0.3f

    // Petal Size
    const val PETAL_SIZE_MIN = 5.0f
    const val PETAL_SIZE_MAX = 30.0f
    const val PETAL_SIZE_DEFAULT = 12.0f

    // Fall Speed
    const val FALL_SPEED_MIN = 0.0f
    const val FALL_SPEED_MAX = 0.5f
    const val FALL_SPEED_DEFAULT = 0.11f

    // Rotation Speed
    const val ROTATION_SPEED_MIN = 0.0f
    const val ROTATION_SPEED_MAX = 2.0f
    const val ROTATION_SPEED_DEFAULT = 0.87f

    // Transparency
    const val ALPHA_MIN = 0
    const val ALPHA_MAX = 255
    const val ALPHA_DEFAULT = 193

    // Colors
    val COLOR_START = Color.WHITE
    val COLOR_END = Color.rgb(255, 182, 193) // Light Pink
    
    // Default color at 30% between Start and End
    val COLOR_DEFAULT = Color.rgb(255, 233, 236)

    // Settle Settings
    const val COLLECT_AT_BOTTOM_DEFAULT = true

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
