package com.wylew.sakuralivewallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.sqrt
import kotlin.random.Random

class SakuraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return SakuraEngine()
    }

    inner class SakuraEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
        private val TAG = "SakuraWallpaper"
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var lastTime = System.currentTimeMillis()
        private var lastLogTime = System.currentTimeMillis()
        private var spawnsInInterval = 0
        
        private val activePetals = mutableListOf<Petal>()
        private val groundedPetals = mutableListOf<Petal>()
        
        private var backgroundBitmap: Bitmap? = null
        private var groundedLayerBitmap: Bitmap? = null
        private var groundedLayerCanvas: Canvas? = null
        
        private val backgroundMatrix = Matrix()
        
        private val prefs: SharedPreferences by lazy {
            getSharedPreferences("sakura_prefs", MODE_PRIVATE)
        }

        private lateinit var sensorManager: SensorManager
        private var accelerometer: Sensor? = null
        
        private val drawRunner = object : Runnable {
            override fun run() {
                draw()
            }
        }

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            loadSettings()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastTime = System.currentTimeMillis()
                lastLogTime = System.currentTimeMillis()
                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
                draw()
            } else {
                sensorManager.unregisterListener(this)
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(drawRunner)
            
            groundedLayerBitmap?.recycle()
            groundedLayerBitmap = null
            groundedLayerCanvas = null
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            loadSettings()
        }

        private fun loadSettings() {
            val bgUriStr = prefs.getString("background_uri", null)
            bgUriStr?.let {
                try {
                    val uri = Uri.parse(it)
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val softwareBitmap = BitmapFactory.decodeStream(stream)
                        backgroundBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && softwareBitmap != null) {
                            val hwBitmap = softwareBitmap.copy(Bitmap.Config.HARDWARE, false)
                            softwareBitmap.recycle()
                            hwBitmap ?: softwareBitmap
                        } else {
                            softwareBitmap
                        }
                    }
                } catch (e: Exception) {
                    backgroundBitmap = null
                }
            } ?: run { backgroundBitmap = null }

            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            
            if (width > 0 && height > 0) {
                val targetCount = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)
                val wind = prefs.getFloat("wind_strength", 0.5f)
                val size = prefs.getFloat("petal_size", 20f)
                val speed = prefs.getFloat("fall_speed", 0.5f)
                val rotSpeed = prefs.getFloat("rotation_speed", 1.0f)
                val color = prefs.getInt("petal_color", Color.WHITE)
                val alpha = prefs.getInt("petal_alpha", 200)
                val collect = prefs.getBoolean("collect_at_bottom", false)

                val oldWidth = activePetals.firstOrNull()?.screenWidth ?: 0
                val oldHeight = activePetals.firstOrNull()?.screenHeight ?: 0
                val isRotation = (width != oldWidth || height != oldHeight) && oldWidth > 0

                if (!collect) {
                    groundedPetals.clear()
                    groundedLayerBitmap?.eraseColor(Color.TRANSPARENT)
                }

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

                activePetals.forEach { it.updateSettings(width, height, size, wind, speed, color, alpha, collect, rotSpeed) }
                groundedPetals.forEach { it.updateSettings(width, height, size, wind, speed, color, alpha, collect, rotSpeed) }
                
                if (collect && groundedPetals.isNotEmpty()) {
                    redrawGroundedLayer()
                }
            }
        }

        private fun redrawGroundedLayer() {
            groundedLayerBitmap?.eraseColor(Color.TRANSPARENT)
            groundedPetals.forEach { p ->
                groundedLayerCanvas?.let { p.draw(it) }
            }
        }

        private fun ensureGroundedLayer(width: Int, height: Int) {
            if (groundedLayerBitmap == null || groundedLayerBitmap!!.width != width || groundedLayerBitmap!!.height != height) {
                groundedLayerBitmap?.recycle()
                groundedLayerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                groundedLayerCanvas = Canvas(groundedLayerBitmap!!)
                redrawGroundedLayer()
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                // Optimization: Use Hardware Canvas if available (API 26+)
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
                
                if (canvas != null) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = ((currentTime - lastTime) / 1000f).coerceAtMost(0.033f)
                    lastTime = currentTime

                    ensureGroundedLayer(canvas.width, canvas.height)
                    drawBackground(canvas)
                    
                    groundedLayerBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

                    val collect = prefs.getBoolean("collect_at_bottom", false)
                    val targetCount = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)

                    val activeIterator = activePetals.iterator()
                    while (activeIterator.hasNext()) {
                        val p = activeIterator.next()
                        p.update(deltaTime)
                        p.draw(canvas)

                        val isOffBottom = p.y > canvas.height + p.size * 4
                        val isBlowingOffScreen = p.isBlowingAway && (p.y < -p.size * 10 || p.x < -p.size * 10 || p.x > canvas.width + p.size * 10)

                        if (isOffBottom || isBlowingOffScreen) {
                            if (isOffBottom && !p.isBlowingAway && collect && groundedPetals.size < WallpaperConfig.MAX_GROUNDED_PETALS) {
                                if (Random.nextFloat() < WallpaperConfig.SETTLE_PROBABILITY) {
                                    val maxPileY = canvas.height * (1f - WallpaperConfig.MAX_PILE_HEIGHT_PERCENT)
                                    p.y = canvas.height - (Random.nextFloat() * (canvas.height - maxPileY))
                                    p.isGrounded = true
                                    groundedPetals.add(p)
                                    groundedLayerCanvas?.let { p.draw(it) }
                                    activeIterator.remove()
                                    continue
                                }
                            }

                            if (activePetals.size > targetCount) {
                                activeIterator.remove()
                            } else {
                                p.reset()
                                spawnsInInterval++
                            }
                        }
                    }

                    while (activePetals.size < targetCount) {
                        val p = createNewPetal(canvas.width, canvas.height)
                        p.reset()
                        activePetals.add(p)
                        spawnsInInterval++
                    }

                    if (currentTime - lastLogTime > 2000) {
                        val elapsedSeconds = (currentTime - lastLogTime) / 1000f
                        val spawnRate = if (elapsedSeconds > 0) spawnsInInterval / elapsedSeconds else 0f
                        Log.v(TAG, "Stats -> Falling: ${activePetals.size}, Grounded: ${groundedPetals.size}, Total: ${activePetals.size + groundedPetals.size}, Spawning: ${"%.2f".format(spawnRate)}/s (Total: $spawnsInInterval)")
                        lastLogTime = currentTime
                        spawnsInInterval = 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Draw error", e)
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

        private fun createNewPetal(width: Int, height: Int): Petal {
            return Petal(
                width,
                height,
                0,
                prefs.getFloat("wind_strength", 0.5f),
                prefs.getFloat("petal_size", 20f),
                prefs.getFloat("fall_speed", 0.5f),
                prefs.getInt("petal_color", Color.WHITE),
                prefs.getInt("petal_alpha", 200),
                prefs.getBoolean("collect_at_bottom", false),
                prefs.getFloat("rotation_speed", 1.0f)
            )
        }

        private fun drawBackground(canvas: Canvas) {
            backgroundBitmap?.let { bitmap ->
                val canvasWidth = canvas.width.toFloat()
                val canvasHeight = canvas.height.toFloat()
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()
                val scale = if (bitmapWidth * canvasHeight > canvasWidth * bitmapHeight) canvasHeight / bitmapHeight else canvasWidth / bitmapWidth
                val dx = (canvasWidth - bitmapWidth * scale) * 0.5f
                val dy = (canvasHeight - bitmapHeight * scale) * 0.5f
                
                backgroundMatrix.reset()
                backgroundMatrix.setScale(scale, scale)
                backgroundMatrix.postTranslate(dx, dy)
                canvas.drawBitmap(bitmap, backgroundMatrix, null)
            } ?: canvas.drawColor(Color.BLACK)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt(x*x + y*y + z*z) / SensorManager.GRAVITY_EARTH
                
                if (gForce > (WallpaperConfig.SHAKE_THRESHOLD / 9.81f) + 1.0f) {
                    if (groundedPetals.isNotEmpty()) {
                        groundedPetals.forEach { 
                            it.blowAway()
                            activePetals.add(it)
                        }
                        groundedPetals.clear()
                        groundedLayerBitmap?.eraseColor(Color.TRANSPARENT)
                        Log.d(TAG, "Shake detected! Grounded cleared. Active count: ${activePetals.size}")
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            loadSettings()
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(drawRunner)
            
            groundedLayerBitmap?.recycle()
            groundedLayerBitmap = null
            groundedLayerCanvas = null
        }
    }
}
