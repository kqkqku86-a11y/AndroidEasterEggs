package com.android_i.egg

import android.animation.TimeAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.max
import kotlin.random.Random

class Nyandroid : Activity() {

    class Board(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

        companion object {
            const val FIXED_STARS = true
            const val NUM_CATS = 20
            val rng = Random
        }

        private fun lerp(a: Float, b: Float, f: Float) = (b - a) * f + a
        private fun rand(a: Float, b: Float) = lerp(a, b, rng.nextFloat())

        inner class FlyingCat(ctx: Context) : ImageView(ctx) {

            var v = 0f
            var dist = 0f

            // ✅ SOLUSI 2 FIX: rename backing field
            private var _z = 0f

            var component: ComponentName? = null

            // ✅ Java-style getter/setter manual (NO conflict)
            fun getZ(): Float = _z
            fun setZ(value: Float) { _z = value }

            init {
                setImageResource(R.drawable.i_nyandroid_anim)
            }

            fun reset() {
                val scale = lerp(0.1f, 2f, _z)
                scaleX = scale
                scaleY = scale

                x = -scale * width
                y = rand(0f, (this@Board.height - height).toFloat())

                v = lerp(100f, 1000f, _z)
                dist = 0f
            }

            fun update(dt: Float) {
                dist += v * dt
                x += v * dt
            }
        }

        private var anim: TimeAnimator? = null

        init {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            setBackgroundColor(0xFF003366.toInt())
        }

        private fun reset() {
            removeAllViews()

            val wrap = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            if (FIXED_STARS) {
                repeat(20) {
                    val star = ImageView(context)
                    star.setImageResource(R.drawable.i_star_anim)
                    addView(star, wrap)
                }
            }

            repeat(NUM_CATS) {
                val cat = FlyingCat(context)
                addView(cat, wrap)

                // 🔥 FIXED: pakai setter manual
                cat.setZ((it.toFloat() / NUM_CATS) * (it.toFloat() / NUM_CATS))

                cat.reset()
                cat.x = rand(0f, width.toFloat())
            }

            anim?.cancel()
            anim = TimeAnimator().apply {
                setTimeListener { _, _, dt ->
                    val sec = dt / 1000f

                    for (i in 0 until childCount) {
                        val v = getChildAt(i)
                        if (v is FlyingCat) {
                            v.update(sec)

                            if (v.x > width || v.x < -100) {
                                v.reset()
                            }
                        }
                    }
                }
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            post {
                reset()
                anim?.start()
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            anim?.cancel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        setContentView(Board(this, null))
    }
}