package com.iha.olmega_mobilesoftware_v2;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;

public class ControlService extends Service {
    private String TAG = this.getClass().getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private SystemStatus systemStatus;
    public SystemStatus Status() { return systemStatus;}

    private Handler mTaskHandler = new Handler();
    private int mActivityCheckTime = 5000;

    @Override
    public void onCreate() {
        super.onCreate();
        systemStatus = new SystemStatus(this);
        Log.d(TAG, "Service onCreate");
        mTaskHandler.post(mActivityCheckRunnable);
    }

    public void startForeground(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = getString(R.string.app_name);
            String channelName = getString(R.string.app_name);
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.logo)
                    .setTicker(getString(R.string.app_name))
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.app_name))
                    .setContentIntent(intent)
                    .build();
            startForeground(1, notification);
        }
        else
            startForeground(1, new Notification());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        Status().onDestroy();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        ControlService getService() {
            return ControlService.this;
        }
    }

    private Runnable mActivityCheckRunnable = new Runnable() {
        @Override
        public void run() {
            startMainActivity(false);
            mTaskHandler.postDelayed(mActivityCheckRunnable, mActivityCheckTime);
        }
    };

    public boolean startMainActivity(boolean forceStartActivity) {
        boolean isActivityRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        for (int iActivity = 0; iActivity < runningTaskInfo.size(); iActivity++) {
            ComponentName componentInfo = runningTaskInfo.get(iActivity).topActivity;
            if (componentInfo.getPackageName().equals(getPackageName())) {
                isActivityRunning = true;
            }
        }
        if (isActivityRunning == false && (forceStartActivity || (systemStatus.Preferences().autoStartActivity() && systemStatus.Preferences().isInKioskMode && !systemStatus.Preferences().isAdmin()))) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return isActivityRunning;
    }
}
