package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult;
import com.google.mediapipe.tasks.audio.core.RunningMode;
import com.google.mediapipe.tasks.components.containers.AudioData;
import com.google.mediapipe.tasks.core.BaseOptions;

import java.util.HashMap;

/**
 * Feature: Audio Classification using yamnet
 * Example configuration:
 * <stage feature="StageProcClassify" id="30" blocksize="15360" hopsize="15360">
 *     <stage feature="StageFeatureWrite" id="31" prefix="CLASS" nfeatures="12"/>
 * </stage>
 */

public class StageProcClassify extends Stage {

    final static String LOG = "StageProcClassify";
    private final AudioClassifier audioClassifier;
    final private AudioData audioData;

    public StageProcClassify(HashMap parameter) {
        super(parameter);

        BaseOptions baseOptions =
                BaseOptions.builder().setModelAssetPath("yamnet.tflite").build();

        AudioClassifier.AudioClassifierOptions options =
            AudioClassifier.AudioClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.AUDIO_CLIPS)
                    .setMaxResults(3)
                    .build();
        ;

        audioClassifier = AudioClassifier.createFromOptions(context, options);

        // input length for yamnet is 0.975 * 16000 = 15600 samples = blockSize
        audioData = AudioData.create(AudioData.AudioDataFormat.builder()
                                .setNumOfChannels(1)
                                .setSampleRate(samplingrate)
                                .build(),
                            blockSize);
    }


    @Override
    protected void process(float[][] buffer) {

        float[][] dataOut = new float[buffer.length][6]; // 3 results/channel, each with id and score interleaved
        //String resultUi = "";

        for (int channel = 0; channel < buffer.length; channel++) {
            audioData.load(buffer[channel]);
            AudioClassifierResult result = audioClassifier.classify(audioData);

            for (int i = 0; i < 3; i++) {
                dataOut[channel][i * 2] = result.classificationResults().get(0).classifications().get(0).categories().get(i).index();
                dataOut[channel][i * 2 + 1] = result.classificationResults().get(0).classifications().get(0).categories().get(i).score();
                //resultUi =  result.classificationResults().get(0).classifications().get(0).categories().get(i).categoryName();
            }
        }
        // send one result to UI
        //sendMessage("CLASS", resultUi);
        send(dataOut);
    }


}
