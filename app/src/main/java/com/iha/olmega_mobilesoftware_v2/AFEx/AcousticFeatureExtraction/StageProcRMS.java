package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

/**
 * Feature extraction: RMS
 */

public class StageProcRMS extends Stage {

    final static String LOG = "StageProcRMS";

    public StageProcRMS(HashMap parameter) {
        super(parameter);
    }


    @Override
    protected void process(float[][] buffer) {

        float[][] dataOut = new float[buffer.length][1];

        for (int i = 0; i < buffer.length; i++) {
            dataOut[i][0] = rms(buffer[i]);
        }

        send(dataOut);
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
