package com.wylew.sakuralivewallpaper

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class SakuraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return SakuraEngine()
    }

    inner class SakuraEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var lastTime = System.currentTimeMillis()
        
        private val petals = mutableListOf<Petal>()
        private var backgroundBitmap: Bitmap? = null
        private val prefs: SharedPreferences by lazy {
            getSharedPreferences("sakura_prefs", MODE_PRIVATE)
        }

        private val drawRunner = object : Runnable {
            override fun run() {
                draw()
            }
        }

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
            loadSettings()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastTime = System.currentTimeMillis()
                draw()
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunner)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
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
                    contentResolver.openInputStream(uri)?.use { stream ->
                        backgroundBitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    backgroundBitmap = null
                }
            } ?: run { backgroundBitmap = null }

            if (petals.size != count) {
                petals.clear()
                val width = surfaceHolder.surfaceFrame.width()
                val height = surfaceHolder.surfaceFrame.height()
                if (width > 0 && height > 0) {
                    repeat(count) {
                        petals.add(Petal(width, height, count, wind, size, speed, color, alpha, collect, rotSpeed))
                    }
                }
            } else {
                petals.forEach { it.updateSettings(size, wind, speed, color, alpha, collect, rotSpeed) }
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastTime) / 1000f
                    lastTime = currentTime

                    drawBackground(canvas)

                    petals.forEach {
                        it.update(deltaTime)
                        it.draw(canvas)
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, 16)
            }
        }

        private fun drawBackground(canvas: Canvas) {
            backgroundBitmap?.let { bitmap ->
                val canvasWidth = canvas.width.toFloat()
                val canvasHeight = canvas.height.toFloat()
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

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            loadSettings()
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            handler.removeCallbacks(drawRunner)
        }
    }
}
