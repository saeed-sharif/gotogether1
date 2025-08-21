package com.bodyshapeeditor.slim_body_photo_editor

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateFailureListener
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateSuccessListener
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.bodyshapeeditor.slim_body_photo_editor.ads.AppOpenManager
import com.bodyshapeeditor.slim_body_photo_editor.ads.getIsAdRemove
import java.util.concurrent.atomic.AtomicBoolean

class SplashScreen : AppCompatActivity() {

    private val TAG: String =
        SplashScreen::class.java.getSimpleName()
    private var appOpenManager: AppOpenManager? = null
    private var countDownTimer: CountDownTimer? = null

    private var consentInformation: ConsentInformation? = null

    // Use an atomic boolean to initialize the Google Mobile Ads SDK and load ads once.
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestConcernFrom()
    }

    override fun onResume() {
        super.onResume()
        AppOpenManager.adLoaded = false
        AppOpenManager.isShowingAd = false
    }

    private fun requestConcernFrom() {
        //        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
//                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//                .addTestDeviceHashedId("C56BCFE285BE2A5EDC95E4A901B37071")
//                .build();


        // Create a ConsentRequestParameters object.


        val params = ConsentRequestParameters.Builder().build() //                .setConsentDebugSettings(debugSettings)


        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        //        consentInformation.reset();
        consentInformation!!.requestConsentInfoUpdate(
            this,
            params,
            OnConsentInfoUpdateSuccessListener {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this,
                    OnConsentFormDismissedListener { loadAndShowError: FormError? ->
                        if (loadAndShowError != null) {
                            initializeMobileAdsSdk()
                            //                                    // Consent gathering failed.
                            Log.w(
                                TAG, String.format(
                                    "%s: %s",
                                    loadAndShowError.errorCode,
                                    loadAndShowError.message
                                )
                            )
                        }
                        // Consent has been gathered.
                        if (consentInformation!!.canRequestAds()) {
                            initializeMobileAdsSdk()
                        }
                    } as OnConsentFormDismissedListener
                )
            } as OnConsentInfoUpdateSuccessListener,
            OnConsentInfoUpdateFailureListener { requestConsentError: FormError? ->
                // Consent gathering failed.
                initializeMobileAdsSdk()
            } as OnConsentInfoUpdateFailureListener)

        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        if (consentInformation!!.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        if (getIsAdRemove(this)) {
            intentToHomeScreen()
        } else {
            MobileAds.initialize(this@SplashScreen) {
                if (isNetworkAvailable()) {
                    appOpenManager = AppOpenManager(this@SplashScreen)
                    appOpenManager?.fetchAd(resources.getString(R.string.appopen))

                    countDownTimer = object : CountDownTimer(5000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            if (AppOpenManager.adsisLoaded()) {
                                appOpenManager?.showAdIfAvailable()
                                countDownTimer!!.cancel()
                                Log.d("mmmm", "ads is show")
                            }
                        }

                        override fun onFinish() {
                            intentToHomeScreen()
                        }
                    }.start()
                } else {
                    intentToHomeScreen()
                }
            }
        }
    }

    fun intentToHomeScreen() {
        Handler().postDelayed({
            val intent = Intent(
                this@SplashScreen,
                MainActivity::class.java
            )
            intent.putExtra("sourceIntent", "howToUse")
            startActivity(intent)
            //                requestPermissions();
            finish()
        }, 300)
    }

    fun stopCountdown() {
        countDownTimer!!.cancel()
        Log.d("mmmm", "stop countdown")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}