package com.iha.olmega_mobilesoftware_v2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO;

import java.io.DataOutputStream;
import java.io.IOException;
import android.media.MediaPlayer;
import android.os.SystemClock;
import java.util.Locale;
import android.app.AlertDialog;
import java.io.File;

public class ReferenceCaptureActivity extends Activity {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusView;
    private TextView timerView;
    private Button recordButton;
    private Button playButton;
    private Button deleteButton;
    private final String DEVICE_NAME = "Sennheiser XS LAV USB-C";

    private volatile boolean isRecording = false;
    private Thread recordThread = null;
    private long recordStartTimeMs = 0;
    private Runnable timerRunnable;
    private String lastSavedPath = null;
    private MediaPlayer mediaPlayer = null;
    private Runnable playbackUpdateRunnable = null;
    private int playbackTotalMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_capture);
        int statusId = R.id.rc_status;
        int recordBtnId = R.id.rc_record_button;
        int timerId = R.id.rc_timer;
        int playBtnId = R.id.rc_play_button;
        int deleteBtnId = R.id.rc_delete_button;
        statusView = findViewById(statusId);
        timerView = findViewById(timerId);
        recordButton = findViewById(recordBtnId);
        playButton = findViewById(playBtnId);
        deleteButton = findViewById(deleteBtnId);
        playButton.setText("Play");

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                AudioDeviceInfo usbDevice = findUsbDeviceIfPresent();
                if (usbDevice == null) {
                    final String msg = "Required USB device not found: " + DEVICE_NAME;
                    statusView.setText(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    return;
                }

                File refFile = new File(AudioFileIO.getSpeakerIdPath(), "reference.wav");
                if (refFile.exists()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Reference exists")
                            .setMessage("reference.wav already exists. Overwrite?")
                            .setPositiveButton("Overwrite", (dialog, which) -> {
                                startRecording(usbDevice, "reference");
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // do nothing
                            })
                            .setCancelable(true)
                            .show();
                } else {
                    startRecording(usbDevice, "reference");
                }

            } else {
                stopRecording();
            }
        });

        playButton.setOnClickListener(v -> {
            if (lastSavedPath == null) {
                Toast.makeText(this, "No reference recorded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mediaPlayer != null) {
                // stop playback
                try {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                } catch (Exception ignored) {}
                try { mediaPlayer.release(); } catch (Exception ignored) {}
                mediaPlayer = null;
                if (playbackUpdateRunnable != null) handler.removeCallbacks(playbackUpdateRunnable);
                playButton.setText("Play");
                String total = playbackTotalMs > 0 ? formatTimeMs(playbackTotalMs) : "00:00";
                timerView.setText("00:00 / " + total);
            } else {
                // start playback
                playButton.setText("Stop");
                playReference(lastSavedPath);
            }
        });

        // delete button handler
        deleteButton.setOnClickListener(v -> {
            if (lastSavedPath == null) {
                Toast.makeText(this, "No reference file to delete", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Delete reference")
                    .setMessage("Delete reference.wav?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        try {
                            File f = new File(lastSavedPath);
                            boolean ok = f.delete();
                            if (ok) {
                                lastSavedPath = null;
                                handler.post(() -> {
                                    playButton.setEnabled(false);
                                    deleteButton.setEnabled(false);
                                    statusView.setText("reference.wav deleted");
                                    timerView.setText("00:00");
                                    Toast.makeText(this, "reference.wav deleted", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                handler.post(() -> Toast.makeText(this, "Failed to delete reference.wav", Toast.LENGTH_LONG).show());
                            }
                        } catch (Exception ex) {
                            handler.post(() -> Toast.makeText(this, "Error deleting file: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsed = SystemClock.elapsedRealtime() - recordStartTimeMs;
                    int seconds = (int) (elapsed / 1000);
                    int mm = seconds / 60;
                    int ss = seconds % 60;
                    timerView.setText(String.format(Locale.US, "%02d:%02d", mm, ss));
                    handler.postDelayed(this, 500);
                }
            }
        };
        checkExistingReference();
    }

    private void startRecording(AudioDeviceInfo usbDevice, String desiredFilenameNoExt) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Record audio permission not granted", Toast.LENGTH_LONG).show();
            return;
        }

        isRecording = true;
        recordButton.setText("Stop");
        statusView.setText("Recording...");
        recordStartTimeMs = SystemClock.elapsedRealtime();
        handler.post(timerRunnable);

        recordThread = new Thread(() -> {
            AudioFileIO audioIO = new AudioFileIO(desiredFilenameNoExt, AudioFileIO.getSpeakerIdPath());
            DataOutputStream dos = null;
            AudioRecord recorder = null;
            try {
                dos = audioIO.openDataOutStream(SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE, true);

                int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        ENCODING);
                if (minBuf <= 0) minBuf = SAMPLE_RATE * 2;

                android.media.AudioFormat format = new android.media.AudioFormat.Builder()
                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build();

                try {
                    recorder = new AudioRecord.Builder()
                            .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                            .setAudioFormat(format)
                            .setBufferSizeInBytes(minBuf)
                            .build();

                    // try to bind to USB device; if this fails or routing does not point to USB device, abort
                    try {
                        recorder.setPreferredDevice(usbDevice);
                    } catch (Exception se) {
                        // cleanup and abort
                        try { recorder.release(); } catch (Exception ignored) {}
                        throw new RuntimeException("Failed to set preferred USB device", se);
                    }

                    // check routed device to ensure the recorder is using the USB device
                    AudioDeviceInfo routed = recorder.getRoutedDevice();
                    if (routed == null || routed.getId() != usbDevice.getId()
                            || routed.getProductName() == null
                            || !routed.getProductName().toString().contains(DEVICE_NAME)) {
                        try { recorder.release(); } catch (Exception ignored) {}
                        throw new RuntimeException("USB device not routed to AudioRecord");
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Cannot record with USB device: " + e.getMessage(), e);
                }

                short[] buffer = new short[Math.max(minBuf / 2, 1024)];

                recorder.startRecording();

                while (isRecording && !Thread.currentThread().isInterrupted()) {
                    int r = recorder.read(buffer, 0, buffer.length);
                    if (r > 0) {
                        for (int i = 0; i < r; i++) {
                            short s = buffer[i];
                            dos.writeByte(s & 0xFF);
                            dos.writeByte((s >> 8) & 0xFF);
                        }
                    }
                }

                try { recorder.stop(); } catch (Exception ignored) {}
                try { recorder.release(); } catch (Exception ignored) {}
                recorder = null;

                audioIO.closeDataOutStream();
                lastSavedPath = audioIO.filename;

                // compute duration from WAV file length and fall back to MediaPlayer if necessary.
                String dateStr = "none";
                String durationFormatted = "none";
                try {
                    File f = new File(lastSavedPath);
                    try {
                        java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        dateStr = df.format(new java.util.Date(f.lastModified()));
                    } catch (Exception ex) { dateStr = "none"; }

                    // WAV file: data bytes = total length - header (44 bytes)
                    long len = f.length();
                    long dataBytes = len - 44;
                    if (dataBytes > 0) {
                        int bytesPerSample = BITS_PER_SAMPLE / 8;
                        long totalSamples = dataBytes / (bytesPerSample * CHANNELS);
                        long durMs = (totalSamples * 1000) / SAMPLE_RATE;
                        if (durMs > 0) durationFormatted = formatTimeMs((int) durMs);
                    }

                    // If length-based failed, try MediaPlayer as fallback
                    if ("none".equals(durationFormatted)) {
                        int durMs = -1;
                        for (int attempt = 0; attempt < 5; attempt++) {
                            MediaPlayer tmp = null;
                            try {
                                tmp = new MediaPlayer();
                                tmp.setDataSource(lastSavedPath);
                                tmp.prepare();
                                durMs = tmp.getDuration();
                                break;
                            } catch (Exception ex) {
                                // wait a bit and retry, file might still be finalizing on some devices
                                try { Thread.sleep(200); } catch (InterruptedException ie) { /* ignore */ }
                            } finally {
                                if (tmp != null) {
                                    try { tmp.release(); } catch (Exception ignored) {}
                                }
                            }
                        }
                        if (durMs > 0) durationFormatted = formatTimeMs(durMs);
                    }
                } catch (Exception ex) {
                }

                final String status = "reference.wav — Datum: " + dateStr + "  Dauer: " + durationFormatted;
                final String timerText = durationFormatted.equals("none") ? "00:00" : durationFormatted;
                handler.post(() -> {
                    lastSavedPath = new File(lastSavedPath).getAbsolutePath();
                    playButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    statusView.setText(status);
                    timerView.setText(timerText);
                    Toast.makeText(this, "Saved: " + lastSavedPath, Toast.LENGTH_LONG).show();
                    recordButton.setText("Record");
                    recordButton.setEnabled(true);
                });

             } catch (Exception e) {
                if (recorder != null) {
                    try { recorder.stop(); } catch (Exception ignored) {}
                    try { recorder.release(); } catch (Exception ignored) {}
                }
                if (dos != null) {
                    try { audioIO.closeDataOutStream(); } catch (Exception ignored) {}
                }
                String userMsg = e.getMessage() != null ? e.getMessage() : e.toString();

                // Ensure recording state/timer is reset
                isRecording = false;
                handler.post(() -> {
                    statusView.setText(userMsg);
                    Toast.makeText(this, userMsg, Toast.LENGTH_LONG).show();
                    recordButton.setText("Record");
                    recordButton.setEnabled(true);
                    handler.removeCallbacks(timerRunnable);
                    timerView.setText("00:00");
                });
             }
         });

         recordThread.start();
    }

    private void stopRecording() {
        isRecording = false;
        recordButton.setEnabled(false);
        handler.removeCallbacks(timerRunnable);
        timerView.setText("00:00");
        // Schedule a short delayed refresh, background thread may still be finalizing the file.
        handler.postDelayed(() -> {
            try { checkExistingReference(); } catch (Exception ignored) {}
        }, 500);
    }

    private void playReference(String path) {
        // stop any previous player and its callbacks
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
            if (playbackUpdateRunnable != null) handler.removeCallbacks(playbackUpdateRunnable);
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            playbackTotalMs = mediaPlayer.getDuration();
            final String totalFormatted = formatTimeMs(playbackTotalMs);
            // show initial 00:00 / TOTAL
            timerView.setText("00:00 / " + totalFormatted);

            // update runnable
            playbackUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int elapsed = mediaPlayer.getCurrentPosition();
                        timerView.setText(formatTimeMs(elapsed) + " / " + totalFormatted);
                        handler.postDelayed(this, 250);
                    }
                }
            };

            mediaPlayer.setOnCompletionListener(mp -> {
                // ensure final display is total/total
                timerView.setText(totalFormatted + " / " + totalFormatted);
                handler.removeCallbacks(playbackUpdateRunnable);
                try { mp.release(); } catch (Exception ignored) {}
                mediaPlayer = null;
                playButton.setText("Play");
            });

            mediaPlayer.start();
            handler.postDelayed(playbackUpdateRunnable, 250);
        } catch (IOException e) {
            if (playbackUpdateRunnable != null) handler.removeCallbacks(playbackUpdateRunnable);
            Toast.makeText(this, "Playback failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            playButton.setText("Play");
        }
    }

    private static String formatTimeMs(int ms) {
        if (ms <= 0) return "00:00";
        int totalSec = ms / 1000;
        int mm = totalSec / 60;
        int ss = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mm, ss);
    }

    private AudioDeviceInfo findUsbDeviceIfPresent() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            Log.e("REFERENCE", "NO AUDIO MANAGER FOUND");
            return null;
        }
        AudioDeviceInfo[] inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        for (AudioDeviceInfo device : inputDevices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                CharSequence pname = device.getProductName();
                if (pname != null) {
                    String pn = pname.toString().trim();
                    if (pn.contains(DEVICE_NAME)) {
                        return device;
                    }
                }
            }
        }
        return null;
    }

    private void checkExistingReference() {
        try {
            File refFile = new File(AudioFileIO.getSpeakerIdPath(), "reference.wav");
            if (refFile.exists()) {
                lastSavedPath = refFile.getAbsolutePath();
                // enable play
                handler.post(() -> playButton.setEnabled(true));

                String dateStr;
                try {
                    java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    dateStr = df.format(new java.util.Date(refFile.lastModified()));
                } catch (Exception e) {
                    dateStr = "none";
                }

                String durationStr = "none";
                MediaPlayer tmp = null;
                try {
                    tmp = new MediaPlayer();
                    tmp.setDataSource(lastSavedPath);
                    tmp.prepare();
                    int durMs = tmp.getDuration();
                    int seconds = durMs / 1000;
                    durationStr = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
                } catch (Exception ignored) {
                    // leave duration unknown
                } finally {
                    if (tmp != null) try { tmp.release(); } catch (Exception ignored) {}
                }

                final String durationForUI = durationStr.equals("none") ? "00:00" : durationStr;
                final String status = "reference.wav — Datum: " + dateStr + "  Dauer: " + durationStr;
                handler.post(() -> {
                    statusView.setText(status);
                    timerView.setText(durationForUI);
                    deleteButton.setEnabled(true);
                });
            } else {
                handler.post(() -> {
                    playButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                    timerView.setText("00:00");
                });
            }
        } catch (Exception e) {
            handler.post(() -> {
                playButton.setEnabled(false);
                timerView.setText("00:00");
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
        }
        if (playbackUpdateRunnable != null) handler.removeCallbacks(playbackUpdateRunnable);
        isRecording = false;
    }

}
