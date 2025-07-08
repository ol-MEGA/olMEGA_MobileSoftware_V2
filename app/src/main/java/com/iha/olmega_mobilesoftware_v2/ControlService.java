package com.iha.olmega_mobilesoftware_v2;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;

import java.util.List;

public class ControlService extends Service {
    private String TAG = this.getClass().getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private SystemStatus systemStatus;
    public SystemStatus Status() { return systemStatus;}

    private Handler mTaskHandler = new Handler();
    private int mActivityCheckTime = 5000;

    private final BroadcastReceiver mDisplayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "android.intent.action.SCREEN_ON":
                        LogIHAB.log("Display: on");
                        break;
                    case "android.intent.action.SCREEN_OFF":
                        LogIHAB.log("Display: off");
                        break;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        systemStatus = new SystemStatus(this);
        //Log.d(TAG, "Service onCreate");
        mTaskHandler.post(mActivityCheckRunnable);

        // Register receiver for display activity
        IntentFilter displayFilter = new IntentFilter();
        displayFilter.addAction(Intent.ACTION_SCREEN_ON);
        displayFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mDisplayReceiver, displayFilter);
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

            PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
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
        startForeground();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Log.d(TAG, "Service destroyed");
        Status().onDestroy();
        unregisterReceiver(mDisplayReceiver);
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
