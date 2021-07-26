package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.time.Instant;
import java.util.HashMap;

/**
 * Audio Output
 */

public class StageAudioOutput extends Stage {

    final static String LOG = "StageProducer";

    private AudioTrack audioTrack = null;
    private int buffersize;
    private float[] calibValues = new float[]{1, 1};
    boolean CalibValuesLoaded = false;

    public StageAudioOutput(HashMap parameter) {
        super(parameter);

        Log.d(LOG, "Setting up audioOutput");
        buffersize = AudioRecord.getMinBufferSize(samplingrate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        ) * 20;
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                samplingrate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffersize,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    @Override
    protected void process(float[][] buffer) {
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
        audioTrack.write(dataOut, 0, dataOut.length, AudioTrack.WRITE_NON_BLOCKING);
    }


    public void stop() {
        audioTrack.stop();
    }

}
