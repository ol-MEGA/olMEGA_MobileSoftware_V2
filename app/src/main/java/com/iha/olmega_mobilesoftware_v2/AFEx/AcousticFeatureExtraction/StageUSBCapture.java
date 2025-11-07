package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.States;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;

/**
 * Capture audio using Android's AudioRecorder (USB preferred)
 */
public class StageUSBCapture extends Stage implements USBDeviceMonitor.Listener {

    final static String LOG = "StageProducer";
    private USBDeviceMonitor usbMonitor;
    private AudioRecord audioRecord;
    private int buffersize, blocksize_ms, frames;
    boolean startup;
    private boolean stopRecording = false;
    private boolean deviceConnected = false;
    private boolean isStartingOrStopping = false;
    private final Object lock = new Object();
    final private String DEVICE_NAME = "Sennheiser XS LAV USB-C";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onTargetDeviceConnected() {
        syncStart();
    }

    @Override
    public void onTargetDeviceDisconnected() {
        syncStop();
    }

    private void syncStart() {
        synchronized (lock) {
            if (isStartingOrStopping) return;
            isStartingOrStopping = true;
        }

        try {
            if (!deviceConnected) {
                Log.d(LOG, "USB device connected");
                sendBroadcast(States.connected);
                deviceConnected = true;
                start();
            }
        } finally {
            synchronized (lock) {
                isStartingOrStopping = false;
            }
        }
    }

    private void syncStop() {
        synchronized (lock) {
            if (isStartingOrStopping) return;
            isStartingOrStopping = true;
        }

        try {
            if (deviceConnected) {
                Log.d(LOG, "USB device disconnected");
                sendBroadcast(States.usb_no_device);
                deviceConnected = false;
                stop();
            }
        } finally {
            synchronized (lock) {
                isStartingOrStopping = false;
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public StageUSBCapture(HashMap parameter) {
        super(parameter);
        usbMonitor = new USBDeviceMonitor(context, DEVICE_NAME, this);
        usbMonitor.start();
        startup = true;
    }

    @Override
    public void start() {
        Log.d(LOG, "Setting up audioCapture");

        hasInput = false;

        blocksize_ms = 25;
        frames = blocksize_ms * samplingrate / 100;

        int channelConfig = (channels == 1)
                ? AudioFormat.CHANNEL_IN_MONO
                : AudioFormat.CHANNEL_IN_STEREO;

        buffersize = AudioRecord.getMinBufferSize(
                samplingrate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT
        ) * 4;

        Log.d(LOG, "Buffersize: " + buffersize);

        AudioDeviceInfo usbDevice = null;

        try {
            // Try to find a USB device
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

            for (AudioDeviceInfo device : inputDevices) {
                if (device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE ||
                        device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    usbDevice = device;
                    Log.d(LOG, "USB device found: " + device.getProductName());
                    LogIHAB.log("USB device found: " + device.getProductName());
                    break;
                }
            }

            if (usbDevice != null && usbDevice.getProductName().toString().contains(DEVICE_NAME)) {
                AudioFormat format = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(samplingrate)
                        .setChannelMask(channelConfig)
                        .build();

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                audioRecord = new AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(buffersize)
                        .build();

                // Set the USB device after the AudioRecord is built
                audioRecord.setPreferredDevice(usbDevice);
                sendBroadcast(States.connected);
                deviceConnected = true;
                super.start();
            } else {
                sendBroadcast(States.usb_no_device);
                Log.e(LOG, "No suitable USB audio device found. Looking for: " + DEVICE_NAME);

            }
        } catch (Exception e) {
           if (usbDevice != null && usbDevice.getProductName().toString().contains(DEVICE_NAME) && e.getMessage().contains("temporal")) {
                Log.d(LOG, "---------------> USB device present, but : " + e.getMessage());
                sendBroadcast(States.connected);
                deviceConnected = true;
            } else {
                sendBroadcast(States.usb_no_device);
                Log.e(LOG, "Failed to initialize USB AudioRecord. Error: " + e.getMessage());
                if (startup) {
                    startup = false;
                    mainHandler.postDelayed(() -> syncStart(), 500);
                }
            }
        }
    }

    @Override
    protected void process(float[][] temp) {

        if (!deviceConnected) {
            return;
        }

        int samplesRead, framesRead, i = 0;
        short[] buffer = new short[buffersize / 2];
        float[][] dataOut = new float[channels][frames];

        audioRecord.startRecording();

        Log.d(LOG, "Routed device: " + audioRecord.getRoutedDevice().getProductName());
        Log.d(LOG, "Started producing");
        sendBroadcast(States.connected);

        // start all consumers!
//        for (Stage consumer : consumerSet) {
//            consumer.start();
//        }

        while (!stopRecording && !Thread.currentThread().isInterrupted()) {

            // check if device is sill connected
//            if (!audioRecord.getRoutedDevice().getProductName().toString().contains(DEVICE_NAME)) {
//                sendBroadcast(States.usb_no_device);
//                deviceConnected = false;
//                Log.e(LOG, "USB audio device disconnected");
//                try {
//                    Thread.sleep(250);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    continue;
//                }
//                continue;
//            }

            samplesRead = audioRecord.read(buffer, 0, buffer.length);
            framesRead = samplesRead / channels;

            for (int k = 0; k < framesRead; k++) {

                if (channels == 1) {
                    dataOut[0][i] = buffer[k] / (float) Short.MAX_VALUE;
                } else {
                    dataOut[0][i] = buffer[k * 2] / (float) Short.MAX_VALUE;
                    dataOut[1][i] = buffer[k * 2 + 1] / (float) Short.MAX_VALUE;
                }

                i++;

                if (i >= frames) {
                    if (Stage.startTime == null)
                        Stage.startTime = Instant.now().minusMillis((long)((float)frames / (float)samplingrate * 1000.0));
                    send(dataOut);
                    dataOut = new float[channels][frames];
                    i = 0;
                }
            }
        }

        Log.d(LOG, "Stopped producing");
        audioRecord.stop();
        stopRecording = false;
        Stage.startTime = null;
    }

    public void stop() {
        stopRecording = true;
    }

    @Override
    public void cleanup() {
        if (usbMonitor != null) {
            usbMonitor.stop();
            usbMonitor = null;
        }
        super.cleanup();
    }

    private void sendBroadcast(States state) {
        switch (state) {
            case init:
                LogIHAB.log("USB: initializing");
                break;
            case connecting:
                LogIHAB.log("USB: connecting");
                break;
            case connected:
                LogIHAB.log("USB: connected");
                break;
            case usb_no_device:
                LogIHAB.log("USB: no device found");
                break;
            default:
                LogIHAB.log("USB: " + state.name());
        }
        Intent intent = new Intent("StageState");    //action: "msg"
        intent.setPackage(context.getPackageName());
        intent.putExtra("currentState", state.ordinal());
        context.sendBroadcast(intent);
    }

}