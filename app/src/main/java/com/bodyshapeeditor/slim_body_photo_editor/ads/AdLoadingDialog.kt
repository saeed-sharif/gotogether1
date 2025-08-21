package com.bodyshapeeditor.slim_body_photo_editor.ads


import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import com.bodyshapeeditor.slim_body_photo_editor.R


@Keep
class AdLoadingDialog(context: Context) : AlertDialog(context) {
    private val handler: Handler = Handler(Looper.getMainLooper())
    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading)
    }

    fun dismissAfterDelay(delayMillis: Long) {
        handler.postDelayed({
            if (isShowing) {
                try {
                    dismiss()
                }catch (e: Exception){
                    Log.d("exception","${e.message}")
                }
            }
        }, delayMillis)
    }
}