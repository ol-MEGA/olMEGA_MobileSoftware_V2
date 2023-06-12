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

    AudioFileIO io = null;
    DataOutputStream stream;
    private float[] calibValues = new float[]{1, 1};
    boolean CalibValuesLoaded = false;

    DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    public StageAudioWrite(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start() {
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

        if (io != null)
            io.closeDataOutStream();

        Log.d(LOG, id + ": Stopped consuming");
    }


    @Override
    protected void process(float[][] buffer) {
        if (io == null && Stage.startTime != null) {
            io = new AudioFileIO("cache_" + timeFormat.format(Stage.startTime));

            stream = io.openDataOutStream(
                    samplingrate,
                    channels,
                    16,
                    true);
        }
        else if (io != null && Stage.startTime != null) {
            byte[] dataOut = new byte[buffer.length * buffer[0].length * 2];
            short tmp;
            if (!CalibValuesLoaded) {
                Stage tempStage = this;
                while (tempStage != null) {
                    tempStage = tempStage.inStage;
                    if (tempStage != null && tempStage.getClass() == StageRFCOMM.class && !Float.isNaN(((StageRFCOMM)tempStage).calibValues[0]) && !Float.isNaN(((StageRFCOMM)tempStage).calibValues[1])) {
                        calibValues = ((StageRFCOMM)tempStage).calibValues.clone();
                        break;
                    }
                }
                CalibValuesLoaded = true;
            }
            for (int i = 0; i < buffer[0].length; i++) {
                tmp = (short) ((buffer[0][i] * (float)Short.MAX_VALUE) / calibValues[0]);
                dataOut[i * 4] = (byte) (tmp & 0xff);
                dataOut[i * 4 + 1] = (byte) ((tmp >> 8) & 0xff);
                tmp = (short) ((buffer[1][i] * (float)Short.MAX_VALUE) / calibValues[1]);
                dataOut[i * 4 + 2] = (byte) (tmp & 0xff);
                dataOut[i * 4 + 3] = (byte) ((tmp >> 8) & 0xff);
            }
            if (WriteDataToStorage) {
                try {
                    stream.write(dataOut);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
