package com.heao.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final int DURATION = 1;
    // 设置时间间隔为1min
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(DURATION);

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);
        // 一个用来发送intent的Context，
        // 一个区分PendingIntent来源的请求代码，
        // 一个待发送的Intent对象
        // 一组用来决定如何创建PendingIntent的标志符
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            // 一个描述定时器时间基准的常量
            // 定时器启动的时间
            // 定时器循环的时间间隔
            // 一个到时要发送的PendingIntent
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        } else {
            // 取消定时器
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent
                .getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public PollService() {
        super(TAG);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }
        Log.d(TAG, "onHandleIntent() called");
        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        // TODO 这些items有什么用？怎样传回Fragment中？
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.size() == 0) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            Notification notification = new NotificationCompat
                    .Builder(this, "NotificationChannelID")
                    .setTicker(resources.getString(R.string.new_pictures_title))
//                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setSmallIcon(R.drawable.bill_up_close)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                //只在Android O之上需要渠道，这里的第一个参数要和下面的channelId一样
                NotificationChannel notificationChannel = new NotificationChannel(
                        "NotificationChannelID", "name", NotificationManager.IMPORTANCE_HIGH);
                //如果这里用IMPORTANCE_NOENE就需要在系统的设置里面开启渠道，通知才能正常弹出
                notificationManager.createNotificationChannel(notificationChannel);
            }
            notificationManager.notify(0, notification);
        }
        QueryPreferences.setLastResultId(this, resultId);
    }

    /**
     * 检查后台网络可用性
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetwork() != null;
        boolean isNetworkConnected = cm.getActiveNetworkInfo().isConnected();
        return isNetworkAvailable && isNetworkConnected;
    }
}
