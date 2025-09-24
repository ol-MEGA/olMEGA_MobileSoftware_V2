package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.HashMap;

/**
 * Read audio file for testing and debugging. Expects 16 kHz, stereo wav file.
 */

public class StageReadAudioFile extends Stage {

    final static String LOG = "StageProducer";

    private int channels, frames;
    InputStream stream;
    private boolean stopProducing = false;


    public StageReadAudioFile(HashMap parameters) {
        super(parameters);

        hasInput = false;

        channels = 2;
        frames = 1024;
    }

    @Override
    void start() {

        AssetManager assetManager = context.getAssets();
        try {
            stream = assetManager.open("cache_20250904_111131920_kueche.wav");
            // skip wav header (44 bytes)
            stream.skip(44);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        super.start();
    }

    @Override
    protected void process(float[][] temp) {


        int bytesRead, idx = 0;
        byte[] byteArray = new byte[frames * channels * 2];
        float[][] dataOut = new float[channels][frames];

        Log.d(LOG, "Started producing");

        //Stage.startTime = Instant.now();

        while (!stopProducing & !Thread.currentThread().isInterrupted()) {

            // read data from audio file
            try {
                bytesRead = stream.read(byteArray);
                if (bytesRead == -1) break; // EOF

                // Create a ByteBuffer only for the valid bytes read
                ByteBuffer buffer = ByteBuffer.wrap(byteArray, 0, bytesRead);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.position(0);

                for (int k = 0; k < bytesRead / 4; k++) {
                    dataOut[0][k] = (float) buffer.getShort() / 32768.0f;
                    dataOut[1][k] = (float) buffer.getShort() / 32768.0f;
                }

                if (Stage.startTime == null)
                    Stage.startTime = Instant.now().minusMillis((long)((float)frames / (float)samplingrate * 1000.0));

                send(dataOut);

                // short breaks to prevent memory issues with feeding the
                // processing chain too quickly.
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                dataOut = new float[channels][frames];

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Log.d(LOG, "Stopped producing");
    }



    public void setStopProducing() {

        stopProducing = true;
    }

}
