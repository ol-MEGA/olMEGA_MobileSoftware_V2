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
    final static boolean passthrough = true;

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
                .setSilenceDurationMs(25)
                .setSpeechDurationMs(25)
                .build();

        super.start();
    }

    @Override
    protected void process(float[][] buffer) {

        // copy buffer for normalisation for VAD
        float[][] bufferNorm = new float[buffer.length][];
        for (int i = 0; i < buffer.length; i++) {
            bufferNorm[i] = buffer[i].clone();
        }
        // Normalise data to improve VAD performance
        Utilities.normaliseToDbFS(bufferNorm, -12.0f);

        int outchannels = 1;
        if (passthrough) {
            outchannels += buffer.length;
        }
        float[][] dataOut = new float[outchannels][];
        dataOut[outchannels-1] = new float[1]; // VAD for 1st channel only!
        boolean isSpeech = vad.isSpeech(bufferNorm[0]);
        dataOut[outchannels-1][0] = isSpeech ? 1.0f : 0.0f;
        if (passthrough) {
            for (int channel = 0; channel < channels; channel++) {
                dataOut[channel] = new float[buffer[channel].length];
                dataOut[channel] = buffer[channel].clone();
            }
        }
        send(dataOut);
    }

    @Override
    protected void cleanup() {
        vad.close();
    }

}
