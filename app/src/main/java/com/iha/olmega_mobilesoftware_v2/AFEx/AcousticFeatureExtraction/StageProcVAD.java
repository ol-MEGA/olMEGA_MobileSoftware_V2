package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;
import com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Utilities;

import com.konovalov.vad.silero.Vad;
import com.konovalov.vad.silero.VadSilero;
import com.konovalov.vad.silero.config.FrameSize;
import com.konovalov.vad.silero.config.Mode;
import com.konovalov.vad.silero.config.SampleRate;

import java.util.HashMap;

/**
 * Feature: Voice Activity Detection using Silero
 * https://github.com/gkonovalov/android-vad/
 */

public class StageProcVAD extends Stage {

    final static String LOG = "StageProcVAD";
    private VadSilero vad;

    // passthrough sends the audio data along with the corresponding VAD results to enable
    // conditional processing in attached stages. VAD data is a single value for each audio channel
    // in the last channel of the output array, i.e. the first value corresponds to the 1st channel.
    final static boolean passthrough = false;

    public StageProcVAD(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start(){
        vad = Vad.builder()
                .setContext(context)
                .setSampleRate(SampleRate.SAMPLE_RATE_16K)
                .setFrameSize(FrameSize.FRAME_SIZE_512)
                .setMode(Mode.NORMAL)
                .setSilenceDurationMs(10)
                .setSpeechDurationMs(50)
                .build();

        super.start();
    }

    @Override
    protected void process(float[][] buffer) {

        // Normalise to [-1, 1], apparently this isn't done in Silero   ...
        Utilities.normalise(buffer);

        int outchannels = 1;
        if (passthrough) {
            outchannels += buffer.length;
        }
        float[][] dataOut = new float[outchannels][]; // VAD data
        dataOut[outchannels-1] = new float[1];
        for (int channel = 0; channel < 1; channel++) {
            boolean isSpeech = vad.isSpeech(buffer[channel]);
            //sendMessage("VAD", String.valueOf(isSpeech));
            dataOut[outchannels-1][channel] = isSpeech ? 1.0f : 0.0f;
            if (passthrough) {
                dataOut[channel] = new float[buffer[channel].length];
                System.arraycopy(buffer[channel], 0, dataOut[channel], 0, buffer[channel].length);
            }
        }

        send(dataOut);
    }

    @Override
    protected void cleanup() {
        vad.close();
    }

}
