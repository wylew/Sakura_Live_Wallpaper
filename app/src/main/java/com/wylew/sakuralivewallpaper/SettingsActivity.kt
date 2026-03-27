package com.wylew.sakuralivewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("sakura_prefs", MODE_PRIVATE) }
    private lateinit var previewView: WallpaperPreviewView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("background_uri", it.toString()).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        previewView = findViewById(R.id.wallpaper_preview)

        setupSliders()
        setupButtons()
        setupCheckbox()
        updateSettleSettingsState(prefs.getBoolean("collect_at_bottom", WallpaperConfig.COLLECT_AT_BOTTOM_DEFAULT))
    }

    private fun setupSliders() {
        findViewById<Slider>(R.id.slider_petal_count)?.apply {
            valueFrom = WallpaperConfig.PETAL_COUNT_MIN.toFloat()
            valueTo = WallpaperConfig.PETAL_COUNT_MAX.toFloat()
            updateValue(this, prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT).toFloat())
            addOnChangeListener { _, value, _ ->
                prefs.edit().putInt("petal_count", value.toInt()).apply()
            }
        }

        findViewById<Slider>(R.id.slider_wind_strength)?.apply {
            valueFrom = WallpaperConfig.WIND_STRENGTH_MIN
            valueTo = WallpaperConfig.WIND_STRENGTH_MAX
            val step = 0.05f
            val savedValue = prefs.getFloat("wind_strength", WallpaperConfig.WIND_STRENGTH_DEFAULT)
            updateValue(this, (savedValue / step).roundToInt() * step)

            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("wind_strength", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_turbulence_speed)?.apply {
            valueFrom = WallpaperConfig.TURBULENCE_SPEED_MIN
            valueTo = WallpaperConfig.TURBULENCE_SPEED_MAX
            updateValue(this, prefs.getFloat("turbulence_speed", WallpaperConfig.TURBULENCE_SPEED_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("turbulence_speed", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_turbulence_radius)?.apply {
            valueFrom = WallpaperConfig.TURBULENCE_RADIUS_MIN
            valueTo = WallpaperConfig.TURBULENCE_RADIUS_MAX
            updateValue(this, prefs.getFloat("turbulence_radius", WallpaperConfig.TURBULENCE_RADIUS_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("turbulence_radius", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_petal_size)?.apply {
            valueFrom = WallpaperConfig.PETAL_SIZE_MIN
            valueTo = WallpaperConfig.PETAL_SIZE_MAX
            updateValue(this, prefs.getFloat("petal_size", WallpaperConfig.PETAL_SIZE_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("petal_size", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_fall_speed)?.apply {
            valueFrom = WallpaperConfig.FALL_SPEED_MIN
            valueTo = WallpaperConfig.FALL_SPEED_MAX
            updateValue(this, prefs.getFloat("fall_speed", WallpaperConfig.FALL_SPEED_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("fall_speed", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_rotation_speed)?.apply {
            valueFrom = WallpaperConfig.ROTATION_SPEED_MIN
            valueTo = WallpaperConfig.ROTATION_SPEED_MAX
            updateValue(this, prefs.getFloat("rotation_speed", WallpaperConfig.ROTATION_SPEED_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("rotation_speed", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_petal_color)?.apply {
            valueFrom = 0f
            valueTo = 100f
            val color = prefs.getInt("petal_color", WallpaperConfig.COLOR_DEFAULT)
            updateValue(this, calculateColorProgress(color).toFloat())
            addOnChangeListener { _, value, _ ->
                prefs.edit().putInt("petal_color", interpolateColor(value / 100f)).apply()
            }
        }

        findViewById<Slider>(R.id.slider_transparency)?.apply {
            valueFrom = WallpaperConfig.ALPHA_MIN.toFloat()
            valueTo = WallpaperConfig.ALPHA_MAX.toFloat()
            updateValue(this, prefs.getInt("petal_alpha", WallpaperConfig.ALPHA_DEFAULT).toFloat())
            addOnChangeListener { _, value, _ ->
                prefs.edit().putInt("petal_alpha", value.toInt()).apply()
            }
        }

        findViewById<Slider>(R.id.slider_settle_probability)?.apply {
            valueFrom = WallpaperConfig.SETTLE_PROBABILITY_MIN
            valueTo = WallpaperConfig.SETTLE_PROBABILITY_MAX
            updateValue(this, prefs.getFloat("settle_probability", WallpaperConfig.SETTLE_PROBABILITY_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("settle_probability", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_settle_height)?.apply {
            valueFrom = WallpaperConfig.PILE_HEIGHT_MIN
            valueTo = WallpaperConfig.PILE_HEIGHT_MAX
            updateValue(this, prefs.getFloat("max_pile_height", WallpaperConfig.PILE_HEIGHT_DEFAULT))
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("max_pile_height", value).apply()
            }
        }
    }

    private fun updateValue(slider: Slider, value: Float) {
        slider.value = value.coerceIn(slider.valueFrom, slider.valueTo)
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btn_select_background)?.setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<View>(R.id.btn_set_wallpaper)?.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, SakuraWallpaperService::class.java))
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_reset_defaults)?.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun resetToDefaults() {
        prefs.edit().apply {
            putInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)
            putFloat("wind_strength", WallpaperConfig.WIND_STRENGTH_DEFAULT)
            putFloat("turbulence_speed", WallpaperConfig.TURBULENCE_SPEED_DEFAULT)
            putFloat("turbulence_radius", WallpaperConfig.TURBULENCE_RADIUS_DEFAULT)
            putFloat("petal_size", WallpaperConfig.PETAL_SIZE_DEFAULT)
            putFloat("fall_speed", WallpaperConfig.FALL_SPEED_DEFAULT)
            putFloat("rotation_speed", WallpaperConfig.ROTATION_SPEED_DEFAULT)
            putInt("petal_color", WallpaperConfig.COLOR_DEFAULT)
            putInt("petal_alpha", WallpaperConfig.ALPHA_DEFAULT)
            putBoolean("collect_at_bottom", WallpaperConfig.COLLECT_AT_BOTTOM_DEFAULT)
            putFloat("settle_probability", WallpaperConfig.SETTLE_PROBABILITY_DEFAULT)
            putFloat("max_pile_height", WallpaperConfig.PILE_HEIGHT_DEFAULT)
            apply()
        }
        
        // Refresh UI
        setupSliders()
        setupCheckbox()
        updateSettleSettingsState(WallpaperConfig.COLLECT_AT_BOTTOM_DEFAULT)
    }

    private fun setupCheckbox() {
        findViewById<MaterialCheckBox>(R.id.check_collect_at_bottom)?.apply {
            isChecked = prefs.getBoolean("collect_at_bottom", WallpaperConfig.COLLECT_AT_BOTTOM_DEFAULT)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("collect_at_bottom", isChecked).apply()
                updateSettleSettingsState(isChecked)
            }
        }
    }

    private fun updateSettleSettingsState(enabled: Boolean) {
        val alphaValue = if (enabled) 1.0f else 0.4f
        
        findViewById<View>(R.id.layout_settle_prob)?.apply {
            this.isEnabled = enabled
            this.alpha = alphaValue
        }
        findViewById<View>(R.id.layout_settle_height)?.apply {
            this.isEnabled = enabled
            this.alpha = alphaValue
        }
        findViewById<Slider>(R.id.slider_settle_probability)?.isEnabled = enabled
        findViewById<Slider>(R.id.slider_settle_height)?.isEnabled = enabled
    }

    private fun interpolateColor(fraction: Float): Int {
        val start = WallpaperConfig.COLOR_START
        val end = WallpaperConfig.COLOR_END
        val r = (Color.red(start) + fraction * (Color.red(end) - Color.red(start))).toInt()
        val g = (Color.green(start) + fraction * (Color.green(end) - Color.green(start))).toInt()
        val b = (Color.blue(start) + fraction * (Color.blue(end) - Color.blue(start))).toInt()
        return Color.rgb(r, g, b)
    }

    private fun calculateColorProgress(color: Int): Int {
        val startG = Color.green(WallpaperConfig.COLOR_START).toFloat()
        val endG = Color.green(WallpaperConfig.COLOR_END).toFloat()
        val currentG = Color.green(color).toFloat()
        if (startG == endG) return 0
        return (((currentG - startG) / (endG - startG)) * 100).toInt().coerceIn(0, 100)
    }

    override fun onResume() {
        super.onResume()
        previewView.start()
    }

    override fun onPause() {
        super.onPause()
        previewView.stop()
    }
}
