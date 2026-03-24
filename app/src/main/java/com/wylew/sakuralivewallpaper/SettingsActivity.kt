package com.wylew.sakuralivewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

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
        setupSwitch()
    }

    private fun setupSliders() {
        findViewById<Slider>(R.id.slider_petal_count).apply {
            valueFrom = WallpaperConfig.PETAL_COUNT_MIN.toFloat()
            valueTo = WallpaperConfig.PETAL_COUNT_MAX.toFloat()
            value = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT).toFloat().coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putInt("petal_count", value.toInt()).apply()
            }
        }

        findViewById<Slider>(R.id.slider_wind_strength).apply {
            valueFrom = WallpaperConfig.WIND_STRENGTH_MIN
            valueTo = WallpaperConfig.WIND_STRENGTH_MAX
            value = prefs.getFloat("wind_strength", WallpaperConfig.WIND_STRENGTH_DEFAULT).coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("wind_strength", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_petal_size).apply {
            valueFrom = WallpaperConfig.PETAL_SIZE_MIN
            valueTo = WallpaperConfig.PETAL_SIZE_MAX
            value = prefs.getFloat("petal_size", WallpaperConfig.PETAL_SIZE_DEFAULT).coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("petal_size", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_fall_speed).apply {
            valueFrom = WallpaperConfig.FALL_SPEED_MIN
            valueTo = WallpaperConfig.FALL_SPEED_MAX
            value = prefs.getFloat("fall_speed", WallpaperConfig.FALL_SPEED_DEFAULT).coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("fall_speed", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_rotation_speed).apply {
            valueFrom = WallpaperConfig.ROTATION_SPEED_MIN
            valueTo = WallpaperConfig.ROTATION_SPEED_MAX
            value = prefs.getFloat("rotation_speed", WallpaperConfig.ROTATION_SPEED_DEFAULT).coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("rotation_speed", value).apply()
            }
        }

        findViewById<Slider>(R.id.slider_petal_color).apply {
            valueFrom = 0f
            valueTo = 100f
            value = calculateColorProgress(prefs.getInt("petal_color", WallpaperConfig.COLOR_START)).toFloat()
            addOnChangeListener { _, value, _ ->
                prefs.edit().putInt("petal_color", interpolateColor(value / 100f)).apply()
            }
        }

        findViewById<Slider>(R.id.slider_transparency).apply {
            valueFrom = WallpaperConfig.ALPHA_MIN.toFloat()
            valueTo = WallpaperConfig.ALPHA_MAX.toFloat()
            value = prefs.getInt("petal_alpha", WallpaperConfig.ALPHA_DEFAULT).toFloat().coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putInt("petal_alpha", value.toInt()).apply()
            }
        }
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btn_select_background).setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<MaterialButton>(R.id.btn_set_wallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, SakuraWallpaperService::class.java))
            startActivity(intent)
        }
    }

    private fun setupSwitch() {
        findViewById<MaterialSwitch>(R.id.switch_collect_at_bottom).apply {
            isChecked = prefs.getBoolean("collect_at_bottom", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("collect_at_bottom", isChecked).apply()
            }
        }
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
