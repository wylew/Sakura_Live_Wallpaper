package com.wylew.sakuralivewallpaper

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import kotlin.math.sqrt
import kotlin.random.Random

class SakuraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return SakuraEngine()
    }

    inner class SakuraEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
        private val tag = "SakuraWallpaper"
        
        private var renderThread: RenderThread? = null
        private val activePetals = mutableListOf<Petal>()
        private val groundedPetals = mutableListOf<Petal>()
        
        private var backgroundBitmap: Bitmap? = null
        private var scaledBackground: Bitmap? = null
        private var groundedLayerBitmap: Bitmap? = null
        private var groundedLayerCanvas: Canvas? = null
        
        private val prefs: SharedPreferences by lazy {
            getSharedPreferences("sakura_prefs", MODE_PRIVATE)
        }

        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null
        private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
            val sm = getSystemService(SENSOR_SERVICE) as SensorManager
            sensorManager = sm
            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            loadSettings()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                startRenderThread()
                accelerometer?.let {
                    sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } else {
                stopRenderThread()
                sensorManager?.unregisterListener(this)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            loadSettings()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopRenderThread()
            
            scaledBackground?.recycle()
            scaledBackground = null
            groundedLayerBitmap?.recycle()
            groundedLayerBitmap = null
            groundedLayerCanvas = null
        }

        private fun startRenderThread() {
            if (renderThread == null) {
                renderThread = RenderThread().apply {
                    start()
                }
            }
        }

        private fun stopRenderThread() {
            renderThread?.running = false
            try {
                renderThread?.join(500)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            renderThread = null
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            loadSettings()
        }

        private fun loadSettings() {
            val bgUriStr = prefs.getString("background_uri", null)
            bgUriStr?.let {
                try {
                    val uri = it.toUri()
                    contentResolver.openInputStream(uri)?.use { stream ->
                        backgroundBitmap = BitmapFactory.decodeStream(stream)
                        updateScaledBackground()
                    }
                } catch (_: Exception) {
                    backgroundBitmap = null
                    scaledBackground = null
                }
            } ?: run { 
                backgroundBitmap = null
                scaledBackground = null
            }

            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            
            if (width > 0 && height > 0) {
                val targetCount = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)
                val wind = prefs.getFloat("wind_strength", WallpaperConfig.WIND_STRENGTH_DEFAULT)
                val size = prefs.getFloat("petal_size", WallpaperConfig.PETAL_SIZE_DEFAULT)
                val speed = prefs.getFloat("fall_speed", WallpaperConfig.FALL_SPEED_DEFAULT)
                val rotSpeed = prefs.getFloat("rotation_speed", WallpaperConfig.ROTATION_SPEED_DEFAULT)
                val color = prefs.getInt("petal_color", WallpaperConfig.COLOR_START)
                val alpha = prefs.getInt("petal_alpha", WallpaperConfig.ALPHA_DEFAULT)
                val collect = prefs.getBoolean("collect_at_bottom", false)
                
                // New turbulence settings
                val turbSpeed = prefs.getFloat("turbulence_speed", WallpaperConfig.TURBULENCE_SPEED_DEFAULT)
                val turbRadius = prefs.getFloat("turbulence_radius", WallpaperConfig.TURBULENCE_RADIUS_DEFAULT)

                val oldWidth = activePetals.firstOrNull()?.screenWidth ?: 0
                val oldHeight = activePetals.firstOrNull()?.screenHeight ?: 0
                val isRotation = (width != oldWidth || height != oldHeight) && oldWidth > 0

                if (!collect) {
                    groundedPetals.clear()
                    groundedLayerBitmap?.eraseColor(Color.TRANSPARENT)
                }

                synchronized(activePetals) {
                    if (isRotation) {
                        activePetals.clear()
                        val prepopulateCount = (targetCount * 0.8f).toInt()
                        repeat(prepopulateCount) {
                            val p = createNewPetal(width, height)
                            p.reset(fullRandomY = true)
                            activePetals.add(p)
                        }
                    } else {
                        if (activePetals.size < targetCount) {
                            repeat(targetCount - activePetals.size) {
                                val p = createNewPetal(width, height)
                                p.reset(fullRandomY = true)
                                activePetals.add(p)
                            }
                        }
                    }

                    activePetals.forEach { it.updateSettings(width, height, size, wind, speed, color, alpha, collect, rotSpeed, turbSpeed, turbRadius) }
                    groundedPetals.forEach { it.updateSettings(width, height, size, wind, speed, color, alpha, collect, rotSpeed, turbSpeed, turbRadius) }
                }
                
                if (collect) {
                    redrawGroundedLayer()
                }
            }
        }

        private fun updateScaledBackground() {
            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            val original = backgroundBitmap
            if (width <= 0 || height <= 0 || original == null) {
                scaledBackground = null
                return
            }

            val bw = original.width.toFloat()
            val bh = original.height.toFloat()
            val scale = if (bw * height > width * bh) height / bh else width / bw
            
            val sw = (bw * scale).toInt()
            val sh = (bh * scale).toInt()
            
            val softwareScaled = original.scale(sw, sh, true)
            
            // Center crop
            val x = (sw - width) / 2
            val y = (sh - height) / 2
            val cropped = Bitmap.createBitmap(softwareScaled, x, y, width, height)
            
            scaledBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hw = cropped.copy(Bitmap.Config.HARDWARE, false)
                cropped.recycle()
                softwareScaled.recycle()
                hw ?: cropped
            } else {
                softwareScaled.recycle()
                cropped
            }
        }

        private fun redrawGroundedLayer() {
            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            if (width <= 0 || height <= 0) return

            ensureGroundedLayer(width, height)
            groundedLayerBitmap?.eraseColor(Color.TRANSPARENT)
            synchronized(activePetals) {
                groundedPetals.forEach { p ->
                    groundedLayerCanvas?.let { p.draw(it) }
                }
            }
        }

        private fun ensureGroundedLayer(width: Int, height: Int) {
            if (groundedLayerBitmap == null || groundedLayerBitmap!!.width != width || groundedLayerBitmap!!.height != height) {
                groundedLayerBitmap?.recycle()
                groundedLayerBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                groundedLayerCanvas = Canvas(groundedLayerBitmap!!)
            }
        }

        private fun createNewPetal(width: Int, height: Int): Petal {
            return Petal(
                screenWidth = width,
                screenHeight = height,
                size = prefs.getFloat("petal_size", WallpaperConfig.PETAL_SIZE_DEFAULT),
                windStrength = prefs.getFloat("wind_strength", WallpaperConfig.WIND_STRENGTH_DEFAULT),
                speed = prefs.getFloat("fall_speed", WallpaperConfig.FALL_SPEED_DEFAULT),
                color = prefs.getInt("petal_color", WallpaperConfig.COLOR_START),
                alpha = prefs.getInt("petal_alpha", WallpaperConfig.ALPHA_DEFAULT),
                collectAtBottom = prefs.getBoolean("collect_at_bottom", false),
                rotationSpeed = prefs.getFloat("rotation_speed", WallpaperConfig.ROTATION_SPEED_DEFAULT),
                turbulenceSpeed = prefs.getFloat("turbulence_speed", WallpaperConfig.TURBULENCE_SPEED_DEFAULT),
                turbulenceRadius = prefs.getFloat("turbulence_radius", WallpaperConfig.TURBULENCE_RADIUS_DEFAULT)
            )
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt(x*x + y*y + z*z) / SensorManager.GRAVITY_EARTH
                
                if (gForce > (WallpaperConfig.SHAKE_THRESHOLD / 9.81f) + 1.0f) {
                    synchronized(activePetals) {
                        if (groundedPetals.isNotEmpty()) {
                            groundedPetals.forEach { 
                                it.blowAway()
                                activePetals.add(it)
                            }
                            groundedPetals.clear()
                            groundedLayerBitmap?.eraseColor(Color.TRANSPARENT)
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            sensorManager?.unregisterListener(this)
            stopRenderThread()
        }

        private inner class RenderThread : Thread("SakuraRenderThread") {
            var running = true
            private var lastFrameNanoTime = 0L

            override fun run() {
                lastFrameNanoTime = System.nanoTime()
                
                while (running) {
                    val startTime = System.nanoTime()
                    
                    // Battery Saver Check
                    val isPowerSave = powerManager.isPowerSaveMode
                    val targetFps = if (isPowerSave) 30 else 60
                    val frameTimeNs = 1_000_000_000L / targetFps

                    doDraw()

                    val endTime = System.nanoTime()
                    val sleepTimeMs = ((frameTimeNs - (endTime - startTime)) / 1_000_000L)
                    if (sleepTimeMs > 0) {
                        try {
                            sleep(sleepTimeMs)
                        } catch (_: InterruptedException) {
                            // Exit loop
                            break
                        }
                    }
                }
            }

            private fun doDraw() {
                val holder = surfaceHolder
                var canvas: Canvas? = null
                try {
                    canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        holder.lockHardwareCanvas()
                    } else {
                        holder.lockCanvas()
                    }

                    if (canvas != null) {
                        val currentNanoTime = System.nanoTime()
                        val deltaTime = ((currentNanoTime - lastFrameNanoTime) / 1_000_000_000f).coerceAtMost(0.033f)
                        lastFrameNanoTime = currentNanoTime

                        // Draw background (pre-scaled)
                        scaledBackground?.let {
                            canvas.drawBitmap(it, 0f, 0f, null)
                        } ?: canvas.drawColor(Color.BLACK)
                        
                        // Draw grounded layer
                        groundedLayerBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

                        val collect = prefs.getBoolean("collect_at_bottom", false)
                        val settleProb = prefs.getFloat("settle_probability", WallpaperConfig.SETTLE_PROBABILITY_DEFAULT)
                        val pileHeightPercent = prefs.getFloat("max_pile_height", WallpaperConfig.PILE_HEIGHT_DEFAULT)
                        
                        val targetCount = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)
                        val maxCount = if (powerManager.isPowerSaveMode) (targetCount * 0.5f).toInt() else targetCount

                        synchronized(activePetals) {
                            val activeIterator = activePetals.iterator()
                            while (activeIterator.hasNext()) {
                                val p = activeIterator.next()
                                p.update(deltaTime)
                                p.draw(canvas)

                                val isOffBottom = p.y > canvas.height - p.size
                                val isBlowingOffScreen = p.isBlowingAway && (p.y < -p.size * 10 || p.x < -p.size * 10 || p.x > canvas.width + p.size * 10)

                                if (isOffBottom || isBlowingOffScreen) {
                                    if (isOffBottom && !p.isBlowingAway && collect && groundedPetals.size < WallpaperConfig.MAX_GROUNDED_PETALS) {
                                        if (Random.nextFloat() < settleProb) {
                                            val maxPileY = canvas.height * (1f - pileHeightPercent)
                                            p.y = canvas.height - (Random.nextFloat() * (canvas.height - maxPileY))
                                            p.settle()
                                            groundedPetals.add(p)
                                            groundedLayerCanvas?.let { p.draw(it) }
                                            activeIterator.remove()
                                            continue
                                        }
                                    }

                                    if (activePetals.size > maxCount) {
                                        activeIterator.remove()
                                    } else {
                                        p.reset()
                                    }
                                }
                            }

                            while (activePetals.size < maxCount) {
                                val p = createNewPetal(canvas.width, canvas.height)
                                p.reset()
                                activePetals.add(p)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Draw error", e)
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas)
                        } catch (e: IllegalArgumentException) {
                            Log.e(tag, "Surface already released", e)
                        }
                    }
                }
            }
        }
    }
}
