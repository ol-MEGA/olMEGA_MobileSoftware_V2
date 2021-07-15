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
        for (int i = 0; i < buffer[0].length; i++) {
            short tmp = (short) (buffer[0][i] * Short.MAX_VALUE);
            dataOut[i * 4] = (byte) (tmp & 0xff);
            dataOut[i * 4 + 1] = (byte) ((tmp >> 8) & 0xff);
            tmp = (short) (buffer[1][i] * Short.MAX_VALUE);
            dataOut[i * 4 + 2] = (byte) (tmp & 0xff);
            dataOut[i * 4 + 3] = (byte) ((tmp >> 8) & 0xff);
        }
        audioTrack.write(dataOut, 0, dataOut.length, AudioTrack.WRITE_NON_BLOCKING);
    }


    public void stop() {
        audioTrack.stop();
    }

}
