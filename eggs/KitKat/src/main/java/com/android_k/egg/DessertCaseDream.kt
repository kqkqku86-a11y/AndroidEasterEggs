/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android_k.egg

import android.service.dreams.DreamService

class DessertCaseDream : DreamService() {

    private lateinit var mView: DessertCaseView
    private lateinit var mContainer: DessertCaseView.RescalingContainer
    private var action: Runnable? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setInteractive(false)

        mView = DessertCaseView(this)

        mContainer = DessertCaseView.RescalingContainer(this)

        mContainer.setView(mView)

        setContentView(mContainer)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()

        action = Runnable {
            mView.start()
        }

        mView.postDelayed(action!!, 1000)
    }

    override fun onDreamingStopped() {
        action?.let { mView.removeCallbacks(it) }
        super.onDreamingStopped()
        mView.stop()
    }
}