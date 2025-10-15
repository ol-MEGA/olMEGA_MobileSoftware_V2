package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class USBDeviceMonitor {

    public interface Listener {
        void onTargetDeviceConnected();
        void onTargetDeviceDisconnected();
    }

    private static final String LOG = "USBDeviceMonitor";
    private final Context context;
    private final String targetDeviceName;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isRegistered = false;
    private boolean deviceConnected = false;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(LOG, "USB_DEVICE_ATTACHED");
                //checkDevices(true);
                mainHandler.postDelayed(() -> checkDevices(true), 500);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(LOG, "USB_DEVICE_DETACHED");
                handleDisconnect();
            }
        }
    };

    public USBDeviceMonitor(Context context, String targetDeviceName, Listener listener) {
        this.context = context.getApplicationContext();
        this.targetDeviceName = targetDeviceName;
        this.listener = listener;
    }

    public void start() {
        if (isRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbReceiver, filter);
        isRegistered = true;
        checkDevices(false);
    }

    public void stop() {
        if (isRegistered) {
            context.unregisterReceiver(usbReceiver);
            isRegistered = false;
        }
    }

    private void checkDevices(boolean fromBroadcast) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        boolean found = false;

        for (AudioDeviceInfo device : inputs) {
            if (device.getProductName().toString().contains(targetDeviceName)) {
                found = true;
                break;
            }
        }

        if (found && !deviceConnected) {
            deviceConnected = true;
            Log.d(LOG, "Target USB device connected");
            mainHandler.post(() -> listener.onTargetDeviceConnected());
        } else if (!found && deviceConnected) {
            handleDisconnect();
        } else if (fromBroadcast) {
            Log.d(LOG, "Broadcast received but no matching device found");
        }
    }

    private void handleDisconnect() {
        if (deviceConnected) {
            deviceConnected = false;
            Log.d(LOG, "Target USB device disconnected");
            mainHandler.post(() -> listener.onTargetDeviceDisconnected());
        }
    }
}