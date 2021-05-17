package com.iha.olmega_mobilesoftware_v2;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        //Toast.makeText(context, "Device admin enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Warning";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        //Toast.makeText(context, "Device admin disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLockTaskModeEntering(Context context, Intent intent, String pkg) {
        //Toast.makeText(context, "Kiosk mode enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        //Toast.makeText(context, "Kiosk mode disabled", Toast.LENGTH_SHORT).show();
    }
}