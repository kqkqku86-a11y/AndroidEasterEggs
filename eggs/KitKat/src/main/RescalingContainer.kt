package com.android_k.egg

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class RescalingContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    fun setView(view: android.view.View) {
        removeAllViews()
        addView(view)
    }
}
