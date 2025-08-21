package com.genderswap.faceswapphotoeditor.extention

import android.view.View
import androidx.annotation.Keep
import com.genderswap.faceswapphotoeditor.extention.SafeClickListener

@Keep
object ViewExtention {
    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }
}