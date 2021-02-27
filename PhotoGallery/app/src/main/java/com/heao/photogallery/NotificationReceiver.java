package com.heao.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK) {
            // 应用正在运行中就会动态注册receiver 并且设置resultCode为CANCELED
            return;
        }

        int requestCode = intent.getIntExtra(PollService.REQUEST_CODE, 0);
        Notification notification = (Notification) intent.getParcelableExtra(PollService.NOTIFICATION);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 只在Android O之上需要 channelId和notification创建时相同
            NotificationChannel notificationChannel = new NotificationChannel(
                    PollService.NOTIF_CHANNEL_ID, "name", NotificationManager.IMPORTANCE_HIGH);
            // 用IMPORTANCE_NONE就需要在系统的设置里面开启渠道，通知才能正常弹出
            notificationManager.createNotificationChannel(notificationChannel);
        }
        notificationManager.notify(requestCode, notification);
    }
}
