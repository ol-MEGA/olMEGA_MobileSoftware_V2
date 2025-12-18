package com.iha.olmega_mobilesoftware_v2;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
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

        // Register receiver for StageState broadcasts coming from StageUSBCapture
        IntentFilter stageFilter = new IntentFilter();
        stageFilter.addAction("StageState");
        registerReceiver(mStageStateReceiver, stageFilter);
    }

    // BroadcastReceiver to react to StageUSBCapture state changes
    private final BroadcastReceiver mStageStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            int stateOrdinal = intent.getIntExtra("currentState", -1);
            if (stateOrdinal < 0) return;
            try {
                // If connected, try to bring MainActivity to front so AudioRecord can initialize properly
                if (stateOrdinal == com.iha.olmega_mobilesoftware_v2.States.connected.ordinal()) {
                    LogIHAB.log("StageState: connected received in Service - bringing MainActivity to front");
                    // try to move existing task to front or start activity
                    boolean running = startMainActivity(true);
                    if (!running) {
                        // startMainActivity(true) started the activity; otherwise explicit fallback
                        // nothing more to do here
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed handling StageState broadcast: " + e.getMessage());
            }
        }
    };

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
        try {
            unregisterReceiver(mStageStateReceiver);
        } catch (Exception ignored) {}
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
        try {
            // Try to find existing AppTask and move it to front (preferred)
            List<ActivityManager.AppTask> appTasks = manager.getAppTasks();
            if (appTasks != null) {
                for (ActivityManager.AppTask t : appTasks) {
                    ActivityManager.RecentTaskInfo info = t.getTaskInfo();
                    if (info != null && info.baseIntent != null && info.baseIntent.getComponent() != null) {
                        if (MainActivity.class.getName().equals(info.baseIntent.getComponent().getClassName())) {
                            // Move existing task to front
                            try {
                                t.moveToFront();
                                isActivityRunning = true;
                                break;
                            } catch (Exception e) {
                                Log.w(TAG, "moveToFront failed: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getAppTasks failed: " + e.getMessage());
        }

        // If not running, decide whether to start it
        if (!isActivityRunning && (forceStartActivity || (systemStatus.Preferences().autoStartActivity() && systemStatus.Preferences().isInKioskMode && !systemStatus.Preferences().isAdmin()))) {
             Intent intent = new Intent(this, MainActivity.class);
             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             startActivity(intent);
         }
         return isActivityRunning;
     }
 }
