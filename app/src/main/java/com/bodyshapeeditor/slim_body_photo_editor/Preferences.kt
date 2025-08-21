package com.bodyshapeeditor.slim_body_photo_editor

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    private val mySharedPreferences: SharedPreferences

    init {
        mySharedPreferences = context.getSharedPreferences("makemeedit", Context.MODE_PRIVATE)
    }


    fun setAppOpen(value: Boolean?) {
        mySharedPreferences.edit().putBoolean("isOpen", value ?: false).apply()
    }

    fun getAppOpen(): Boolean {
        return mySharedPreferences.getBoolean("isOpen", false)
    }


    fun setPermissionDenied(value: Int?) {
        mySharedPreferences.edit().putInt("times", value ?: 0).apply()
    }

    fun getPermissionDenied(): Int {
        return mySharedPreferences.getInt("times", 0)
    }

}