package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
/**
 * Write raw audio to disk
 */

public class StageAudioWrite extends Stage {

    final static String LOG = "StageConsumer";

    AudioFileIO io;
    DataOutputStream stream;

    DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    public StageAudioWrite(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start() {

        io = new AudioFileIO("cache_" + timeFormat.format(Stage.startTime));

        stream = io.openDataOutStream(
                samplingrate,
                channels,
                16,
                true);

        super.start();
    }


    void rebuffer() {

        // we do not want rebuffering in a writer stage, just get the data and and pass it on.

        boolean abort = false;

        Log.d(LOG, "----------> " + id + ": Start processing");

        while (!Thread.currentThread().isInterrupted() & !abort) {

            float[][] data = receive();

            if (data != null) {

                process(data);

            } else {
                abort = true;
            }
        }

        io.closeDataOutStream();

        Log.d(LOG, id + ": Stopped consuming");
    }


    @Override
    protected void process(float[][] buffer) {

        byte[] dataOut = new byte[buffer.length * buffer[0].length * 2];

        for (int i = 0; i < buffer[0].length; i++) {

            short tmp = (short) buffer[0][i];
            dataOut[i * 4] = (byte) (tmp & 0xff);
            dataOut[i * 4 + 1] = (byte) ((tmp >> 8) & 0xff);

            tmp = (short) buffer[1][i];
            dataOut[i * 4 + 2] = (byte) (tmp & 0xff);
            dataOut[i * 4 + 3] = (byte) ((tmp >> 8) & 0xff);

        }

        try {
            stream.write(dataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
