package com.android_i.egg

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.Toast

class PlatLogoActivity : Activity() {

    private var mToast: Toast? = null
    private lateinit var mContent: ImageView
    private var mCount = 0

    private val mHandler = Handler(Looper.getMainLooper())

    private val mSuperLongPress = object : Runnable {
        override fun run() {
            mCount++

            val vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vib.vibrate(50L * mCount)

            val scale = 1f + 0.25f * mCount * mCount

            val drawable = mContent.drawable

            if (drawable is VectorDrawable) {
                val newW = drawable.intrinsicWidth * scale
                val newH = drawable.intrinsicHeight * scale

                val left = -((newW - drawable.intrinsicWidth) / 2f).toInt()
                val top = -((newH - drawable.intrinsicHeight) / 2f).toInt()

                drawable.setBounds(
                    left,
                    top,
                    (newW + left).toInt(),
                    (newH + top).toInt()
                )
            } else {
                mContent.scaleX = scale
                mContent.scaleY = scale
            }

            if (mCount <= 3) {
                mHandler.postDelayed(
                    this,
                    ViewConfiguration.getLongPressTimeout().toLong()
                )
            } else {
                try {
                    startActivity(Intent(this@PlatLogoActivity, Nyandroid::class.java))
                } catch (e: Exception) {
                    android.util.Log.e("PlatLogoActivity", "Couldn't find Nyandroid")
                }
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mToast = Toast.makeText(
            this,
            "Android 4.0: Ice Cream Sandwich",
            Toast.LENGTH_SHORT
        )

        mContent = ImageView(this).apply {
            setImageResource(R.drawable.i_platlogo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE

            setOnTouchListener { _, event ->
                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        mHandler.removeCallbacks(mSuperLongPress)
                        mCount = 0
                        mHandler.postDelayed(
                            mSuperLongPress,
                            2 * ViewConfiguration.getLongPressTimeout().toLong()
                        )
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isPressed) {
                            isPressed = false
                            mHandler.removeCallbacks(mSuperLongPress)
                            mToast?.show()
                        }
                    }
                }
                true
            }
        }

        setContentView(mContent)
    }

    override fun onDestroy() {
        mHandler.removeCallbacks(mSuperLongPress)
        super.onDestroy()
    }
}