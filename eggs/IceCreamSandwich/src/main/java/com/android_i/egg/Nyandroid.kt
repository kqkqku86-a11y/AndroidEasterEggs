/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import kotlin.random.Random

class Nyandroid : Activity() {

    companion object {
        const val DEBUG = false
    }

    class Board(context: Context, as: AttributeSet?) : FrameLayout(context, as) {

        companion object {
            const val FIXED_STARS = true
            const val NUM_CATS = 20

            val sRNG = Random.Default

            fun lerp(a: Float, b: Float, f: Float): Float {
                return (b - a) * f + a
            }

            fun randfrange(a: Float, b: Float): Float {
                return lerp(a, b, sRNG.nextFloat())
            }

            fun randsign(): Int {
                return if (sRNG.nextBoolean()) 1 else -1
            }

            fun <E> pick(array: Array<E>): E? {
                if (array.isEmpty()) return null
                return array[sRNG.nextInt(array.size)]
            }
        }

        inner class FlyingCat(context: Context, as: AttributeSet?) : ImageView(context, as) {

            companion object {
                const val VMAX = 1000.0f
                const val VMIN = 100.0f
            }

            var v: Float = 0f
            var vr: Float = 0f
            var dist: Float = 0f
            var z: Float = 0f
            var component: ComponentName? = null

            init {
                setImageResource(R.drawable.i_nyandroid_anim)

                if (DEBUG) setBackgroundColor(0x80FF0000.toInt())
            }

            override fun toString(): String {
                return "<cat (${x}, ${y}) (${width} x ${height})>"
            }

            fun reset() {
                val scale = lerp(0.1f, 2f, z)
                scaleX = scale
                scaleY = scale

                x = -scale * width + 1
                y = randfrange(0f, (this@Board.height - scale * height).toFloat())
                v = lerp(VMIN, VMAX, z)

                dist = 0f

                Log.d("Nyandroid", "reset cat: $this")
            }

            fun update(dt: Float) {
                dist += v * dt
                x = x + v * dt
            }
        }

        private var mAnim: TimeAnimator? = null

        init {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            setBackgroundColor(0xFF003366.toInt())
        }

        private fun reset() {
            Log.d("Nyandroid", "board reset")
            removeAllViews()

            val wrap = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )

            if (FIXED_STARS) {
                for (i in 0 until 20) {
                    val fixedStar = ImageView(context)
                    if (DEBUG) fixedStar.setBackgroundColor(0x8000FF80.toInt())
                    fixedStar.setImageResource(R.drawable.i_star_anim)

                    addView(fixedStar, wrap)

                    val scale = randfrange(0.1f, 1f)
                    fixedStar.scaleX = scale
                    fixedStar.scaleY = scale
                    fixedStar.x = randfrange(0f, width.toFloat())
                    fixedStar.y = randfrange(0f, height.toFloat())

                    val anim = fixedStar.drawable as AnimationDrawable

                    for (n in 0 until anim.numberOfFrames) {
                        val drawable = anim.getFrame(n)
                        if (drawable is BitmapDrawable) {
                            drawable.setTargetDensity(480)
                        }
                    }

                    postDelayed({
                        anim.start()
                    }, randfrange(0f, 1000f).toLong())
                }
            }

            for (i in 0 until NUM_CATS) {
                val nv = FlyingCat(context, null)
                addView(nv, wrap)

                nv.z = (i.toFloat() / NUM_CATS)
                nv.z *= nv.z

                nv.reset()
                nv.x = randfrange(0f, width.toFloat())

                val anim = nv.drawable as AnimationDrawable

                for (n in 0 until anim.numberOfFrames) {
                    val drawable = anim.getFrame(n)
                    if (drawable is BitmapDrawable) {
                        drawable.setTargetDensity(480)
                    }
                }

                postDelayed({
                    anim.start()
                }, randfrange(0f, 1000f).toLong())
            }

            mAnim?.cancel()

            mAnim = TimeAnimator().apply {
                setTimeListener { _, totalTime, deltaTime ->
                    Log.d("Nyandroid", "t=$totalTime")

                    for (i in 0 until childCount) {
                        val v = getChildAt(i)
                        if (v !is FlyingCat) continue

                        v.update(deltaTime / 1000f)

                        val catWidth = v.width * v.scaleX
                        val catHeight = v.height * v.scaleY

                        if (v.x + catWidth < -2 ||
                            v.x > width + 2 ||
                            v.y + catHeight < -2 ||
                            v.y > height + 2
                        ) {
                            v.reset()
                        }
                    }
                }
            }
        }

        private val postStart = Runnable {
            reset()
            mAnim?.start()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            Log.d("Nyandroid", "resized: ${w}x$h")
            post(postStart)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            removeCallbacks(postStart)
            mAnim?.cancel()
        }

        override fun isOpaque(): Boolean {
            return true
        }
    }

    private var mBoard: Board? = null

    override fun onStart() {
        super.onStart()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
    }

    override fun onResume() {
        super.onResume()

        mBoard = Board(this, null)
        setContentView(mBoard)

//        mBoard?.setOnSystemUiVisibilityChangeListener {
//            if (it and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
//                finish()
//            }
//        }
    }

    override fun onUserInteraction() {
        Log.d("Nyandroid", "finishing on user interaction")
        finish()
    }
}