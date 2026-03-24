package com.wylew.sakuralivewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

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

        setupSeekBars()
        setupButtons()
        setupCheckbox()
    }

    private fun setupSeekBars() {
        findViewById<SeekBar>(R.id.sb_petal_count).apply {
            max = WallpaperConfig.PETAL_COUNT_MAX
            progress = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    prefs.edit().putInt("petal_count", progress.coerceAtLeast(WallpaperConfig.PETAL_COUNT_MIN)).apply()
                }
            })
        }

        findViewById<SeekBar>(R.id.sb_wind_strength).apply {
            max = 100
            val current = prefs.getFloat("wind_strength", WallpaperConfig.WIND_STRENGTH_DEFAULT)
            progress = ((current - WallpaperConfig.WIND_STRENGTH_MIN) / (WallpaperConfig.WIND_STRENGTH_MAX - WallpaperConfig.WIND_STRENGTH_MIN) * 100).toInt()
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = WallpaperConfig.WIND_STRENGTH_MIN + (progress / 100f) * (WallpaperConfig.WIND_STRENGTH_MAX - WallpaperConfig.WIND_STRENGTH_MIN)
                    prefs.edit().putFloat("wind_strength", value).apply()
                }
            })
        }

        findViewById<SeekBar>(R.id.sb_petal_size).apply {
            max = 100
            val current = prefs.getFloat("petal_size", WallpaperConfig.PETAL_SIZE_DEFAULT)
            progress = ((current - WallpaperConfig.PETAL_SIZE_MIN) / (WallpaperConfig.PETAL_SIZE_MAX - WallpaperConfig.PETAL_SIZE_MIN) * 100).toInt()
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = WallpaperConfig.PETAL_SIZE_MIN + (progress / 100f) * (WallpaperConfig.PETAL_SIZE_MAX - WallpaperConfig.PETAL_SIZE_MIN)
                    prefs.edit().putFloat("petal_size", value).apply()
                }
            })
        }

        findViewById<SeekBar>(R.id.sb_fall_speed).apply {
            max = 100
            val current = prefs.getFloat("fall_speed", WallpaperConfig.FALL_SPEED_DEFAULT)
            progress = ((current - WallpaperConfig.FALL_SPEED_MIN) / (WallpaperConfig.FALL_SPEED_MAX - WallpaperConfig.FALL_SPEED_MIN) * 100).toInt()
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = WallpaperConfig.FALL_SPEED_MIN + (progress / 100f) * (WallpaperConfig.FALL_SPEED_MAX - WallpaperConfig.FALL_SPEED_MIN)
                    prefs.edit().putFloat("fall_speed", value).apply()
                }
            })
        }

        findViewById<SeekBar>(R.id.sb_rotation_speed).apply {
            max = 100
            val current = prefs.getFloat("rotation_speed", WallpaperConfig.ROTATION_SPEED_DEFAULT)
            progress = ((current - WallpaperConfig.ROTATION_SPEED_MIN) / (WallpaperConfig.ROTATION_SPEED_MAX - WallpaperConfig.ROTATION_SPEED_MIN) * 100).toInt()
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = WallpaperConfig.ROTATION_SPEED_MIN + (progress / 100f) * (WallpaperConfig.ROTATION_SPEED_MAX - WallpaperConfig.ROTATION_SPEED_MIN)
                    prefs.edit().putFloat("rotation_speed", value).apply()
                }
            })
        }

        findViewById<SeekBar>(R.id.sb_petal_color).apply {
            max = 100
            progress = calculateColorProgress(prefs.getInt("petal_color", WallpaperConfig.COLOR_START))
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    prefs.edit().putInt("petal_color", interpolateColor(progress / 100f)).apply()
                }
            })
        }

        findViewById<SeekBar>(R.id.sb_transparency).apply {
            max = WallpaperConfig.ALPHA_MAX
            progress = prefs.getInt("petal_alpha", WallpaperConfig.ALPHA_DEFAULT)
            setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    prefs.edit().putInt("petal_alpha", progress.coerceAtLeast(WallpaperConfig.ALPHA_MIN)).apply()
                }
            })
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_select_background).setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<Button>(R.id.btn_set_wallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, SakuraWallpaperService::class.java))
            startActivity(intent)
        }
    }

    private fun setupCheckbox() {
        findViewById<CheckBox>(R.id.cb_collect_at_bottom).apply {
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

    open class SimpleSeekBarChangeListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }
}
