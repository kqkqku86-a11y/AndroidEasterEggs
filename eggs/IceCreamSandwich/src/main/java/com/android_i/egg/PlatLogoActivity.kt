/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.content.Intent
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
    private var mCount: Int = 0

    private val mHandler = Handler(Looper.getMainLooper())

    private val mSuperLongPress = object : Runnable {
        override fun run() {
            mCount++

            val mZzz = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            mZzz.vibrate(50L * mCount)

            val scale = 1f + 0.25f * mCount * mCount

            val drawable: Drawable? = mContent.drawable

            if (drawable is VectorDrawable) {
                val newWidth = drawable.intrinsicWidth * scale
                val newHeight = drawable.intrinsicHeight * scale

                val left = -((newWidth - drawable.intrinsicWidth) / 2f).toInt()
                val top = -((newHeight - drawable.intrinsicHeight) / 2f).toInt()
                val right = (newWidth + left).toInt()
                val bottom = (newHeight + top).toInt()

                drawable.setBounds(left, top, right, bottom)
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
                    startActivity(
                        Intent(this@PlatLogoActivity, Nyandroid::class.java)
                    )
                } catch (ex: Exception) {
                    android.util.Log.e(
                        "PlatLogoActivity",
                        "Couldn't find platlogo screensaver."
                    )
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

            setOnTouchListener { v, event ->
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
        // fix leak
        mHandler.removeCallbacks(mSuperLongPress)
        super.onDestroy()
    }
}