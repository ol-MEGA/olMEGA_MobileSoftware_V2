package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import java.util.HashMap;

/**
 * Feature extraction: Smoothed RMS
 */

public class StageProcRMSsmooth extends Stage {

    final static String LOG = "StageProcRMSsmooth";
    private final float tau = 0.125f;
    private final float alpha;
    private final int blocks_tau;
    private int block_counter;
    private float[][] smooth_rms;
    boolean initialized;

    public StageProcRMSsmooth(HashMap parameter) {
        super(parameter);

        // params for exp. smoothing
        alpha = (float) Math.exp(-hopSize / (samplingrate * tau));
        blocks_tau = Math.max(1, (int) Math.round((tau * samplingrate) / hopSize));

        smooth_rms = new float[channels][1];
    }


    @Override
    void start() {
        block_counter = 0;
        for (int ch = 0; ch < channels; ch++) {
            smooth_rms[ch][0] = 0f;
        }
        initialized = false;
        super.start();
    }

    @Override
    protected void process(float[][] buffer) {

        float[][] current_rms = new float[channels][1];
        for (int i = 0; i < buffer.length; i++) {
            current_rms[i][0] = rms(buffer[i]);
        }

        if (!initialized) {
            // first RMS
            for (int ch = 0; ch < channels; ch++) {
                smooth_rms[ch][0] = current_rms[ch][0];
                initialized = true;
            }
        } else {
            for (int ch = 0; ch < channels; ch++) {
                smooth_rms[ch][0] = (float) (alpha * current_rms[ch][0] + (1 - alpha) * smooth_rms[ch][0]);
            }
        }

        block_counter++;
        if (block_counter >= blocks_tau) {
            float[][] dataOut = new float[smooth_rms.length][1];
            for (int ch = 0; ch < channels; ch++) {
                dataOut[ch][0] = smooth_rms[ch][0];
            }
            send(dataOut);
            block_counter = 0;
        }
    }

    protected float rms(float[] data) {

        float temp = 0;

        for (float sample : data) {
            temp += sample * sample;
        }
        temp /= data.length;
        return (float) Math.sqrt(temp);

    }

}