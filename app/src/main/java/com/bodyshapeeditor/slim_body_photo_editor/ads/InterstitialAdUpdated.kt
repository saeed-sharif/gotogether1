package com.bodyshapeeditor.slim_body_photo_editor.ads

import android.app.Activity
import android.content.Context
import androidx.annotation.Keep
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.bodyshapeeditor.slim_body_photo_editor.R

@Keep
open class InterstitialAdUpdated {

    var mInterstitialAd: InterstitialAd? = null

    companion object {
        @Volatile
        private var instance: InterstitialAdUpdated? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: InterstitialAdUpdated().also { instance = it }
        }

        var count = 0
    }

    fun checkInterstitial(context: Context) {
        if (isNetworkAvailable(context) && mInterstitialAd == null && !getIsAdRemove(context)) {
            loadInterstitialAd(context)
        }
    }

    fun loadInterstitialAd(context: Context) {
        context.let {
            InterstitialAd.load(it,
                it.getString(R.string.interstitialAd),
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(ad: LoadAdError) {
                        count++
                        if (count < 3) {
                            mInterstitialAd = null
                            checkInterstitial(context)
                        }
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        mInterstitialAd = ad
                    }
                })
        }
    }

    fun showInterstitialAdNew(activity: Activity) {
        activity.let {

            mInterstitialAd?.show(it)
        }


    }

    fun getInter(): InterstitialAd? {
        return mInterstitialAd
    }

    fun setListener(context: Context, afterAdWork: () -> Unit) {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd = null
                checkInterstitial(context)
                afterAdWork()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                mInterstitialAd = null
            }
        }
    }
}