package com.android_k.egg

import android.content.Context

object SpUtils {
    fun getInt(context: Context, key: String, def: Int): Int {
        val prefs = context.getSharedPreferences("egg", Context.MODE_PRIVATE)
        return prefs.getInt(key, def)
    }

    fun putInt(context: Context, key: String, value: Int) {
        val prefs = context.getSharedPreferences("egg", Context.MODE_PRIVATE)
        prefs.edit().putInt(key, value).apply()
    }
}
