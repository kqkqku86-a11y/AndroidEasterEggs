package com.android_k.egg

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class RescalingContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    fun setView(v: View) {
        removeAllViews()
        addView(v)
    }
}