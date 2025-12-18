package com.iha.olmega_mobilesoftware_v2;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

public class UnlockActivity extends Activity {
    private static final String TAG = "UnlockActivity";
    private ControlService controlService;
    private boolean mIsBound = false;

    private final BroadcastReceiver stageStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            int state = intent.getIntExtra("currentState", -1);
            if (state == -1) return;
            try {
                com.iha.olmega_mobilesoftware_v2.States s = com.iha.olmega_mobilesoftware_v2.States.values()[state];
                Log.d(TAG, "StageState received: " + s);
                if (s == com.iha.olmega_mobilesoftware_v2.States.connected) {
                    // Recording started — restore flags, re-lock device and finish
                    try {
                        // Ensure any showWhenLocked/turnScreenOn flags are cleared before locking
                        try { restoreLockState(); } catch (Exception ignored) {}

                        // Delay the actual lock slightly to allow the flags to take effect and avoid
                        // MainActivity immediately reappearing above the lockscreen.
                        getWindow().getDecorView().postDelayed(() -> {
                            try {
                                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                                if (dpm != null) {
                                    dpm.lockNow();
                                    Log.d(TAG, "DevicePolicyManager.lockNow() called (delayed)");
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "lockNow failed: " + e.getMessage());
                            }
                            // finish activity after attempting to lock
                            finishSelf();
                        }, 200);

                    } catch (Exception e) {
                        Log.w(TAG, "stageStateReceiver error: " + e.getMessage());
                        finishSelf();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "stageStateReceiver error: " + e.getMessage());
            }
        }
    };

    private void restoreLockState() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false);
                setTurnScreenOn(false);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            }
        } catch (Exception ignored) {}
    }

    private void finishSelf() {
        try { restoreLockState(); } catch (Exception ignored) {}
        try {
            unregisterReceiver(stageStateReceiver);
        } catch (Exception ignored) {}
        try { if (mIsBound) { unbindService(mServiceConnection); mIsBound = false; } } catch (Exception ignored) {}
        finish();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            controlService = ((ControlService.LocalBinder) service).getService();
            mIsBound = true;
            try {
                // Bring MainActivity to front which in your app triggers the recording init
                controlService.startMainActivity(true);
            } catch (Exception e) {
                Log.w(TAG, "startMainActivity failed: " + e.getMessage());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            controlService = null;
            mIsBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make activity show on locked screen and turn screen on
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            }

            // Try to dismiss keyguard
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }

            // Acquire a short wake lock to ensure screen stays on during start
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG + ":wake");
                wl.acquire(2000);
                wl.release();
            }
        } catch (Exception e) {
            Log.w(TAG, "onCreate unlock helper failed: " + e.getMessage());
        }

        // Register receiver to wait for StageState.connected
        IntentFilter f = new IntentFilter("StageState");
        registerReceiver(stageStateReceiver, f, Context.RECEIVER_NOT_EXPORTED);

        // Bind to ControlService so we can bring MainActivity to foreground
        try {
            bindService(new Intent(this, ControlService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.w(TAG, "bindService failed: " + e.getMessage());
        }

        // As fallback: also request start of main activity directly
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } catch (Exception e) {
            Log.w(TAG, "startActivity MainActivity failed: " + e.getMessage());
        }

        // If nothing happens within timeout, re-lock and finish
        getWindow().getDecorView().postDelayed(() -> {
            Log.d(TAG, "Timeout expired without connection; finishing and re-locking");
            try {
                try { restoreLockState(); } catch (Exception ignored) {}
                // Delay lock to ensure flags are cleared first
                getWindow().getDecorView().postDelayed(() -> {
                    try {
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        if (dpm != null) dpm.lockNow();
                    } catch (Exception ignored) {}
                    finishSelf();
                }, 200);
            } catch (Exception ignored) {}
        }, 8000);
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(stageStateReceiver); } catch (Exception ignored) {}
        try { if (mIsBound) { unbindService(mServiceConnection); mIsBound = false; } } catch (Exception ignored) {}
        super.onDestroy();
    }
}
