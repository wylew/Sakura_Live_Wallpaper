package com.wylew.sakuralivewallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.view.View

class WallpaperPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SharedPreferences.OnSharedPreferenceChangeListener {

    private val petals = mutableListOf<Petal>()
    private var backgroundBitmap: Bitmap? = null
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("sakura_prefs", Context.MODE_PRIVATE)
    }
    
    private var lastTime = System.currentTimeMillis()
    private var running = false

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
        loadSettings()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        loadSettings()
    }

    private fun loadSettings() {
        val count = prefs.getInt("petal_count", 50)
        val wind = prefs.getFloat("wind_strength", 0.5f)
        val size = prefs.getFloat("petal_size", 20f)
        val speed = prefs.getFloat("fall_speed", 0.5f)
        val rotSpeed = prefs.getFloat("rotation_speed", 1.0f)
        val color = prefs.getInt("petal_color", Color.WHITE)
        val alpha = prefs.getInt("petal_alpha", 200)
        val collect = prefs.getBoolean("collect_at_bottom", false)
        val bgUriStr = prefs.getString("background_uri", null)

        bgUriStr?.let {
            try {
                val uri = Uri.parse(it)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    backgroundBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                backgroundBitmap = null
            }
        } ?: run { backgroundBitmap = null }

        if (width > 0 && height > 0) {
            if (petals.size != count) {
                petals.clear()
                repeat(count) {
                    petals.add(Petal(width, height, count, wind, size, speed, color, alpha, collect, rotSpeed))
                }
            } else {
                petals.forEach { 
                    it.updateSettings(width, height, size, wind, speed, color, alpha, collect, rotSpeed)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = ((currentTime - lastTime) / 1000f).coerceAtMost(0.1f)
        lastTime = currentTime

        drawBackground(canvas)

        petals.forEach {
            it.update(deltaTime)
            if (it.y > height + it.size * 4) {
                it.reset()
            }
            it.draw(canvas)
        }

        if (running) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundBitmap?.let { bitmap ->
            val canvasWidth = width.toFloat()
            val canvasHeight = height.toFloat()
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            val scale: Float
            var dx = 0f
            var dy = 0f

            if (bitmapWidth * canvasHeight > canvasWidth * bitmapHeight) {
                scale = canvasHeight / bitmapHeight
                dx = (canvasWidth - bitmapWidth * scale) * 0.5f
            } else {
                scale = canvasWidth / bitmapWidth
                dy = (canvasHeight - bitmapHeight * scale) * 0.5f
            }

            val matrix = Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)
            canvas.drawBitmap(bitmap, matrix, null)
        } ?: canvas.drawColor(Color.BLACK)
    }

    fun start() {
        running = true
        lastTime = System.currentTimeMillis()
        invalidate()
    }

    fun stop() {
        running = false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadSettings()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
