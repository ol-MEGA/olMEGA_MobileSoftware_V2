package com.iha.olmega_mobilesoftware_v2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Start activity on system startup
 *
 * http://stackoverflow.com/questions/6391902/how-to-start-an-application-on-startup
 */

public class AutostartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent _intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autostart = sharedPreferences.getBoolean("autoStartActivity", true);

        if (_intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) && autostart) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
