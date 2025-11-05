package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Feature extraction: Estimate SNR based on segments with and without voice.
 * Uses output of StageProcVAD.
 */
public class StageProcSNR extends Stage {

    final static String LOG = "StageProcSNR";
    private SNR snr;

    // === Feature flags ===
    private boolean ENABLE_SMOOTHING = true;
    private boolean ENABLE_SOFT_VAD = true;
    private boolean ENABLE_LOOKBACK = false;
    private boolean ENABLE_NOISE_SMOOTH = true;

    private float TAU_SPEECH = 0.05f;   // 50 ms
    private float TAU_NOISE  = 0.25f;    // 250 ms

    private int LOOKBACK_FRAMES = 2;
    private int VAD_SMOOTH_WINDOW = 3;
    private LinkedList<Float> vadHistory = new LinkedList<>();

    public StageProcSNR(HashMap parameter) {
        super(parameter);
    }

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

        private float[][] snr_value = new float[1][1];
        private float rms_speech = 0.0f;
        private float rms_noise = 0.0f;

        private LinkedList<Boolean> vadBuffer = new LinkedList<>();

        private final float FRAME_TIME;
        private final float ALPHA_SPEECH;
        private final float ALPHA_NOISE;

        SNR() {
            snr_value[0][0] = 0.0f;

            FRAME_TIME = (float) blockSize / (float) samplingrate;

            ALPHA_SPEECH = (float) Math.exp(-FRAME_TIME / TAU_SPEECH);
            ALPHA_NOISE  = (float) Math.exp(-FRAME_TIME / TAU_NOISE);

            Log.d(LOG, String.format("SNR stage initialized with frame_time=%.3f s, α_s=%.4f, α_n=%.4f",
                    FRAME_TIME, ALPHA_SPEECH, ALPHA_NOISE));
        }

        void calculate(float[][] input) {

            float vadRaw = input[input.length - 1][0];
            boolean isSpeech = vadRaw >= 0.5f;

            // optional soft-VAD
            float vadSoft = updateSoftVAD(vadRaw);

            // optional lookback correction
            if (ENABLE_LOOKBACK) updateVADBuffer(isSpeech);
            if (ENABLE_LOOKBACK && shouldMarkLookback()) {
                isSpeech = true;
            }

            float newRMS = rms(input[0]);

            if (ENABLE_SMOOTHING) {
                if (isSpeech) {
                    rms_speech = ALPHA_SPEECH * rms_speech + (1 - ALPHA_SPEECH) * newRMS;
                } else {
                    if (ENABLE_NOISE_SMOOTH) {
                        rms_noise = ALPHA_NOISE * rms_noise + (1 - ALPHA_NOISE) * newRMS;
                    } else {
                        rms_noise = newRMS;
                    }
                }
            } else {
                if (isSpeech) rms_speech = newRMS;
                else rms_noise = newRMS;
            }

            snr_value[0][0] = rms_speech / (rms_noise + 1e-6f);

            float[][] tmp_out = new float[1][3];
            tmp_out[0][0] = snr_value[0][0];
            tmp_out[0][1] = rms_speech;
            tmp_out[0][2] = rms_noise;
            send(tmp_out);
        }

        private float updateSoftVAD(float vadRaw) {
            if (!ENABLE_SOFT_VAD) return vadRaw;
            vadHistory.add(vadRaw);
            if (vadHistory.size() > VAD_SMOOTH_WINDOW) vadHistory.removeFirst();
            float sum = 0f;
            for (float v : vadHistory) sum += v;
            return sum / vadHistory.size();
        }

        private void updateVADBuffer(boolean isSpeech) {
            vadBuffer.add(isSpeech);
            if (vadBuffer.size() > LOOKBACK_FRAMES + 1)
                vadBuffer.removeFirst();
        }

        private boolean shouldMarkLookback() {
            if (vadBuffer.size() < LOOKBACK_FRAMES + 1) return false;
            boolean last = vadBuffer.getLast();
            if (!last) return false;
            int trueCount = 0;
            for (int i = vadBuffer.size() - 1; i >= Math.max(0, vadBuffer.size() - 1 - LOOKBACK_FRAMES); i--) {
                if (vadBuffer.get(i)) trueCount++;
            }
            return trueCount > 0;
        }
    }

    protected float rms(float[] data) {
        float temp = 0;
        for (float sample : data) temp += sample * sample;
        temp /= data.length;
        return (float) Math.sqrt(temp);
    }
}
