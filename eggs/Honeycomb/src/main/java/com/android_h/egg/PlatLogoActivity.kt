/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android_h.egg

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast

class PlatLogoActivity : Activity() {

    private lateinit var mToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mToast = Toast.makeText(this, "REZZZZZZZ...", Toast.LENGTH_SHORT)

        val content = ImageView(this)
        content.setImageResource(randomPlatlogo())
        // content.setImageResource(R.drawable.h_platlogo)
        content.scaleType = ImageView.ScaleType.CENTER_INSIDE

        setContentView(content)
    }

    private fun randomPlatlogo(): Int {
        val r = Math.random()
        if (r >= 0.5) {
            return R.drawable.h_platlogo_1
        }
        return R.drawable.h_platlogo
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            mToast.show()
        }
        return super.dispatchTouchEvent(ev)
    }
}