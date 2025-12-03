package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.MappedByteBuffer;
import java.util.HashMap;

/**
 * Classifier with TensorFlow Lite (YAMNet)
 */
public class StageProcClassify extends Stage {

    private static final String LOG = "StageProcClassify";
    private static final String MODEL_PATH = "yamnet.tflite";
    private Interpreter tfliteInterpreter;
    private float[] inputBuffer;
    private java.util.List<String> labels;

    public StageProcClassify(HashMap parameter) {
        super(parameter);
        inputBuffer = new float[blockSize];
    }

    @Override
    void start() {
        try {
            MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MODEL_PATH);
            tfliteInterpreter = new Interpreter(modelBuffer);
            tfliteInterpreter.resizeInput(0, new int[]{blockSize}, true);
            labels = FileUtil.loadLabels(context, "yamnet_class_map.txt");
            Log.d(LOG, "TFLite YAMNet ready with " + labels.size() + " classes");
            super.start();
        } catch (Exception e) {
            throw new RuntimeException("TFLite init failed", e);
        }
    }

    @Override
    protected void process(float[][] buffer) {
        // only classify single channel
        float[] classificationResult = new float[6];  // [ID1, Score1, ID2, Score2, ID3, Score3]

        try {
            int channel = 0;
            System.arraycopy(buffer[channel], 0, inputBuffer, 0, blockSize);

            TensorBuffer inputTensor = TensorBuffer.createFixedSize(new int[]{blockSize}, DataType.FLOAT32);
            inputTensor.loadArray(inputBuffer);

            TensorBuffer outputTensor = TensorBuffer.createFixedSize(new int[]{labels.size()}, DataType.FLOAT32);

            tfliteInterpreter.run(inputTensor.getBuffer(), outputTensor.getBuffer().rewind());

            float[] outputScores = outputTensor.getFloatArray();

            float[] topScores = new float[3];
            int[] topIndices = new int[3];

            for (int i = 0; i < 3; i++) {
                topScores[i] = -Float.MAX_VALUE;
                topIndices[i] = -1;
            }

            for (int i = 0; i < outputScores.length; i++) {
                float score = outputScores[i];
                for (int j = 0; j < 3; j++) {
                    if (score > topScores[j]) {
                        for (int k = 2; k > j; k--) {
                            topScores[k] = topScores[k-1];
                            topIndices[k] = topIndices[k-1];
                        }
                        topScores[j] = score;
                        topIndices[j] = i;
                        break;
                    }
                }
            }

            for (int i = 0; i < 3; i++) {
                classificationResult[2*i] = topIndices[i];
                classificationResult[2*i + 1] = topScores[i];
            }

            //Log.d(LOG, "Classification: " + topIndices[0] + "(" + topScores[0] + ")");

        } catch (Exception e) {
            Log.e(LOG, "Classification failed", e);
            // output zeroes...
            for (int i = 0; i < 6; i++) classificationResult[i] = 0f;
        }

        float[][] dataOut = new float[1][6];
        System.arraycopy(classificationResult, 0, dataOut[0], 0, 6);

        send(dataOut);
    }

    @Override
    protected void cleanup() {
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }
}
