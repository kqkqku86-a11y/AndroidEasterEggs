/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package com.android_j.egg

import android.animation.TimeAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.Random
import kotlin.math.sqrt

class BeanBag : Activity() {

    private lateinit var mBoard: Board

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

        mBoard = Board(this, null)
        setContentView(mBoard)
    }

    override fun onPause() {
        super.onPause()
        mBoard.stopAnimation()
    }

    override fun onResume() {
        super.onResume()
        mBoard.startAnimation()
    }

    class Board(context: Context, asAttrs: AttributeSet?) : FrameLayout(context, asAttrs) {

        companion object {
            val sRNG = Random()

            fun lerp(a: Float, b: Float, f: Float) = (b - a) * f + a
            fun randfrange(a: Float, b: Float) = lerp(a, b, sRNG.nextFloat())
            fun randsign() = if (sRNG.nextBoolean()) 1 else -1
            fun flip() = sRNG.nextBoolean()
            fun mag(x: Float, y: Float) = sqrt((x * x + y * y).toDouble()).toFloat()
            fun clamp(x: Float, a: Float, b: Float) = when {
                x < a -> a
                x > b -> b
                else -> x
            }

            fun dot(x1: Float, y1: Float, x2: Float, y2: Float) =
                x1 * x2 + y1 + y2

            fun <E> pick(array: Array<E>): E? =
                if (array.isEmpty()) null else array[sRNG.nextInt(array.size)]

            fun pickInt(array: IntArray): Int =
                if (array.isEmpty()) 0 else array[sRNG.nextInt(array.size)]

            const val NUM_BEANS = 40
            const val MIN_SCALE = 0.2f
            const val MAX_SCALE = 1f
            const val LUCKY = 0.001f
            const val MAX_RADIUS = (576 * MAX_SCALE).toInt()

            val BEANS = intArrayOf(
                R.drawable.j_redbean0,
                R.drawable.j_redbean0,
                R.drawable.j_redbean0,
                R.drawable.j_redbean0,
                R.drawable.j_redbean1,
                R.drawable.j_redbean1,
                R.drawable.j_redbean2,
                R.drawable.j_redbean2,
                R.drawable.j_redbeandroid
            )

            val COLORS = intArrayOf(
                0xFF00CC00.toInt(),
                0xFFCC0000.toInt(),
                0xFF0000CC.toInt(),
                0xFFFFFF00.toInt(),
                0xFFFF8000.toInt(),
                0xFF00CCFF.toInt(),
                0xFFFF0080.toInt(),
                0xFF8000FF.toInt(),
                0xFFFF8080.toInt(),
                0xFF8080FF.toInt(),
                0xFFB0C0D0.toInt(),
                0xFFDDDDDD.toInt(),
                0xFF333333.toInt()
            )
        }

        inner class Bean(context: Context, attrs: AttributeSet?) : ImageView(context, attrs) {

            var x = 0f
            var y = 0f
            var a = 0f

            var va = 0f
            var vx = 0f
            var vy = 0f

            var r = 0f
            var z = 0f

            var h = 0
            var w = 0

            var grabbed = false
            var grabx = 0f
            var graby = 0f
            var grabtime: Long = 0
            var grabx_offset = 0f
            var graby_offset = 0f

            private fun pickBean() {
                var beanId = pickInt(BEANS)

                if (randfrange(0f, 1f) <= LUCKY) {
                    beanId = R.drawable.j_jandycane
                }

                val bean = context.resources.getDrawable(beanId) as BitmapDrawable
                bean.setTargetDensity(480)
                val bmp = bean.bitmap

                h = bmp.height
                w = bmp.width

                setImageDrawable(bean)

                val color = pickInt(COLORS)
                val cm = ColorMatrix()
                val m = cm.array

                m[0] = ((color and 0x00FF0000) shr 16) / 255f
                m[5] = ((color and 0x0000FF00) shr 8) / 255f
                m[10] = (color and 0x000000FF) / 255f

                val pt = Paint(Paint.ANTI_ALIAS_FLAG)
                pt.colorFilter = ColorMatrixColorFilter(cm)

                setLayerType(
                    View.LAYER_TYPE_HARDWARE,
                    if (beanId == R.drawable.j_jandycane) null else pt
                )
            }

            fun reset() {
                pickBean()

                val scale = lerp(MIN_SCALE, MAX_SCALE, z)
                scaleX = scale
                scaleY = scale

                r = 0.3f * maxOf(h, w) * scale

                a = randfrange(0f, 360f)
                va = randfrange(-30f, 30f)

                vx = randfrange(-40f, 40f) * z
                vy = randfrange(-40f, 40f) * z

                val boardh = boardHeight.toFloat()
                val boardw = boardWidth.toFloat()

                if (flip()) {
                    x = if (vx < 0) boardw + 2 * r else -r * 4f
                    y = randfrange(0f, boardh - 3 * r) * 0.5f +
                            if (vy < 0) boardh * 0.5f else 0f
                } else {
                    y = if (vy < 0) boardh + 2 * r else -r * 4f
                    x = randfrange(0f, boardw - 3 * r) * 0.5f +
                            if (vx < 0) boardw * 0.5f else 0f
                }
            }

            fun update(dt: Float) {
                if (grabbed) {
                    vx = vx * 0.75f + ((grabx - x) / dt) * 0.25f
                    x = grabx

                    vy = vy * 0.75f + ((graby - y) / dt) * 0.25f
                    y = graby
                } else {
                    x += vx * dt
                    y += vy * dt
                    a += va * dt
                }
            }

            fun overlap(other: Bean): Float {
                val dx = x - other.x
                val dy = y - other.y
                return mag(dx, dy) - r - other.r
            }

            private fun isTouchedBean(e: MotionEvent): Boolean {
                val drawable = drawable as? BitmapDrawable ?: return false
                val bmp = drawable.bitmap

                val sx = bmp.width.toFloat() / width
                val sy = bmp.height.toFloat() / height

                val px = (e.x * sx).toInt()
                val py = (e.y * sy).toInt()

                if (px !in 0 until bmp.width || py !in 0 until bmp.height) return false

                return Color.alpha(bmp.getPixel(px, py)) > 0
            }

            override fun onTouchEvent(e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isTouchedBean(e)) return false
                        grabbed = true
                        grabx_offset = e.rawX - x
                        graby_offset = e.rawY - y
                        va = 0f
                    }

                    MotionEvent.ACTION_MOVE -> {
                        grabx = e.rawX - grabx_offset
                        graby = e.rawY - graby_offset
                        grabtime = e.eventTime
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        grabbed = false
                        var a = randsign() * clamp(mag(vx, vy) * 0.33f, 0f, 1080f)
                        va = randfrange(a * 0.5f, a)
                    }
                }
                return true
            }
        }

        private var boardWidth = 0
        private var boardHeight = 0
        private var mAnim: TimeAnimator? = null

        init {
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE)
            setWillNotDraw(false)
        }

        private fun reset() {
            removeAllViews()

            val wrap = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )

            for (i in 0 until NUM_BEANS) {
                val b = Bean(context, null)
                addView(b, wrap)

                b.z = (i.toFloat() / NUM_BEANS)
                b.z *= b.z

                b.reset()
                b.x = randfrange(0f, boardWidth.toFloat())
                b.y = randfrange(0f, boardHeight.toFloat())
            }

            mAnim?.cancel()
            mAnim = TimeAnimator().apply {
                setTimeListener { _, _, dt ->
                    for (i in 0 until childCount) {
                        val v = getChildAt(i)
                        if (v !is Bean) continue

                        v.update(dt / 1000f)

                        for (j in i + 1 until childCount) {
                            val v2 = getChildAt(j)
                            if (v2 is Bean) {
                                v.overlap(v2)
                            }
                        }

                        v.rotation = v.a
                        v.x = v.x - v.pivotX
                        v.y = v.y - v.pivotY

                        if (v.x < -MAX_RADIUS ||
                            v.x > boardWidth + MAX_RADIUS ||
                            v.y < -MAX_RADIUS ||
                            v.y > boardHeight + MAX_RADIUS
                        ) {
                            v.reset()
                        }
                    }

                    if (DEBUG) invalidate()
                }
            }
        }

        fun startAnimation() {
            stopAnimation()
            if (mAnim == null) {
                post {
                    reset()
                    startAnimation()
                }
            } else {
                mAnim?.start()
            }
        }

        fun stopAnimation() {
            mAnim?.cancel()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            stopAnimation()
        }

        override fun isOpaque(): Boolean = false

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            boardWidth = w
            boardHeight = h
        }

        override fun onDraw(canvas: Canvas) {
            if (DEBUG) {
                val pt = Paint(Paint.ANTI_ALIAS_FLAG)
                pt.style = Paint.Style.STROKE
                pt.color = Color.RED
                pt.strokeWidth = 4f

                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pt)
            }
        }
    }

    companion object {
        const val DEBUG = false
    }
}