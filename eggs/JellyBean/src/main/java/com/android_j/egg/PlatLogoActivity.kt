/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android_j.egg

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import java.util.Random

class PlatLogoActivity : Activity() {

    lateinit var mToast: Toast
    lateinit var mContent: ImageView
    var mCount: Int = 0
    val mHandler: Handler = Handler()

    private fun makeView(): View {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val view = LinearLayout(this)
        view.orientation = LinearLayout.VERTICAL
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val light = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val normal = Typeface.create("sans-serif", Typeface.BOLD)

        val size = 14 * metrics.density
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER_HORIZONTAL
        lp.bottomMargin = (-4 * metrics.density).toInt()

        var tv = TextView(this)
        if (light != null) tv.typeface = light
        tv.textSize = 1.25f * size
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.setShadowLayer(4 * metrics.density, 0f, 2 * metrics.density, 0x66000000)
        tv.text = "Android 4." + (Random().nextInt(3) + 1)
        tv.gravity = Gravity.CENTER
        view.addView(tv, lp)

        tv = TextView(this)
        if (normal != null) tv.typeface = normal
        tv.textSize = size
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.setShadowLayer(4 * metrics.density, 0f, 2 * metrics.density, 0x66000000)
        tv.text = "JELLY BEAN"
        view.addView(tv, lp)

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG)
        mToast.view = makeView()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        mContent = ImageView(this)
        mContent.setImageResource(R.drawable.j_platlogo_alt)
        mContent.scaleType = ImageView.ScaleType.CENTER_INSIDE

        val p = (32 * metrics.density).toInt()
        mContent.setPadding(p, p, p, p)

        mContent.setOnClickListener {
            mToast.show()
            mContent.setImageResource(R.drawable.j_platlogo)
        }

        mContent.setOnLongClickListener {
            try {
                startActivity(Intent(this@PlatLogoActivity, BeanBag::class.java))
            } catch (ex: ActivityNotFoundException) {
                android.util.Log.e("PlatLogoActivity", "Couldn't find a bag of beans.")
            }
            finish()
            true
        }

        setContentView(mContent)
    }
}