package com.bodyshapeeditor.slim_body_photo_editor.ads

import android.content.Context
import androidx.annotation.Keep


private const val PREFS_FILENAME = "my_prefs"
// SharedPreferences keys
private const val ALRMSWITCH = "switech_key" // Corrected key
private const val ISADrEMOVE = "is_ad_key" // Corrected key


@Keep
fun saveIsAdsRemove(context: Context, value: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putBoolean(ISADrEMOVE, value)
    editor.apply()
}

// Function to retrieve boolean value
fun getIsAdRemove(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(ISADrEMOVE, false) // Default value is false
}