package com.bodyshapeeditor.slim_body_photo_editor.ads

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.Keep
import com.google.android.gms.ads.AdSize

@Keep
class BannerAds(private var context: Context) {
    val adSize: AdSize
        get() {
            val window =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            val display =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    context.display
                } else {
                    window.defaultDisplay
                }
            val outMetrics = DisplayMetrics()
            display?.getRealMetrics(outMetrics)
            val widthPixels = outMetrics.widthPixels.toFloat()
            val density = outMetrics.density
            val adWidth = (widthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                adWidth
            )
        }
}