package com.wylew.sakuralivewallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Petal(
    var screenWidth: Int,
    var screenHeight: Int,
    var count: Int,
    var windStrength: Float,
    var size: Float,
    var speed: Float,
    var color: Int,
    var alpha: Int,
    var collectAtBottom: Boolean,
    var rotationSpeed: Float
) {
    var x = 0f
    var y = 0f
    
    private var rotationX = Random.nextFloat() * 360f
    private var rotationY = Random.nextFloat() * 360f
    private var rotationZ = Random.nextFloat() * 360f
    
    private var baseRotSpeedX = (Random.nextFloat() - 0.5f) * 0.04f
    private var baseRotSpeedY = (Random.nextFloat() - 0.5f) * 0.1f
    private var baseRotSpeedZ = (Random.nextFloat() - 0.5f) * 0.06f

    private var swayOffset = Random.nextFloat() * Math.PI.toFloat() * 2
    private var swaySpeed = 0.5f + Random.nextFloat() * 1.5f
    private var horizontalDrift = (Random.nextFloat() - 0.5f) * 30f
    private var verticalOscillationOffset = Random.nextFloat() * Math.PI.toFloat() * 2
    private var verticalOscillationSpeed = 0.5f + Random.nextFloat() * 1f
    
    var isGrounded = false
    var isBlowingAway = false
    private var blowAwaySpeedX = 0f
    private var blowAwaySpeedY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val transformationMatrix = Matrix()

    // Cached values for draw performance
    private var cachedScaleX = 1f
    private var cachedScaleY = 1f
    private var cachedBw = 0f
    private var cachedBh = 0f
    private var petalBitmap: Bitmap? = null

    companion object {
        private var cachedBitmap: Bitmap? = null
        private var lastSize = -1f
        private var lastColor = -1

        private fun getOrCreateBitmap(size: Float, color: Int): Bitmap {
            if (cachedBitmap != null && lastSize == size && lastColor == color && !cachedBitmap!!.isRecycled) {
                return cachedBitmap!!
            }
            
            lastSize = size
            lastColor = color
            
            val padding = 2f
            val bw = (size * 2.4f) + padding * 2
            val bh = (size * 2f) + padding * 2
            
            val softwareBitmap = Bitmap.createBitmap(
                bw.toInt().coerceAtLeast(1), 
                bh.toInt().coerceAtLeast(1), 
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(softwareBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
            }
            
            val cx = size * 1.2f + padding
            val cy = size + padding
            val s = size
            
            val path = Path()
            path.moveTo(cx, cy + s)
            path.cubicTo(cx + s * 1.2f, cy + s * 0.5f, cx + s * 1.2f, cy - s * 0.5f, cx + s * 0.3f, cy - s)
            path.lineTo(cx, cy - s * 0.7f)
            path.lineTo(cx - s * 0.3f, cy - s)
            path.cubicTo(cx - s * 1.2f, cy - s * 0.5f, cx - s * 1.2f, cy + s * 0.5f, cx, cy + s)
            path.close()
            
            canvas.drawPath(path, paint)
            
            // Optimization: Convert to HARDWARE bitmap if supported (API 26+)
            val finalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hwBitmap = softwareBitmap.copy(Bitmap.Config.HARDWARE, false)
                softwareBitmap.recycle()
                hwBitmap ?: softwareBitmap
            } else {
                softwareBitmap
            }
            
            cachedBitmap = finalBitmap
            return finalBitmap
        }
        
        fun clearCache() {
            cachedBitmap?.recycle()
            cachedBitmap = null
            lastSize = -1f
            lastColor = -1
        }
    }

    init {
        updateCachedDimensions()
        updatePetalBitmap()
        reset(fullRandomY = true)
    }

    private fun updateCachedDimensions() {
        val padding = 2f
        cachedBw = (size * 2.4f) + padding * 2
        cachedBh = (size * 2f) + padding * 2
    }

    private fun updatePetalBitmap() {
        petalBitmap = getOrCreateBitmap(size, color)
    }

    fun update(deltaTime: Float) {
        if (isGrounded) return

        if (isBlowingAway) {
            x += blowAwaySpeedX * deltaTime
            y += blowAwaySpeedY * deltaTime
            blowAwaySpeedY += 500f * deltaTime // Gravity
            rotationZ += 10f
            updateRotationalConstants()
            return
        }

        verticalOscillationOffset += deltaTime * verticalOscillationSpeed
        val vOsc = sin(verticalOscillationOffset.toDouble()).toFloat() * 15f
        val baseSpeed = (40f + speed * 160f) + vOsc
        y += baseSpeed * deltaTime
        
        swayOffset += deltaTime * swaySpeed
        val breeze = sin(swayOffset.toDouble()).toFloat() * (15f + windStrength * 120f)
        val windDrift = windStrength * 250f
        x += (windDrift + breeze + horizontalDrift) * deltaTime
        
        rotationX += baseRotSpeedX * rotationSpeed
        rotationY += baseRotSpeedY * rotationSpeed
        rotationZ += baseRotSpeedZ * rotationSpeed
        
        updateRotationalConstants()

        // X-axis wrapping
        if (x > screenWidth + size * 4) x = -size * 2
        if (x < -size * 4) x = screenWidth.toFloat() + size * 2
    }

    private fun updateRotationalConstants() {
        cachedScaleX = cos(rotationX.toDouble()).toFloat()
        cachedScaleY = sin(rotationY.toDouble()).toFloat()
    }

    fun reset(fullRandomY: Boolean = false) {
        x = Random.nextFloat() * screenWidth
        y = if (fullRandomY) {
            Random.nextFloat() * screenHeight
        } else {
            -(Random.nextFloat() * screenHeight * 0.8f + size * 5)
        }
        
        isGrounded = false
        isBlowingAway = false
        swayOffset = Random.nextFloat() * Math.PI.toFloat() * 2
        horizontalDrift = (Random.nextFloat() - 0.5f) * 30f
        verticalOscillationOffset = Random.nextFloat() * Math.PI.toFloat() * 2
        
        baseRotSpeedX = (Random.nextFloat() - 0.5f) * 0.04f
        baseRotSpeedY = (Random.nextFloat() - 0.5f) * 0.1f
        baseRotSpeedZ = (Random.nextFloat() - 0.5f) * 0.06f
        
        updateRotationalConstants()
    }

    fun blowAway() {
        if (!isGrounded) return
        isGrounded = false
        isBlowingAway = true
        blowAwaySpeedX = (Random.nextFloat() - 0.2f) * 800f
        blowAwaySpeedY = -Random.nextFloat() * 600f - 200f
    }

    fun draw(canvas: Canvas) {
        val bitmap = petalBitmap ?: return
        paint.alpha = alpha
        
        transformationMatrix.reset()
        transformationMatrix.postTranslate(-cachedBw / 2f, -cachedBh / 2f)
        transformationMatrix.postScale(cachedScaleX, cachedScaleY)
        transformationMatrix.postRotate(rotationZ)
        transformationMatrix.postTranslate(x, y)
        
        canvas.drawBitmap(bitmap, transformationMatrix, paint)
    }
    
    fun updateSettings(
        screenWidth: Int,
        screenHeight: Int,
        size: Float,
        windStrength: Float,
        speed: Float,
        color: Int,
        alpha: Int,
        collectAtBottom: Boolean,
        rotationSpeed: Float
    ) {
        val oldWidth = this.screenWidth
        val oldHeight = this.screenHeight
        val oldSize = this.size
        val oldColor = this.color
        
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.size = size
        this.windStrength = windStrength
        this.speed = speed
        this.color = color
        this.alpha = alpha
        this.collectAtBottom = collectAtBottom
        this.rotationSpeed = rotationSpeed
        
        if (oldSize != size || oldColor != color) {
            updateCachedDimensions()
            updatePetalBitmap()
        }
        
        if (!collectAtBottom && isGrounded) {
            isGrounded = false
        }

        if (isGrounded && (oldWidth != screenWidth || oldHeight != screenHeight)) {
            x = Random.nextFloat() * screenWidth
            val maxPileY = screenHeight * (1f - WallpaperConfig.MAX_PILE_HEIGHT_PERCENT)
            y = screenHeight - (Random.nextFloat() * (screenHeight - maxPileY))
        }
        
        if (!isGrounded && !isBlowingAway) {
            if (x > screenWidth + size * 10) x = Random.nextFloat() * screenWidth
            if (y > screenHeight + size * 10) y = -(Random.nextFloat() * screenHeight * 0.5f)
        }
    }
}
