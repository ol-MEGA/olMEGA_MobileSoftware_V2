package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import java.util.HashMap;


/**
 * Test Data Producer
 */

public class StageTestData extends Stage {

    final static String LOG = "StageProducer";

    private int channels, frames;
    private boolean stopProducing = false;


    public StageTestData(HashMap parameters) {
        super(parameters);

        hasInput = false;

        channels = 2;
        frames = 10;

    }

    @Override
    protected void process(float[][] temp) {

        int samplesRead, idx = 0;
        short[] buffer = new short[frames * channels];
        float[][] dataOut = new float[channels][frames];

        for (int i = 0; i < frames * 2; i++) {
            buffer[i] = 0;
        }

        Log.d(LOG, "Started producing");

        while (!stopProducing & !Thread.currentThread().isInterrupted()) {

            // create data
            for (int i = 0; i < frames * 2; i++) {
                buffer[i] += 1;
            }

            for (int k = 0; k < frames; k++) {
                // split channels
                dataOut[0][idx] = buffer[k * 2];
                dataOut[1][idx] = buffer[k * 2 + 1];
            }

            send(dataOut);
            dataOut = new float[channels][frames];

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        Log.d(LOG, "Stopped producing");

    }


    public void setStopProducing() {

        stopProducing = true;

    }

}
