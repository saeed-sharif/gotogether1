package com.bodyshapeeditor.slim_body_photo_editor.fcm;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.bodyshapeeditor.slim_body_photo_editor.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Keep
public class MyNotificationManager {

    private static MyNotificationManager myNotificationManager;
    String CHANNEL_ID = "this_is_GenderSwapChannel_id";
    private final Context mContext;
    private ImageView iconImageView, featureImageView;
    private final AtomicInteger deem = new AtomicInteger();

    public MyNotificationManager(Context mContext) {
        this.mContext = mContext;
    }

    public static synchronized MyNotificationManager getInstance(Context mContext) {
        if (myNotificationManager == null) {
            myNotificationManager = new MyNotificationManager(mContext);
        }
        return myNotificationManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void displayNotification(String title, String short_desc, String long_desc, String appUrl, String feature, String iconURL, int notificationID) {


        Intent toAppURL = new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl));

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, toAppURL, PendingIntent.FLAG_UPDATE_CURRENT);

        @SuppressLint("RemoteViewLayout")
        RemoteViews remoteViews = new RemoteViews("com.bodyshapeeditor.slim_body_photo_editor", R.layout.fcm_notification);
        remoteViews.setTextViewText(R.id.tv_title, title);
        remoteViews.setTextViewText(R.id.fcm_short_dis, short_desc);
        remoteViews.setTextViewText(R.id.tv_fcm_long_dis, long_desc);

        if (long_desc != null && !long_desc.isEmpty())
            remoteViews.setViewVisibility(R.id.tv_fcm_long_dis, View.VISIBLE);
        else
            remoteViews.setViewVisibility(R.id.tv_fcm_long_dis, View.GONE);
        try {

            Bitmap bmp = Picasso.get().load(iconURL).get();
            Bitmap bmpFeature = Picasso.get().load(feature).get();
            remoteViews.setImageViewBitmap(R.id.fcm_icon, bmp);
            remoteViews.setImageViewBitmap(R.id.iv_fcm_feature, bmpFeature);
        } catch (IOException e) {
            e.printStackTrace();
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, this.CHANNEL_ID)
                .setContentTitle(title)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews);

        NotificationManager mNotificationManager = (NotificationManager) this.mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(this.CHANNEL_ID,
                    "Make Me Slim Photo Editor", NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(this.CHANNEL_ID);
        }

        mNotificationManager.notify(notificationID, mBuilder.build());

    }
}