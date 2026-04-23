/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android_g.egg

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast

class PlatLogoActivity : Activity() {

    private lateinit var mToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mToast = Toast.makeText(this, "Zombie art by Jack Larson", Toast.LENGTH_SHORT)

        val content = ImageView(this)
        content.setImageResource(R.drawable.g_platlogo)
        content.scaleType = ImageView.ScaleType.FIT_CENTER

        setContentView(content)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            mToast.show()
        }
        return super.dispatchTouchEvent(ev)
    }
}