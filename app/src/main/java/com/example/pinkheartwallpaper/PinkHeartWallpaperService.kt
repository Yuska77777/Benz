package com.example.pinkheartwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class HeartParticle(
    var x: Float,
    var y: Float,
    var size: Float,
    var speed: Float,
    var drift: Float,
    var alpha: Int,
    var life: Int
)

class PinkHeartWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = HeartEngine()

    private inner class HeartEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val heartPath = Path()
        private val particles = mutableListOf<HeartParticle>()
        private var wallpaperBitmap: Bitmap? = null
        private var screenWidth = 1
        private var screenHeight = 1
        private var frameNumber = 0f
        private var isVisible = false

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) handler.postDelayed(this, 33L)
            }
        }

        init {
            setTouchEventsEnabled(true)
            wallpaperBitmap = try {
                assets.open("wallpaper.png").use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
            repeat(12) {
                particles.add(createParticle(Random.nextFloat(), Random.nextFloat()))
            }
        }

        private fun createParticle(x: Float, y: Float): HeartParticle =
            HeartParticle(
                x = x.coerceIn(0f, 1f),
                y = y.coerceIn(0f, 1.1f),
                size = 10f + Random.nextFloat() * 18f,
                speed = 0.0007f + Random.nextFloat() * 0.0012f,
                drift = (Random.nextFloat() - 0.5f) * 0.001f,
                alpha = 110 + Random.nextInt(130),
                life = 180 + Random.nextInt(260)
            )

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            screenWidth = width.coerceAtLeast(1)
            screenHeight = height.coerceAtLeast(1)
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            handler.removeCallbacks(drawRunnable)
            if (visible) handler.post(drawRunnable)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            isVisible = false
            handler.removeCallbacks(drawRunnable)
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_MOVE
            ) {
                val count = if (event.action == MotionEvent.ACTION_DOWN) 12 else 3
                repeat(count) {
                    val x = (event.x + Random.nextFloat() * 70f - 35f) / screenWidth
                    val y = (event.y + Random.nextFloat() * 70f - 35f) / screenHeight
                    val particle = createParticle(x, y)
                    particle.size *= 0.85f
                    particle.speed *= 1.7f
                    particle.life = 90 + Random.nextInt(90)
                    particles.add(particle)
                }
                while (particles.size > 90) particles.removeAt(0)
                drawFrame()
            }
            super.onTouchEvent(event)
        }

        private fun drawFrame() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
                // The surface can temporarily be unavailable while Android changes screens.
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // Ignore a surface that was destroyed during drawing.
                    }
                }
            }
        }

        private fun render(canvas: Canvas) {
            canvas.drawColor(0xFFFFF3F7.toInt())

            wallpaperBitmap?.takeUnless { it.isRecycled }?.let { bitmap ->
                val scale = max(
                    screenWidth.toFloat() / bitmap.width,
                    screenHeight.toFloat() / bitmap.height
                )
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale
                val sway = sin(frameNumber * 0.018f) * 4f
                val destination = RectF(
                    (screenWidth - drawWidth) / 2f + sway,
                    (screenHeight - drawHeight) / 2f,
                    (screenWidth + drawWidth) / 2f + sway,
                    (screenHeight + drawHeight) / 2f
                )
                paint.alpha = 255
                canvas.drawBitmap(bitmap, null, destination, paint)
            }

            frameNumber += 1f

            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val particle = iterator.next()
                particle.y -= particle.speed * 16f
                particle.x += particle.drift * 16f +
                    sin((frameNumber + particle.life) * 0.035f) * 0.00015f
                particle.life -= 1

                if (particle.life <= 0 || particle.y < -0.15f) {
                    iterator.remove()
                } else {
                    val fade = min(1f, particle.life / 45f)
                    drawHeart(
                        canvas,
                        particle.x * screenWidth,
                        particle.y * screenHeight,
                        particle.size,
                        (particle.alpha * fade).toInt().coerceIn(0, 255)
                    )
                }
            }

            while (particles.size < 12) {
                particles.add(createParticle(Random.nextFloat(), 1.08f))
            }

            for (index in 0 until 6) {
                val phase = frameNumber * 0.035f + index * 1.7f
                val x = (0.08f + index * 0.16f + sin(phase) * 0.025f) * screenWidth
                val y = (0.16f + (index % 2) * 0.62f) * screenHeight
                paint.color = 0xFFFFC8DC.toInt()
                paint.alpha = (90 + 90 * (0.5f + 0.5f * sin(phase))).toInt()
                canvas.drawCircle(x, y, 2.5f, paint)
                paint.alpha = 35
                canvas.drawCircle(x, y, 7f, paint)
            }
        }

        private fun drawHeart(canvas: Canvas, centerX: Float, centerY: Float, size: Float, alpha: Int) {
            val half = size / 2f
            heartPath.reset()
            heartPath.moveTo(centerX, centerY + half * 0.8f)
            heartPath.cubicTo(
                centerX - size * 1.15f, centerY + half * 0.05f,
                centerX - size * 0.85f, centerY - half * 0.85f,
                centerX, centerY - half * 0.28f
            )
            heartPath.cubicTo(
                centerX + size * 0.85f, centerY - half * 0.85f,
                centerX + size * 1.15f, centerY + half * 0.05f,
                centerX, centerY + half * 0.8f
            )
            paint.color = 0xFFFF5C91.toInt()
            paint.alpha = alpha
            canvas.drawPath(heartPath, paint)
        }
    }
}
