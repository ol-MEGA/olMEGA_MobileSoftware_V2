package com.iha.olmega_mobilesoftware_v2.AFEx.Processing;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;

import com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Preprocessing.MelFilterbank;
import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public class SpeakerID {

    private static final String LOG = "SpeakerID";

    private Context context;
    private OrtEnvironment env;
    private OrtSession session;
    private MelFilterbank melFilterbank;

    private float[] referenceId;
    private float[] referenceIdNormalized;
    private float referenceNorm;

    // Reusable buffers for performance
    private float[] flatDataBuffer;
    private FloatBuffer floatBuffer;
    private long[] tensorDimensions;

    public SpeakerID(Context context, int samplingRate, String onnxModelAssetPath) {
        this.context = context;

        // Initialize ONNX environment and session
        try {
            env = OrtEnvironment.getEnvironment();
            File modelFile = Utilities.copyAssetToFile(context, onnxModelAssetPath);
            session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ONNX Runtime or load model", e);
        }

        melFilterbank = new MelFilterbank(samplingRate);
        tensorDimensions = new long[]{1, 80, 0};
    }

    public void loadReferenceFromWav(String filename) {
        try {
            Instant start = Instant.now();


            InputStream stream = null;
            try {
                AssetFileDescriptor fd = context.getAssets().openFd(filename);
                stream = fd.createInputStream();

                byte[] header = new byte[44];
                int headerBytesRead = stream.read(header, 0, 44);
                if (headerBytesRead != 44) {
                    throw new IOException("Could not read WAV header");
                }

                long audioBytes = fd.getLength() - 44;
                int numSamples = (int) (audioBytes / 2);
                float[] reference_data = new float[numSamples];

                byte[] chunkBuffer = new byte[16384];
                int totalSamplesRead = 0;
                int bytesRead;

                while ((bytesRead = stream.read(chunkBuffer)) != -1 && totalSamplesRead < numSamples) {
                    ByteBuffer buffer = ByteBuffer.wrap(chunkBuffer, 0, bytesRead);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    int samplesInChunk = bytesRead / 2;
                    for (int k = 0; k < samplesInChunk && totalSamplesRead < numSamples; k++) {
                        short sample = buffer.getShort();
                        reference_data[totalSamplesRead++] = sample / 32768.0f;
                    }
                }

                referenceId = getSpeakerID(reference_data);

            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.e(LOG, "Failed to close stream", e);
                    }
                }
            }

            referenceNorm = computeNorm(referenceId);
            referenceIdNormalized = new float[referenceId.length];
            for (int i = 0; i < referenceId.length; i++) {
                referenceIdNormalized[i] = referenceId[i] / referenceNorm;
            }

            Log.d(LOG, "Reference ID loaded and normalized in " + (Instant.now().toEpochMilli() - start.toEpochMilli()) + "ms");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load reference WAV", e);
        }
    }

    public void loadReferenceFromCsv(String filename) {
        try {
            Instant start = Instant.now();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(filename)));
                 CSVReader csv = new CSVReader(br)) {

                String[] row = csv.readNext();
                if (row == null) throw new IOException("Cannot read reference ID, file is empty.");
                if (row.length < 256) throw new IOException("Expected at least 256 values, found: " + row.length);

                float[] values = new float[256];
                for (int i = 0; i < 256; i++) {
                    try {
                        values[i] = Float.parseFloat(row[i].trim());
                    } catch (NumberFormatException e) {
                        throw new NumberFormatException("Invalid value at " + i + ": '" + row[i] + "'");
                    }
                }

                referenceId = getSpeakerID(values);
            }

            referenceNorm = computeNorm(referenceId);
            referenceIdNormalized = new float[referenceId.length];
            for (int i = 0; i < referenceId.length; i++) {
                referenceIdNormalized[i] = referenceId[i] / referenceNorm;
            }

            Log.d(LOG, "Reference ID loaded from CSV and normalized in " + (Instant.now().toEpochMilli() - start.toEpochMilli()) + "ms");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load reference CSV", e);
        }
    }

    public float[] getSpeakerID(float[] audioData) throws OrtException {
        float[][] spectrogram = melFilterbank.compute(audioData);

        int rows = spectrogram.length;
        int cols = spectrogram[0].length;

        int requiredSize = rows * cols;
        if (flatDataBuffer == null || flatDataBuffer.length != requiredSize) {
            flatDataBuffer = new float[requiredSize];
            floatBuffer = FloatBuffer.wrap(flatDataBuffer);
        }

        for (int i = 0; i < rows; i++) {
            System.arraycopy(spectrogram[i], 0, flatDataBuffer, i * cols, cols);
        }

        floatBuffer.rewind();
        tensorDimensions[2] = cols;

        OnnxTensor input = OnnxTensor.createTensor(env, floatBuffer, tensorDimensions);
        try {
            Map<String, OnnxTensor> inputs = Collections.singletonMap("input", input);
            Result results = session.run(inputs);
            try {
                float[][] outputArray = (float[][]) results.get(0).getValue();
                return outputArray[0];
            } finally {
                results.close();
            }
        } finally {
            input.close();
        }
    }
    
    public float compare(float[] audioData) throws OrtException {
        float[] currentId = getSpeakerID(audioData);
        return cosineSimilarity(currentId);
    }

    public float[] getReferenceID() {
        if (referenceId == null) {
            return null;
        }
        return referenceId.clone();
    }

    public float[] getReferenceIdNormalized() {
        if (referenceIdNormalized == null) {
            return null;
        }
        return referenceIdNormalized.clone();
    }

    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (OrtException e) {
            Log.e(LOG, "Failed to close ONNX resources", e);
        }
    }

    private float cosineSimilarity(float[] currentId) {
        float norm = computeNorm(currentId);
        float dot = 0f;
        for (int i = 0; i < currentId.length; i++) {
            dot += (currentId[i] / norm) * referenceIdNormalized[i];
        }
        return dot;
    }

    private float computeNorm(float[] vec) {
        float sum = 0f;
        for (float v : vec) {
            sum += v * v;
        }
        return (float) Math.sqrt(sum);
    }
}
