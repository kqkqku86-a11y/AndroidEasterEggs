package com.android_j.egg

import android.animation.TimeAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.sqrt
import kotlin.random.Random

class BeanBag : Activity() {

    private lateinit var board: Board

    override fun onStart() {
        super.onStart()

        val pm = packageManager
        pm.setComponentEnabledSetting(
            ComponentName(this, BeanBagDream::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            0
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        board = Board(this, null)
        setContentView(board)
    }

    override fun onResume() {
        super.onResume()
        board.startAnimation()
    }

    override fun onPause() {
        super.onPause()
        board.stopAnimation()
    }

    class Board(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

        companion object {
            val rng = Random

            fun lerp(a: Float, b: Float, f: Float) = (b - a) * f + a
            fun rand(a: Float, b: Float) = lerp(a, b, rng.nextFloat())
            fun flip() = rng.nextBoolean()
            fun mag(x: Float, y: Float) = sqrt(x * x + y * y)
            fun clamp(x: Float, a: Float, b: Float) = when {
                x < a -> a
                x > b -> b
                else -> x
            }

            const val NUM_BEANS = 40
            const val MIN_SCALE = 0.2f
            const val MAX_SCALE = 1f
            const val LUCKY = 0.001f
            const val MAX_RADIUS = (576 * MAX_SCALE).toInt()

            val BEANS = intArrayOf(
                R.drawable.j_redbean0,
                R.drawable.j_redbean0,
                R.drawable.j_redbean1,
                R.drawable.j_redbean2,
                R.drawable.j_redbeandroid
            )

            val COLORS = intArrayOf(
                0xFF00CC00.toInt(),
                0xFFCC0000.toInt(),
                0xFF0000CC.toInt(),
                0xFFFFFF00.toInt(),
                0xFFFF8000.toInt()
            )
        }

        private var boardWidth = 0
        private var boardHeight = 0
        private var anim: TimeAnimator? = null

        inner class Bean(ctx: Context) : ImageView(ctx) {

            var _x = 0f
            var _y = 0f
            var a = 0f

            var vx = 0f
            var vy = 0f
            var va = 0f

            var r = 0f
            var z = 0f

            var w = 0
            var h = 0

            private fun pickBean() {
                val id = BEANS.random()
                val drawable = context.getDrawable(id) as BitmapDrawable
                drawable.setTargetDensity(480)

                val bmp = drawable.bitmap
                w = bmp.width
                h = bmp.height

                setImageDrawable(drawable)
            }

            fun reset() {
                pickBean()

                val scale = lerp(MIN_SCALE, MAX_SCALE, z)
                scaleX = scale
                scaleY = scale

                r = 0.3f * maxOf(w, h) * scale

                a = rand(0f, 360f)
                va = rand(-30f, 30f)

                vx = rand(-40f, 40f) * z
                vy = rand(-40f, 40f) * z

                if (flip()) {
                    _x = if (vx < 0) boardWidth + 2 * r else -r * 4
                    _y = rand(0f, boardHeight - 3 * r)
                } else {
                    _y = if (vy < 0) boardHeight + 2 * r else -r * 4
                    _x = rand(0f, boardWidth - 3 * r)
                }
            }

            fun update(dt: Float) {
                _x += vx * dt
                _y += vy * dt
                a += va * dt
            }
        }

        private fun reset() {
            removeAllViews()

            val wrap = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            repeat(NUM_BEANS) { i ->
                val b = Bean(context)
                addView(b, wrap)

                b.z = (i.toFloat() / NUM_BEANS).let { it * it }
                b.reset()
            }

            anim?.cancel()

            anim = TimeAnimator().apply {
                setTimeListener { _, _, dt ->
                    val sec = dt / 1000f

                    for (i in 0 until childCount) {
                        val v = getChildAt(i) as Bean

                        v.update(sec)

                        v.rotation = v.a
                        v.x = v._x - v.pivotX
                        v.y = v._y - v.pivotY

                        if (v._x < -MAX_RADIUS ||
                            v._x > boardWidth + MAX_RADIUS ||
                            v._y < -MAX_RADIUS ||
                            v._y > boardHeight + MAX_RADIUS
                        ) {
                            v.reset()
                        }
                    }
                }
            }
        }

        fun startAnimation() {
            if (anim == null) {
                post {
                    reset()
                    startAnimation()
                }
            } else {
                anim?.start()
            }
        }

        fun stopAnimation() {
            anim?.cancel()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            boardWidth = w
            boardHeight = h

            post {
                reset()
                startAnimation()
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            stopAnimation()
        }

        override fun isOpaque(): Boolean = false
    }
}