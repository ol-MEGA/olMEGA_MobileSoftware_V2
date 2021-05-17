package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;
import java.util.HashMap;

/**
 * Feature extraction: Zero crossing rate of signal and derivative of the signal
 */

public class StageProcZCR extends Stage {

    final static String LOG = "StageProcRMS";


    public StageProcZCR(HashMap parameter) {
        super(parameter);
    }


    @Override
    protected void process(float buffer[][]) {

        float[][] dataOut = new float[buffer.length][2];

        for (int i = 0; i < buffer.length; i++) {
            dataOut[i][0] = zcr(buffer[i]);
            dataOut[i][1] = zcr(diff(buffer[i]));
        }

        send(dataOut);
    }

    protected int zcr(float in[]) {
        int count = 0;
        float data_sign[] = new float[in.length];

        data_sign[0] = Math.signum(in[0]);

        for (int kk = 1; kk < in.length; kk++) {
            data_sign[kk] = Math.signum(in[kk]);

            if (data_sign[kk] - data_sign[kk - 1] != 0)
                count++;
        }

        return count;
    }

    protected float[] diff(float[] data) {
        float[] delta = new float[data.length - 1];

        for (int kk = 0; kk < data.length - 1; kk++)
            delta[kk] = data[kk + 1] - data[kk];

        return delta;
    }

}
