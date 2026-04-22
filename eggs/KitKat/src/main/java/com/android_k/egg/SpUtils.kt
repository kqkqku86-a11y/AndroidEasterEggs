package com.android_k.egg

import android.content.Context

object SpUtils {

    private const val PREF = "egg_prefs"

    fun getLong(context: Context, key: String, def: Long): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(key, def)
    }

    fun putLong(context: Context, key: String, value: Long) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, value)
            .apply()
    }
}