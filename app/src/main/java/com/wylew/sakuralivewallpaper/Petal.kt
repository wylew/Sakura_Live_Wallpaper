package com.wylew.sakuralivewallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    var x = Random.nextFloat() * screenWidth
    var y = Random.nextFloat() * screenHeight
    
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

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        createSakuraPath()
    }

    private fun createSakuraPath() {
        path.reset()
        val s = size
        path.moveTo(0f, s)
        path.cubicTo(s * 1.2f, s * 0.5f, s * 1.2f, -s * 0.5f, s * 0.3f, -s)
        path.lineTo(0f, -s * 0.7f)
        path.lineTo(-s * 0.3f, -s)
        path.cubicTo(-s * 1.2f, -s * 0.5f, -s * 1.2f, s * 0.5f, 0f, s)
        path.close()
    }

    fun update(deltaTime: Float) {
        if (isGrounded) return

        if (isBlowingAway) {
            x += blowAwaySpeedX * deltaTime
            y += blowAwaySpeedY * deltaTime
            blowAwaySpeedY += 500f * deltaTime // Gravity
            rotationZ += 10f
            
            // Boundary check for blowing away handled by service or here
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

        // X-axis wrapping
        if (x > screenWidth + size * 4) x = -size * 2
        if (x < -size * 4) x = screenWidth.toFloat() + size * 2
    }

    fun reset() {
        x = Random.nextFloat() * screenWidth
        y = -size * 4
        isGrounded = false
        isBlowingAway = false
        swayOffset = Random.nextFloat() * Math.PI.toFloat() * 2
        horizontalDrift = (Random.nextFloat() - 0.5f) * 30f
        verticalOscillationOffset = Random.nextFloat() * Math.PI.toFloat() * 2
        
        baseRotSpeedX = (Random.nextFloat() - 0.5f) * 0.04f
        baseRotSpeedY = (Random.nextFloat() - 0.5f) * 0.1f
        baseRotSpeedZ = (Random.nextFloat() - 0.5f) * 0.06f
    }

    fun blowAway() {
        if (!isGrounded) return
        isGrounded = false
        isBlowingAway = true
        blowAwaySpeedX = (Random.nextFloat() - 0.2f) * 800f
        blowAwaySpeedY = -Random.nextFloat() * 600f - 200f
    }

    fun draw(canvas: Canvas) {
        paint.color = color
        paint.alpha = alpha
        
        canvas.save()
        canvas.translate(x, y)
        
        val scaleX = cos(rotationX.toDouble()).toFloat()
        val scaleY = sin(rotationY.toDouble()).toFloat()
        
        canvas.scale(scaleX, scaleY)
        canvas.rotate(rotationZ)
        
        createSakuraPath()
        canvas.drawPath(path, paint)
        canvas.restore()
    }
    
    fun updateSettings(size: Float, windStrength: Float, speed: Float, color: Int, alpha: Int, collectAtBottom: Boolean, rotationSpeed: Float) {
        this.size = size
        this.windStrength = windStrength
        this.speed = speed
        this.color = color
        this.alpha = alpha
        this.collectAtBottom = collectAtBottom
        this.rotationSpeed = rotationSpeed
        
        if (!collectAtBottom && isGrounded) {
            isGrounded = false
            // Will be reset by service on next update if it's below screen
        }
    }
}
