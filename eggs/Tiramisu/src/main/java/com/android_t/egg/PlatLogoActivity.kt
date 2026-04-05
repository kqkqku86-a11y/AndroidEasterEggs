package com.android_t.egg

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.AnalogClock
import android.widget.FrameLayout
import android.widget.ImageView
import org.json.JSONObject
import kotlin.math.*

class PlatLogoActivity : Activity() {

    companion object {
        private const val TAG = "PlatLogoActivity"
        private const val S_EGG_UNLOCK_SETTING = "egg_mode_s"
    }

    // ---------------------------------------------------------------
    // Bubble — versi extended dengan field `drawable` untuk COLR emoji
    // ---------------------------------------------------------------
    class Bubble {
        var x: Float = 0f
        var y: Float = 0f
        var r: Float = 0f
        var color: Int = 0
        var text: CharSequence? = null
        var drawable: Drawable? = null  // ← field tambahan yang dibutuhkan COLREmojiCompat
    }

    private lateinit var mClock: SettableAnalogClock
    private lateinit var mLogo: ImageView
    private lateinit var mBg: BubblesDrawable

    private var mPressureMin = 0.0
    private var mPressureMax = -1.0

    override fun onPause() {
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setNavigationBarColor(0)
        window.setStatusBarColor(0)
        actionBar?.hide()

        val layout = FrameLayout(this)
        mClock = SettableAnalogClock(this)

        val dm: DisplayMetrics = resources.displayMetrics
        val dp = dm.density
        val minSide = min(dm.widthPixels, dm.heightPixels)
        val widgetSize = (minSide * 0.75).toInt()
        val lp = FrameLayout.LayoutParams(widgetSize, widgetSize).apply {
            gravity = Gravity.CENTER
        }
        layout.addView(mClock, lp)

        mLogo = ImageView(this).apply { visibility = View.GONE }
        mLogo.setImageResource(R.drawable.t_android_logo)
        layout.addView(mLogo, lp)

        mBg = BubblesDrawable().apply {
            level = 0
            avoid = widgetSize / 2f
            padding = 0.5f * dp
            minR = 1f * dp
        }
        layout.background = mBg
        layout.setOnLongClickListener(mBg)
        setContentView(layout)
    }

    private fun shouldWriteSettings() = packageName == "android"

    private fun launchNextStage(locked: Boolean) {
        mClock.animate()
            .alpha(0f).scaleX(0.5f).scaleY(0.5f)
            .withEndAction { mClock.visibility = View.GONE }
            .start()

        mLogo.alpha = 0f
        mLogo.scaleX = 0.5f
        mLogo.scaleY = 0.5f
        mLogo.visibility = View.VISIBLE
        mLogo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setInterpolator(OvershootInterpolator())
            .start()

        mLogo.postDelayed({
            ObjectAnimator.ofInt(mBg, "level", 0, 10000).apply {
                interpolator = DecelerateInterpolator(1f)
                start()
            }
        }, 500)

        val cr: ContentResolver = contentResolver
        try {
            if (shouldWriteSettings()) {
                Log.v(TAG, "Saving egg unlock=$locked")
                syncTouchPressure()
                Settings.System.putLong(
                    cr, S_EGG_UNLOCK_SETTING,
                    if (locked) 0L else System.currentTimeMillis()
                )
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Can't write settings", e)
        }

        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .addCategory("com.android.internal.category.PLATLOGO")
            )
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "No more eggs.")
        }
    }

    private fun measureTouchPressure(event: MotionEvent) {
        val pressure = event.pressure
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (mPressureMax < 0) { mPressureMin = pressure.toDouble(); mPressureMax = pressure.toDouble() }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressure < mPressureMin) mPressureMin = pressure.toDouble()
                if (pressure > mPressureMax) mPressureMax = pressure.toDouble()
            }
        }
    }

    private fun syncTouchPressure() {
        try {
            val touchDataJson = Settings.System.getString(contentResolver, TOUCH_STATS)
            val touchData = JSONObject(touchDataJson ?: "{}")
            if (touchData.has("min")) mPressureMin = min(mPressureMin, touchData.getDouble("min"))
            if (touchData.has("max")) mPressureMax = max(mPressureMax, touchData.getDouble("max"))
            if (mPressureMax >= 0) {
                touchData.put("min", mPressureMin)
                touchData.put("max", mPressureMax)
                if (shouldWriteSettings()) {
                    Settings.System.putString(contentResolver, TOUCH_STATS, touchData.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't write touch settings", e)
        }
    }

    override fun onStart() { super.onStart(); syncTouchPressure() }
    override fun onStop() { syncTouchPressure(); super.onStop() }

    // ---------------------------------------------------------------
    // SettableAnalogClock
    // ---------------------------------------------------------------
    inner class SettableAnalogClock(context: android.content.Context) : AnalogClock(context) {
        private var mOverrideHour = -1
        private var mOverrideMinute = 0

        private fun toPositiveDegrees(rad: Double): Double {
            val deg = Math.toDegrees(rad)
            return if (deg < 0) deg + 360 else deg
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            measureTouchPressure(ev)
            return when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (mOverrideHour < 0) {
                        // derive hour & minute from current time
                        val now = java.util.Calendar.getInstance()
                        mOverrideHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                        mOverrideMinute = now.get(java.util.Calendar.MINUTE)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val x = ev.x; val y = ev.y
                    val cx = width / 2f; val cy = height / 2f
                    val angle = toPositiveDegrees(atan2((x - cx).toDouble(), (y - cy).toDouble())).toFloat()
                    val minutes = (75 - (angle / 6).toInt()) % 60
                    val minuteDelta = minutes - mOverrideMinute
                    if (minuteDelta != 0) {
                        if (abs(minuteDelta) > 45 && mOverrideHour >= 0) {
                            val hourDelta = if (minuteDelta < 0) 1 else -1
                            mOverrideHour = (mOverrideHour + 24 + hourDelta) % 24
                        }
                        mOverrideMinute = minutes
                        if (mOverrideMinute == 0) {
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            if (scaleX == 1f) {
                                scaleX = 1.05f; scaleY = 1.05f
                                animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                            }
                        } else {
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        postInvalidate()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (mOverrideMinute == 0 && (mOverrideHour % 12) == 1) {
                        Log.v(TAG, "13:00")
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        launchNextStage(false)
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ---------------------------------------------------------------
    // BubblesDrawable
    // ---------------------------------------------------------------
    inner class BubblesDrawable : Drawable(), View.OnLongClickListener {
        private val MAX_BUBBS = 2000
        var avoid = 0f
        var padding = 0f
        var minR = 0f

        private val mBubbs = Array(MAX_BUBBS) { Bubble() }
        private var mNumBubbs = 0
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun draw(canvas: Canvas) {
            if (level == 0) return
            val f = level / 10000f
            mPaint.style = Paint.Style.FILL
            mPaint.textAlign = Paint.Align.CENTER
            for (j in 0 until mNumBubbs) {
                val b = mBubbs[j]
                if (b.color == 0 || b.r == 0f) continue
                if (b.text != null) {
                    mPaint.textSize = b.r * 1.75f
                    canvas.drawText(b.text.toString(), b.x, b.y + b.r * f * 0.6f, mPaint)
                } else {
                    mPaint.color = b.color
                    canvas.drawCircle(b.x, b.y, b.r * f, mPaint)
                }
            }
        }

        override fun onLevelChange(level: Int): Boolean { invalidateSelf(); return true }
        override fun onBoundsChange(bounds: Rect) { super.onBoundsChange(bounds); randomize() }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(cf: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT

        override fun onLongClick(v: View): Boolean {
            if (level == 0) return false
            invalidateSelf()
            return true
        }

        private fun randomize() {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val maxR = min(w, h) / 3f
            mNumBubbs = 0
            if (avoid > 0f) {
                mBubbs[mNumBubbs].apply { x = w / 2f; y = h / 2f; r = avoid; color = 0 }
                mNumBubbs++
            }
            for (j in 0 until MAX_BUBBS) {
                var tries = 5
                while (tries-- > 0) {
                    val bx = (Math.random() * w).toFloat()
                    val by = (Math.random() * h).toFloat()
                    var r = min(min(bx, w - bx), min(by, h - by))
                    for (i in 0 until mNumBubbs) {
                        r = min(r, (hypot((bx - mBubbs[i].x).toDouble(), (by - mBubbs[i].y).toDouble()) - mBubbs[i].r - padding).toFloat())
                        if (r < minR) break
                    }
                    if (r >= minR) {
                        mBubbs[mNumBubbs].apply {
                            x = bx; y = by; this.r = min(maxR, r); color = 0xFF888888.toInt()
                        }
                        mNumBubbs++
                        break
                    }
                }
            }
        }
    }

    companion object {
        const val TOUCH_STATS = "touch.stats"
    }
}
