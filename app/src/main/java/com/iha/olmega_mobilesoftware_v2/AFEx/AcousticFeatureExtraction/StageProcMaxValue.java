package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;


import java.util.HashMap;

/**
 * Feature extraction: Maximal value in block
 */

public class StageProcMaxValue extends Stage {

    final static String LOG = "StageProcMaxValue";


    public StageProcMaxValue(HashMap parameter) {
        super(parameter);
    }


    @Override
    protected void process(float[][] buffer) {

        float[][] dataOut = new float[buffer.length][1];

        for (int i = 0; i < buffer.length; i++) {
            dataOut[i][0] = maxValue(buffer[i]);
        }

        send(dataOut);
    }

    protected float maxValue(float[] data) {

        float max = 0;
        float value;

        for (float aData : data) {
            value = Math.abs(aData);
            if (value > max) max = value;
        }

        return max;

    }

}
