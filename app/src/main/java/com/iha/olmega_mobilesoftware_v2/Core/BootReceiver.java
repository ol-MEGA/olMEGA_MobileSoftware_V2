package com.iha.olmega_mobilesoftware_v2.Core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.MainActivity;

public class BootReceiver extends BroadcastReceiver {
    private String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent _intent) {
        Log.d(TAG, "onReceive");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean autostart = sharedPreferences.getBoolean("autoStartActivity", true);
        boolean isAdmin = sharedPreferences.getBoolean("isAdmin", false);
        if (_intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) && autostart && !isAdmin) {
            Log.d(TAG, "onReceive: start Intent");
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}