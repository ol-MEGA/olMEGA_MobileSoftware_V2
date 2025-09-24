package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.Manifest;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.time.Instant;
import java.util.HashMap;

/**
 * Capture audio using Android's AudioRecorder (USB preferred)
 */
public class StageUSBCapture extends Stage {

    final static String LOG = "StageProducer";

    private AudioRecord audioRecord;
    private int buffersize, blocksize_ms, frames;
    private boolean stopRecording = false;

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public StageUSBCapture(HashMap parameter) {
        super(parameter);

        Log.d(LOG, "Setting up audioCapture");

        hasInput = false;

        blocksize_ms = 25;
        frames = blocksize_ms * samplingrate / 100;

        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;

        buffersize = AudioRecord.getMinBufferSize(
                samplingrate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT
        ) * 4;

        Log.d(LOG, "Buffersize: " + buffersize);

        try {
            // Try to find a USB device
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
            AudioDeviceInfo usbDevice = null;

            for (AudioDeviceInfo device : inputDevices) {
                if (device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE ||
                        device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    usbDevice = device;
                    Log.d(LOG, "USB device found: " + device.getProductName());
                    break;
                }
            }

            if (usbDevice != null) {
                AudioFormat format = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(samplingrate)
                        .setChannelMask(channelConfig)
                        .build();

                audioRecord = new AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(buffersize)
                        .build();

                // Set the USB device after the AudioRecord is built
                audioRecord.setPreferredDevice(usbDevice);
            }
        } catch (Exception e) {
            Log.e(LOG, "Failed to initialize USB AudioRecord, falling back. Error: " + e.getMessage());
        }

        if (audioRecord == null) {
            Log.d(LOG, "Using default audio input.");
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.UNPROCESSED,
                    samplingrate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffersize
            );
        }

    }

    @Override
    protected void process(float[][] temp) {

        int samplesRead, i = 0;
        short[] buffer = new short[buffersize / 2];
        float[][] dataOut = new float[channels][frames];

        audioRecord.startRecording();

        Log.d(LOG, "Routed device: " + audioRecord.getRoutedDevice().getProductName());
        // TODO:
        // sendMessage("AUDIO_DEVICE_SELECTED", (String) audioRecord.getRoutedDevice().getProductName());
        Log.d(LOG, "Started producing");

        //Stage.startTime = Instant.now();

        while (!stopRecording && !Thread.currentThread().isInterrupted()) {

            samplesRead = audioRecord.read(buffer, 0, buffer.length);

            for (int k = 0; k < samplesRead / 2; k++) {

                dataOut[0][i] = (float) buffer[k * 2] / Short.MAX_VALUE;
                dataOut[1][i] = (float) buffer[k * 2 + 1] / Short.MAX_VALUE;
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
    }

    public void stop() {
        stopRecording = true;
    }
}