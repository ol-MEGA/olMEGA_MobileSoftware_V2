package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.content.Intent;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Feature extraction: Estimate SNR based on segments with and without voice.
 * Uses output of StageProcVAD.
 */
public class StageProcSNR extends Stage {

    final static String LOG = "StageProcSNR";

    private SNR snr;

    private boolean ENABLE_SMOOTHING = true;
    private boolean ENABLE_LOOKBACK = true;

    private float TAU_SPEECH = 0.05f;
    private float TAU_NOISE  = 0.05f;
    private float TAU_SPEECH_WRITE = 0.125f; // to ensure privacy when writing speech RMS.

    private int LOOKBACK_FRAMES = 2;

    // Event Parameter
    private float EVENT_WINDOW_SEC = 600f; // seconds
    private float EVENT_RMS_THRESHOLD = -40.0f; // dB FS
    private float EVENT_SNR_THRESHOLD = 15f; // dB SNR
    private float EVENT_VAD_RATIO = 0.7f;

    public StageProcSNR(HashMap parameter) { super(parameter); }

    @Override
    void start() {
        snr = new SNR();
        super.start();
    }

    @Override
    void rebuffer() {
        boolean abort = false;
        Log.d(LOG, "----------> " + id + ": Start processing");
        while (!Thread.currentThread().isInterrupted() & !abort) {
            if (hasInQueue()) {
                float[][] data = receive();
                if (data != null) {
                    process(data);
                } else {
                    abort = true;
                }
            }
        }
        Log.d(LOG, id + ": Stopped consuming");
    }

    @Override
    protected void process(float[][] buffer) {
        snr.calculate(buffer);
    }

    private class SNR {

        private float last_event = 0f; // time since last event in s
        private float event_interval = 2; // check for event every interval s
        private float[][] snr_value = new float[1][1];
        private float rms_speech = 0f;
        private float rms_noise = 0f;

        private LinkedList<Boolean> vadBuffer = new LinkedList<>();

        private final float FRAME_TIME;
        private final float ALPHA_SPEECH;
        private final float ALPHA_SPEECH_WRITE;
        private final float ALPHA_NOISE;

        private EventDetection eventDetection;

        SNR() {
            FRAME_TIME = (float) blockSize / (float) samplingrate;
            ALPHA_SPEECH = (float) Math.exp(-FRAME_TIME / TAU_SPEECH);
            ALPHA_SPEECH_WRITE = (float) Math.exp(-FRAME_TIME / TAU_SPEECH_WRITE);
            ALPHA_NOISE  = (float) Math.exp(-FRAME_TIME / TAU_NOISE);

            eventDetection = new EventDetection(FRAME_TIME);
        }

        void calculate(float[][] input) {

            float vadRaw = input[input.length - 1][0];
            boolean isSpeech = vadRaw >= 0.5f;

            // Lookback
            if (ENABLE_LOOKBACK) {
                updateVADBuffer(isSpeech);
                if (shouldMarkLookback())
                    isSpeech = true;
            }

            float newRMS = rms(input[0]);

            // --- Glättung ---
            if (ENABLE_SMOOTHING) {
                if (isSpeech) {
                    rms_speech = ALPHA_SPEECH * rms_speech + (1 - ALPHA_SPEECH) * newRMS;
                } else {
                    rms_noise = ALPHA_NOISE * rms_noise + (1 - ALPHA_NOISE) * newRMS;
                }
            } else {
                if (isSpeech) rms_speech = newRMS;
                else rms_noise = newRMS;
            }

            if (isSpeech) {
                snr_value[0][0] = rms_speech / Math.max(rms_noise, 1e-9f);
            }

            eventDetection.update(newRMS, snr_value[0][0], isSpeech);

            // check for event every event_interval
            last_event += FRAME_TIME;
            if (last_event >= event_interval) {
                if (eventDetection.event()) {
                    Intent intent = new Intent("QuestionnaireEvent");
                    intent.setPackage(context.getPackageName());
                    intent.putExtra("Value", true);
                    context.sendBroadcast(intent);
                    LogIHAB.log("Event: Questionnaire triggered.");
                    Log.i(LOG, "EVENT TRIGGERED!");
                }
                // reset
                last_event = 0f;
            }

            float[][] out = new float[1][3];
            out[0][0] = snr_value[0][0];
            out[0][1] = ALPHA_SPEECH_WRITE * rms_speech + (1 - ALPHA_SPEECH_WRITE) * newRMS;
            out[0][2] = rms_noise;
            send(out);
        }

        private void updateVADBuffer(boolean isSpeech) {
            vadBuffer.add(isSpeech);
            if (vadBuffer.size() > LOOKBACK_FRAMES + 1)
                vadBuffer.removeFirst();
        }

        private boolean shouldMarkLookback() {
            if (vadBuffer.size() < LOOKBACK_FRAMES + 1) return false;
            if (!vadBuffer.getLast()) return false;

            for (int i = vadBuffer.size() - 2;
                 i >= vadBuffer.size() - 1 - LOOKBACK_FRAMES; i--) {
                if (i >= 0 && vadBuffer.get(i)) return true;
            }
            return false;
        }
    }

    class EventDetection {

        private float FRAMES;

        private ArrayDeque<Float> win_rms_db = new ArrayDeque<>();
        private ArrayDeque<Float> win_snr_db = new ArrayDeque<>();
        private ArrayDeque<Float> win_vad = new ArrayDeque<>();

        EventDetection(float frameTime) {
            this.FRAMES = (int) (EVENT_WINDOW_SEC / frameTime);
        }

        void update(float rms, float snr, boolean isSpeech) {

            // RMS window
            float rms_db = 20f * (float) Math.log10(Math.max(1e-9f, rms));
            win_rms_db.addLast(rms_db);
            if (win_rms_db.size() > FRAMES) win_rms_db.removeFirst();

            // SNR window
            float snr_db = 20f * (float) Math.log10(Math.max(1e-9f, snr));
            win_snr_db.addLast(snr_db);
            if (win_snr_db.size() > FRAMES) win_snr_db.removeFirst();

            // VAD window
            win_vad.addLast(isSpeech ? 1f : 0f);
            if (win_vad.size() > FRAMES) win_vad.removeFirst();
        }

        boolean event() {
            // Quantiles
            float rms_q05 = getQuantile(win_rms_db, 0.05f);
            float snr_q95 = getQuantile(win_snr_db, 0.95f);

            // VAD-Ratio
            float sum_vad = 0;
            for (float v : win_vad) sum_vad += v;
            float ratio_vad = sum_vad / win_vad.size();

            //Log.d(LOG, "EVENT DETECTION: SNR: " + snr_q95 + " RMS: " + rms_q05 + " VAD: " + ratio_vad);

            return (rms_q05 >= EVENT_RMS_THRESHOLD) &&
                    (snr_q95 <= EVENT_SNR_THRESHOLD) &&
                    (ratio_vad >= EVENT_VAD_RATIO);
        }

        private float getQuantile(ArrayDeque<Float> win, float q) {
            if (win.isEmpty()) return 0f;

            ArrayList<Float> sorted = new ArrayList<>(win);
            Collections.sort(sorted);

            int idx = Math.max(0, Math.min(sorted.size() - 1,
                    (int) (q * (sorted.size() - 1))));

            return sorted.get(idx);
        }
    }

    protected float rms(float[] data) {
        float sum = 0;
        for (float s : data) sum += s * s;
        return (float) Math.sqrt(sum / data.length);
    }
}
