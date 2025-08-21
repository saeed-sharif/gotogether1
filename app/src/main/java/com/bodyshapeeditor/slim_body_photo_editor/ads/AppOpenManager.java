package com.bodyshapeeditor.slim_body_photo_editor.ads;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.bodyshapeeditor.slim_body_photo_editor.SplashScreen;
@Keep
public class AppOpenManager implements Application.ActivityLifecycleCallbacks {
//    testAd
//    private static String AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";

    //    RealAds
    private static String AD_UNIT_ID = "ca-app-pub-5254151939086942/2867676266";

    private static final String LOG_TAG = "mmmm";
    public static boolean adLoaded = false;
    public static boolean isShowingAd = false;
    private AppOpenAd appOpenAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private final SplashScreen myApplication;

    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    public void onActivityDestroyed(Activity activity) {
    }

    public void onActivityPaused(Activity activity) {
    }

    public void onActivityResumed(Activity activity) {
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    public void onActivityStarted(Activity activity) {
    }

    public void onActivityStopped(Activity activity) {
    }

    public AppOpenManager(SplashScreen splash_Activity) {
        this.myApplication = splash_Activity;
    }

    public void fetchAd(String str) {
        AD_UNIT_ID = str;
        isShowingAd = false;
        if (!isAdAvailable()) {
            this.loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {

                public void onAdLoaded(AppOpenAd appOpenAd) {
                    AppOpenManager.this.appOpenAd = appOpenAd;
                    AppOpenManager.adLoaded = true;
                    Log.d(AppOpenManager.LOG_TAG, "Load Success");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.d(AppOpenManager.LOG_TAG, "Load fail");
                    AppOpenManager.this.myApplication.intentToHomeScreen();
                    AppOpenManager.this.myApplication.stopCountdown();
                }
            };
            AppOpenAd.load(this.myApplication, AD_UNIT_ID, getAdRequest(), 1, this.loadCallback);
        }
    }

    public void showAdIfAvailable() {
        if (isShowingAd || !isAdAvailable()) {
            Log.d(LOG_TAG, "Can not show ad.");
            fetchAd(AD_UNIT_ID);
            return;
        }
        Log.d(LOG_TAG, "Will show ad.");
        this.appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppOpenManager.this.appOpenAd = null;
                AppOpenManager.isShowingAd = true;
                AppOpenManager.adLoaded = false;
                AppOpenManager.this.myApplication.intentToHomeScreen();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                AppOpenManager.isShowingAd = true;
            }
        });
        this.appOpenAd.show(this.myApplication);
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    public boolean isAdAvailable() {
        return this.appOpenAd != null;
    }

    public static boolean adsisLoaded() {
        return adLoaded;
    }

    public static boolean adsisShowed() {
        return isShowingAd;
    }
}
