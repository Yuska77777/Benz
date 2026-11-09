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
import kotlin.random.Random

class PinkHeartWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = HeartEngine()

    inner class HeartEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var bitmap: Bitmap? = null
        private var visible = false
        private var w = 1
        private var h = 1
        private var time = 0f

        private data class Particle(
            var x: Float, var y: Float, var size: Float, var speed: Float,
            var drift: Float, var alpha: Int, var life: Int
        )

        private val hearts = mutableListOf<Particle>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val heartPath = Path()
        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 33L)
            }
        }

        init {
            setTouchEventsEnabled(true)
            bitmap = try {
                BitmapFactory.decodeStream(assets.open("wallpaper.png"))
            } catch (_: Exception) {
                null
            }
            repeat(10) { hearts += newHeart(Random.nextFloat(), Random.nextFloat()) }
        }

        private fun newHeart(x: Float, y: Float) = Particle(
            x.coerceIn(0f, 1f), y.coerceIn(0f, 1f),
            11f + Random.nextFloat() * 19f,
            0.0007f + Random.nextFloat() * 0.0013f,
            (Random.nextFloat() - 0.5f) * 0.0011f,
            105 + Random.nextInt(120), 180 + Random.nextInt(260)
        )

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width.coerceAtLeast(1)
            h = height.coerceAtLeast(1)
            drawFrame()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(frame)
            if (visible) handler.post(frame)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                val count = if (event.action == MotionEvent.ACTION_DOWN) 9 else 2
                repeat(count) {
                    val px = (event.x + Random.nextFloat() * 76f - 38f) / w
                    val py = (event.y + Random.nextFloat() * 76f - 38f) / h
                    hearts += newHeart(px, py).also {
                        it.size *= 0.8f
                        it.life = 100 + Random.nextInt(90)
                        it.speed *= 1.7f
                    }
                }
                if (hearts.size > 75) hearts.subList(0, hearts.size - 75).clear()
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
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
        }

        private fun render(c: Canvas) {
            c.drawColor(0xFFFFF3F7.toInt())
            val bmp = bitmap
            if (bmp != null && !bmp.isRecycled) {
                val scale = max(w.toFloat() / bmp.width, h.toFloat() / bmp.height)
                val dw = bmp.width * scale
                val dh = bmp.height * scale
                val sway = kotlin.math.sin(time * 0.025f) * 5f
                val dst = RectF(
                    (w - dw) / 2f + sway, (h - dh) / 2f,
                    (w + dw) / 2f + sway, (h + dh) / 2f
                )
                paint.alpha = 255
                c.drawBitmap(bmp, null, dst, paint)
            }

            time += 1f
            val iterator = hearts.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.y -= p.speed * 16f
                p.x += p.drift * 16f + kotlin.math.sin((time + p.life) * 0.035f) * 0.00018f
                p.life--
                if (p.life <= 0 || p.y < -0.12f) {
                    iterator.remove()
                    continue
                }
                val fade = min(1f, p.life / 45f)
                drawHeart(c, p.x * w, p.y * h, p.size, (p.alpha * fade).toInt().coerceIn(0, 255))
            }
            while (hearts.size < 10) hearts += newHeart(Random.nextFloat(), 1.08f)

            for (i in 0..4) {
                val phase = time * 0.035f + i * 1.7f
                val x = (0.10f + i * 0.19f + kotlin.math.sin(phase) * 0.025f) * w
                val y = (0.18f + (i % 2) * 0.58f + kotlin.math.cos(phase) * 0.025f) * h
                paint.color = 0xFFFFC8DC.toInt()
                paint.alpha = (90 + 90 * (0.5f + 0.5f * kotlin.math.sin(phase))).toInt()
                c.drawCircle(x, y, 2.2f, paint)
                paint.alpha = 35
                c.drawCircle(x, y, 6f, paint)
            }
        }

        private fun drawHeart(c: Canvas, cx: Float, cy: Float, size: Float, alpha: Int) {
            val half = size / 2f
            heartPath.reset()
            heartPath.moveTo(cx, cy + half * 0.78f)
            heartPath.cubicTo(cx - size * 1.15f, cy + half * 0.05f, cx - size * 0.85f, cy - half * 0.85f, cx, cy - half * 0.28f)
            heartPath.cubicTo(cx + size * 0.85f, cy - half * 0.85f, cx + size * 1.15f, cy + half * 0.05f, cx, cy + half * 0.78f)
            paint.color = 0xFFFF5C91.toInt()
            paint.alpha = alpha
            c.drawPath(heartPath, paint)
        }
    }
}
