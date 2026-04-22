/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android_k.egg

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager

class DessertCase : Activity() {

    private lateinit var mView: DessertCaseView
    private var action: Runnable? = null

    override fun onStart() {
        super.onStart()

        val pm: PackageManager = packageManager
        val cn = ComponentName(this, DessertCaseDream::class.java)

        if (pm.getComponentEnabledSetting(cn)
            != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        ) {
            // Slog.v("DessertCase", "ACHIEVEMENT UNLOCKED")
            pm.setComponentEnabledSetting(
                cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        mView = DessertCaseView(this)

        val container = DessertCaseView.RescalingContainer(this)
        container.setView(mView)

        setContentView(container)
    }

    override fun onResume() {
        super.onResume()

        action = Runnable {
            mView.start()
        }

        mView.postDelayed(action!!, 1000)
    }

    override fun onPause() {
        action?.let { mView.removeCallbacks(it) }
        super.onPause()
        mView.stop()
    }
}