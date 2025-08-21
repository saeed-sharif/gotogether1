package com.bodyshapeeditor.slim_body_photo_editor.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.Keep

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.bodyshapeeditor.slim_body_photo_editor.R

@Keep
fun isAlreadyPurchased(): Boolean {
    return false
}

fun Activity.showAdAndGo(afterAdWork: () -> Unit) {
    if (!isAlreadyPurchased()) {
        if (isInterstitialLoaded()) {
            interListener(afterAdWork)
            showDailog(this)
            showInterstitial()
        } else {
            afterAdWork()
        }
    } else {
        afterAdWork()
    }
}

private fun showDailog(context: Context) {
    var adLoadingDialog = AdLoadingDialog(context)
    adLoadingDialog.show()
    adLoadingDialog.dismissAfterDelay(2000)
}

fun Activity.showInterstitial() {
    InterstitialAdUpdated.getInstance().showInterstitialAdNew(this)
}

fun isInterstitialLoaded(): Boolean {
    return InterstitialAdUpdated.getInstance().getInter() != null
}

fun Activity.interListener(afterAdWork: () -> Unit) {
    InterstitialAdUpdated.getInstance().setListener(this, afterAdWork)
}




fun showBanner(context: Context, frameLayout: FrameLayout, parent: FrameLayout, ads: TextView) {
    if (isNetworkAvailable(context) && !getIsAdRemove(context)) {
        val adView = AdView(context)
        val adaptiveAds = BannerAds(context)
        val adRequest = AdRequest.Builder().build()
        adView.adUnitId = context.resources.getString(R.string.bannerAd)
        frameLayout.addView(adView)
        adView.setAdSize(adaptiveAds.adSize)
        adView.loadAd(adRequest)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                ads.visibility = View.GONE
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                ads.visibility = View.GONE
                frameLayout.visibility = View.GONE
                parent.visibility = View.GONE

            }
        }
    }else{
        ads.visibility = View.GONE
        frameLayout.visibility = View.GONE
        parent.visibility = View.GONE
    }

}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}


