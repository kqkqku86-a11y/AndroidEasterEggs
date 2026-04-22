package com.android_k.egg

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.android_k.egg.SpUtils
import com.dede.basic.utils.TransformationMethodUtils

class PlatLogoActivity : Activity() {

    private lateinit var mContent: FrameLayout

    companion object {
        const val BGCOLOR = 0xffed1d24.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val bold = Typeface.create("sans-serif", Typeface.BOLD)
        val light = Typeface.create("sans-serif-light", Typeface.NORMAL)

        mContent = FrameLayout(this).apply {
            setBackgroundColor(0xC0000000.toInt())
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.k_platlogo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            visibility = View.INVISIBLE
        }

        val bg = View(this).apply {
            setBackgroundColor(BGCOLOR)
            alpha = 0f
        }

        val letter = TextView(this).apply {
            typeface = bold
            textSize = 300f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            text = "K"
        }

        val p = (4 * metrics.density).toInt()

        val tv = TextView(this).apply {
            light?.let { typeface = it }
            textSize = 30f
            setPadding(p, p, p, p)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            transformationMethod =
                TransformationMethodUtils.createAllCapsTransformationMethod(this@PlatLogoActivity)
            text = "Android 4.4"
            visibility = View.INVISIBLE
        }

        mContent.apply {
            addView(bg)
            addView(letter, lp)
            addView(logo, lp)

            val lp2 = FrameLayout.LayoutParams(lp).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (10f * p).toInt()
            }

            addView(tv, lp2)
        }

        var clicks = 0

        mContent.setOnClickListener {
            clicks++
            if (clicks >= 6) {
                mContent.performLongClick()
                return@setOnClickListener
            }

            letter.animate().cancel()
            val offset = letter.rotation.toInt() % 360

            letter.animate()
                .rotationBy((if (Math.random() > 0.5f) 360f else -360f) - offset)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(700)
                .start()
        }

        mContent.setOnLongClickListener {
            if (logo.visibility != View.VISIBLE) {

                bg.scaleX = 0.01f
                bg.animate().alpha(1f).scaleX(1f).setStartDelay(500).start()

                letter.animate()
                    .alpha(0f)
                    .scaleY(0.5f)
                    .scaleX(0.5f)
                    .rotationBy(360f)
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(1000)
                    .start()

                logo.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    scaleX = 0.5f
                    scaleY = 0.5f
                    animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(1000)
                        .setStartDelay(500)
                        .setInterpolator(AnticipateOvershootInterpolator())
                        .start()
                }

                tv.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(1000)
                        .setStartDelay(1000)
                        .start()
                }

                true
            } else false
        }

        logo.setOnLongClickListener {
            if (SpUtils.getLong(it.context, "k_egg_mode", 0) == 0L) {
                SpUtils.putLong(
                    it.context,
                    "k_egg_mode",
                    System.currentTimeMillis()
                )
            }

            try {
                startActivity(Intent(this, DessertCase::class.java))
            } catch (ex: ActivityNotFoundException) {
                android.util.Log.e("PlatLogoActivity", "Couldn't catch a break.")
            }

            finish()
            true
        }

        setContentView(mContent)
    }
}
