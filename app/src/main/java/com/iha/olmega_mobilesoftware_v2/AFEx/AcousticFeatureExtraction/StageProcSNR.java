package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import java.util.HashMap;

/**
 * Feature extraction: Estimate SNR based segments with and without voice.
 * Uses output of StageProcVAD, i.e. throughput must be set accordingly.
 */

public class StageProcSNR extends Stage {

    final static String LOG = "StageProcSNR";
    SNR snr;

    public StageProcSNR(HashMap parameter) {
        super(parameter);
    }

    @Override
    void rebuffer() {
        // we are getting passed through data so we have both, the audio and VAD data. We do not want
        // to rebuffer that (for now), so the blocksize of this stage must match the incoming stage,
        // the VAD (512 samples = 32 ms @ 16 kHz).
        // TODO: implement passthrough into Stage.rebuffer()
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
        SNR snr = new SNR();
        snr.calculate(buffer);
    }

    private class SNR {

        float[][] snr_value = new float[1][1];
        float[] rms_speech = new float[2];
        float[] rms_noise = new float[2];

        SNR() {
            snr_value[0][0] = 0.0f;
            rms_speech[0] = 0.0f;
            rms_speech[1] = 0.0f;
            rms_noise[0] = 0.0f;
            rms_noise[1] = 0.0f;
        }

        void calculate(float[][] input) {
            boolean isSpeech = input[input.length - 1][0] == 1.0f;
            if (isSpeech) { // update speech rms and hold noise
                rms_speech[1] = rms(input[0]);
                rms_noise[1] = rms_noise[0];
            } else {  // update noise rms and hold speech
                rms_noise[1] = rms(input[0]);
                rms_speech[1] = rms_speech[0];
            }
            snr_value[0][0] = rms_speech[1] / (rms_noise[1] + 1e-6f);
            send(snr_value);
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