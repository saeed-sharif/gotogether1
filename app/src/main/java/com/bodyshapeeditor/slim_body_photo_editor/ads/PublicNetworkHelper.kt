package com.bodyshapeeditor.slim_body_photo_editor.ads

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.BuildConfig.DEBUG

@Keep
class PublicNetworkHelper : AudienceNetworkAds.InitListener {

    override fun onInitialized(result: AudienceNetworkAds.InitResult) {
        Log.d(AudienceNetworkAds.TAG, result.message)
    }

    companion object {
        internal fun initialize(context: Context) {
            if (!AudienceNetworkAds.isInitialized(context)) {
                if (DEBUG) {
                    AdSettings.turnOnSDKDebugger(context)
                }

                AudienceNetworkAds.buildInitSettings(context)
                    .withInitListener(PublicNetworkHelper())
                    .initialize()
            }
        }
    }
}
