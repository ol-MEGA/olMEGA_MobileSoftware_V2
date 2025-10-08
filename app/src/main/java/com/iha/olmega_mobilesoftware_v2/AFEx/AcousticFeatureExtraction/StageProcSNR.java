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
    void start(){
        snr = new SNR();
        super.start();
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
        snr.calculate(buffer);
    }

    private class SNR {

        float[][] snr_value = new float[1][1];
        float rms_speech = 0.0f;
        float rms_noise = 0.0f;

        SNR() {
            snr_value[0][0] = 0.0f;
        }

        void calculate(float[][] input) {
            boolean isSpeech = input[input.length - 1][0] == 1.0f;
            if (isSpeech) { // update speech rms
                rms_speech = rms(input[0]);
            } else {  // update noise rms
                rms_noise = rms(input[0]);
            }
            snr_value[0][0] = rms_speech / (rms_noise + 1e-6f);
            float[][] tmp_out = new float[1][3];
            tmp_out[0][0] = snr_value[0][0];
            tmp_out[0][1] = rms_speech;
            tmp_out[0][2] = rms_noise;
            send(tmp_out);
            //send(snr_value);
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