package com.android_k.egg

import android.app.Activity
import com.android_k.egg.RescalingContainer
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle

class DessertCase : Activity() {

    private lateinit var mView: DessertCaseView
    private var action: Runnable? = null

    override fun onStart() {
        super.onStart()

        val pm = packageManager
        val cn = ComponentName(this, DessertCaseDream::class.java)

        if (pm.getComponentEnabledSetting(cn)
            != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        ) {
            pm.setComponentEnabledSetting(
                cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        mView = DessertCaseView(this)

        val container = DessertCaseView.RescalingContainer(this).apply {
            setView(mView)
        }

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
