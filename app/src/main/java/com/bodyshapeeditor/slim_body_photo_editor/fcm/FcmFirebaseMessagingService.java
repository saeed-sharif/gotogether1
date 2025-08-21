package com.bodyshapeeditor.slim_body_photo_editor.fcm;


import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.firebase.BuildConfig;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.atomic.AtomicInteger;

@Keep
public class FcmFirebaseMessagingService extends FirebaseMessagingService {

    String ICON_KEY = "icon";
    String APP_TITLE_KEY = "title";
    String SHORT_DESC_KEY = "short_desc";
    String LONG_DESC_KEY = "long_desc";
    String APP_FEATURE_KEY = "feature";
    String APP_URL_KEY = "app_url";
    String IS_PREMIUM = "is_premium";
    private final AtomicInteger seed = new AtomicInteger();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("MyFCMToken", "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("MyFCMToken", "Message data payload: " + remoteMessage.getData());

        } else {
            Log.d("MyFCMToken", "Message Notification Body: " + remoteMessage.getData());

        }

        // Check if message contains a notification payload.
        remoteMessage.getData();
        Log.d("MyFCMToken", "1" + remoteMessage.getData().get("title"));
        Log.d("MyFCMToken", "2" + remoteMessage.getData().get("short_desc"));

        String iconURL = remoteMessage.getData().get(ICON_KEY);
        String title = remoteMessage.getData().get(APP_TITLE_KEY);
        String shortDesc = remoteMessage.getData().get(SHORT_DESC_KEY);
        String longDesc = remoteMessage.getData().get(LONG_DESC_KEY);
        String feature = remoteMessage.getData().get(APP_FEATURE_KEY);
        String appURL = remoteMessage.getData().get(APP_URL_KEY);

        if (iconURL != null && title != null && shortDesc != null && feature != null && appURL != null) {
            String standard = "https://play.google.com/store/flash/details?id=";

            try {
                String id = appURL.substring(standard.length());
                if (BuildConfig.DEBUG) Log.e("package sent ", id);
                if (!TinyDB.getInstance(this).getBoolean(IS_PREMIUM)) {
                    Log.d("MyFCMToken2", "3" + remoteMessage.getData().get("title"));
                    MyNotificationManager.getInstance(getApplicationContext()).displayNotification(
                            title, shortDesc, longDesc,
                            appURL, feature, iconURL, getNotificationID());
                }

            } catch (Exception e) {
                if (BuildConfig.DEBUG)
                    Log.e("MyFCMToken", "package not valid");
            }


        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d("MyFCMToken", "onNewToken: " + s);
    }

    private int getNotificationID() {
        return seed.incrementAndGet();
    }
}

