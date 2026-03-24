package com.wylew.sakuralivewallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
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
        
        private val allPetals = mutableListOf<Petal>()
        private var backgroundBitmap: Bitmap? = null
        private val prefs: SharedPreferences by lazy {
            getSharedPreferences("sakura_prefs", MODE_PRIVATE)
        }

        private lateinit var sensorManager: SensorManager
        private var accelerometer: Sensor? = null
        
        private var spawnAccumulator = 0f

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
                        backgroundBitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    backgroundBitmap = null
                }
            } ?: run { backgroundBitmap = null }

            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            
            if (width > 0 && height > 0) {
                val wind = prefs.getFloat("wind_strength", 0.5f)
                val size = prefs.getFloat("petal_size", 20f)
                val speed = prefs.getFloat("fall_speed", 0.5f)
                val rotSpeed = prefs.getFloat("rotation_speed", 1.0f)
                val color = prefs.getInt("petal_color", Color.WHITE)
                val alpha = prefs.getInt("petal_alpha", 200)
                val collect = prefs.getBoolean("collect_at_bottom", false)

                allPetals.forEach { it.updateSettings(size, wind, speed, color, alpha, collect, rotSpeed) }
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val currentTime = System.currentTimeMillis()
                    // Cap deltaTime to avoid huge jumps on wake/lag
                    val deltaTime = ((currentTime - lastTime) / 1000f).coerceAtMost(0.05f)
                    lastTime = currentTime

                    drawBackground(canvas)

                    val collect = prefs.getBoolean("collect_at_bottom", false)
                    val targetCount = prefs.getInt("petal_count", WallpaperConfig.PETAL_COUNT_DEFAULT)
                    
                    val currentFallingCount = allPetals.count { !it.isGrounded && !it.isBlowingAway }
                    val groundedCount = allPetals.count { it.isGrounded }
                    
                    // Dynamic spawn rate: faster if we are empty, but always capped
                    val baseSpawnRate = (targetCount / 10f).coerceIn(2f, 15f) 
                    
                    if (currentFallingCount < targetCount) {
                        spawnAccumulator += deltaTime * baseSpawnRate
                        while (spawnAccumulator >= 1f && allPetals.count { !it.isGrounded && !it.isBlowingAway } < targetCount) {
                            val p = createNewPetal(canvas.width, canvas.height, targetCount, collect)
                            p.y = -p.size * 5 // Spawn well above screen
                            allPetals.add(p)
                            spawnAccumulator -= 1f
                        }
                    }
                    if (spawnAccumulator > 1f) spawnAccumulator = 1f

                    val iterator = allPetals.iterator()
                    var fallingTracker = currentFallingCount
                    
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        
                        if (!p.isGrounded) {
                            p.update(deltaTime)
                            
                            val isOffBottom = p.y > canvas.height + p.size * 4
                            val isBlowingOffScreen = p.isBlowingAway && (p.y < -p.size * 10 || p.x < -p.size * 10 || p.x > canvas.width + p.size * 10)

                            if (isOffBottom || isBlowingOffScreen) {
                                // If we have too many petals already, remove this one to prevent "waves"
                                if (fallingTracker > targetCount) {
                                    iterator.remove()
                                    fallingTracker--
                                    continue
                                }

                                if (isOffBottom && !p.isBlowingAway && collect && groundedCount < WallpaperConfig.MAX_GROUNDED_PETALS) {
                                    if (Random.nextFloat() < WallpaperConfig.SETTLE_PROBABILITY) {
                                        val maxPileY = canvas.height * (1f - WallpaperConfig.MAX_PILE_HEIGHT_PERCENT)
                                        p.y = canvas.height - (Random.nextFloat() * (canvas.height - maxPileY))
                                        p.isGrounded = true
                                        fallingTracker--
                                    } else {
                                        p.reset()
                                    }
                                } else {
                                    p.reset()
                                }
                            }
                        }
                        p.draw(canvas)
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

        private fun createNewPetal(width: Int, height: Int, count: Int, collect: Boolean): Petal {
            return Petal(
                width,
                height,
                count,
                prefs.getFloat("wind_strength", 0.5f),
                prefs.getFloat("petal_size", 20f),
                prefs.getFloat("fall_speed", 0.5f),
                prefs.getInt("petal_color", Color.WHITE),
                prefs.getInt("petal_alpha", 200),
                collect,
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
                val matrix = Matrix()
                matrix.setScale(scale, scale)
                matrix.postTranslate(dx, dy)
                canvas.drawBitmap(bitmap, matrix, null)
            } ?: canvas.drawColor(Color.BLACK)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt(x*x + y*y + z*z) / SensorManager.GRAVITY_EARTH
                
                if (gForce > (WallpaperConfig.SHAKE_THRESHOLD / 9.81f) + 1.0f) {
                    allPetals.forEach { it.blowAway() }
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
        }
    }
}
