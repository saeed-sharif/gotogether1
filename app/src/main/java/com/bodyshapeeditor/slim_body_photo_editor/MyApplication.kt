package com.bodyshapeeditor.slim_body_photo_editor

import android.app.Application
import androidx.annotation.Keep
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.bodyshapeeditor.slim_body_photo_editor.ads.InterstitialAdUpdated
import com.bodyshapeeditor.slim_body_photo_editor.ads.PublicNetworkHelper


@Keep
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        InterstitialAdUpdated.getInstance().checkInterstitial(this)

        FirebaseApp.initializeApp(this)

        PublicNetworkHelper.initialize(this)
    }
}