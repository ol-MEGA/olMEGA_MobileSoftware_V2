package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;

import com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Preprocessing.MelFilterbank;
import com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Utilities;
import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.States;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import com.iha.olmega_mobilesoftware_v2.States;

public class StageProcSpeakerID extends Stage {

    final static String LOG = "StageProcSpeakerID";
    private static final org.apache.commons.logging.Log log = LogFactory.getLog(StageProcSpeakerID.class);

    // onnx runtime fields
    private OrtEnvironment env;
    private OrtSession session;
    private MelFilterbank melFilterbank;
    private float[] reference_id;

    // Pre-normalized reference for faster comparison
    private float[] reference_id_normalized;
    private float reference_id_norm;

    // Reusable buffers to avoid allocations
    private float[] flatDataBuffer;
    private FloatBuffer floatBuffer;
    private long[] tensorDimensions;
    private int count = 0;

    public StageProcSpeakerID(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start(){
        // get onnx model from assets
        File modelFile;
        try {
            modelFile = Utilities.copyAssetToFile(context, "speaker_id.onnx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Initialize ONNX Runtime
        env = OrtEnvironment.getEnvironment();

        try {
            session = this.env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
        } catch (OrtException e) {
            e.printStackTrace();
        }

        // init filterbank
        melFilterbank = new MelFilterbank(samplingrate);

        // Pre-allocate tensor dimensions (constant)
        tensorDimensions = new long[]{1, 80, 0};  // cols will be set per call

        // load reference speaker id from wave
        Instant t = Instant.now();
        File reference = new File(com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO.getSpeakerIdPath(), "reference.wav");
        if (reference.exists()) {
            reference_id = loadReferenceFromWav("reference.wav");
        } else {
            sendBroadcast(States.sid_no_reference);
            super.stop();
            return;
        }

        // Pre-normalize reference for optimized cosine similarity
        reference_id_norm = computeNorm(reference_id);
        reference_id_normalized = new float[reference_id.length];
        for (int i = 0; i < reference_id.length; i++) {
            reference_id_normalized[i] = reference_id[i] / reference_id_norm;
        }

        Log.d(LOG, "calculated reference in " + (Instant.now().toEpochMilli() - t.toEpochMilli()) + "ms");

        super.start();
    }

    protected float[] getSpeakerID(float[] audioData) throws OrtException {
        // compute Mel spectrogram
        float[][] spectrogram = melFilterbank.compute(audioData);

        int rows = spectrogram.length;
        int cols = spectrogram[0].length;

        // Allocate flat buffer only once or if size changed
        int requiredSize = rows * cols;
        if (flatDataBuffer == null || flatDataBuffer.length != requiredSize) {
            flatDataBuffer = new float[requiredSize];
            floatBuffer = FloatBuffer.wrap(flatDataBuffer);
        }

        // Optimized flattening: direct array copy in row-major order
        for (int i = 0; i < rows; i++) {
            System.arraycopy(spectrogram[i], 0, flatDataBuffer, i * cols, cols);
        }

        // Rewind buffer for reuse
        floatBuffer.rewind();

        // Update dimensions
        tensorDimensions[2] = cols;

        // Create tensor
        OnnxTensor input = OnnxTensor.createTensor(env, floatBuffer, tensorDimensions);

        try {
            // Create input map (Collections.singletonMap is lightweight)
            Map<String, OnnxTensor> inputs = Collections.singletonMap("input", input);

            // Run inference
            Result results = session.run(inputs);

            try {
                // get the speaker embedding
                float[][] outputArray = (float[][]) results.get(0).getValue();
                return outputArray[0];
            } finally {
                // Close results to free memory
                results.close();
            }
        } finally {
            // Always close tensor to prevent memory leak
            input.close();
        }
    }

    protected float compareSpeaker(float[] audioData) throws OrtException {
        float[] speaker_id = getSpeakerID(audioData);
        return cosineSimilarity(speaker_id);
    }

    @Override
    protected void process(float[][] buffer) {
        float[][] dataOut = new float[buffer.length][1];
        for (int channel = 0; channel < channels; channel++) {
            try {
                dataOut[channel][0] = compareSpeaker(buffer[channel]);
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
        }
        send(dataOut);
    }

    @Override
    protected void cleanup() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        env.close();
    }

    /**
     * cosine similarity using pre-normalized reference
     */
    private float cosineSimilarity(float[] a) {
        // Compute norm of a
        float normA = computeNorm(a);

        // Compute dot product with normalized vectors in one pass
        float dot = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += (a[i] / normA) * reference_id_normalized[i];
        }

        return dot;
    }

    /**
     * Compute L2 norm of vector
     */
    private static float computeNorm(float[] vec) {
        float sum = 0f;
        for (float v : vec) {
            sum += v * v;
        }
        return (float) Math.sqrt(sum);
    }

    public static float[] loadReferenceFromCsv(Context context, String assetFilename)
            throws IOException, NumberFormatException {

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(context.getAssets().open(assetFilename)));
             CSVReader csv = new CSVReader(br)) {

            String[] row = csv.readNext();
            if (row == null) {
                throw new IOException("Cannot read reference ID, file is empty.");
            }
            if (row.length < 256) {
                throw new IOException("Cannot read reference ID, expected 256 values, found: " + row.length);
            }

            float[] values = new float[256];
            for (int i = 0; i < 256; i++) {
                try {
                    values[i] = Float.parseFloat(row[i].trim());
                } catch (NumberFormatException e) {
                    throw new NumberFormatException(
                            "Cannot read reference ID, invalid value at  " + i + ": '" + row[i] + "'");
                }
            }
            return values;
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private float[] loadReferenceFromWav(String filename) {
        InputStream stream = null;
        float[] reference_data;

        try {
            File file = new File(com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO.ID_FOLDER, filename);
            stream = new FileInputStream(file);

            // Skip WAV header (44 bytes)
            byte[] header = new byte[44];
            int headerBytesRead = stream.read(header, 0, 44);
            if (headerBytesRead != 44) {
                throw new IOException("Could not read WAV header");
            }

            long audioBytes = file.length() - 44;
            int numSamples = (int) (audioBytes / 2);
            reference_data = new float[numSamples];

            // Read audio data in chunks with larger buffer
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

            Log.d(LOG, String.format("Loaded %d samples from %s (expected %d)",
                    totalSamplesRead, filename, numSamples));

            if (totalSamplesRead != numSamples) {
                Log.w(LOG, String.format("Warning: Expected %d samples but read %d",
                        numSamples, totalSamplesRead));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load WAV file: " + filename, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(LOG, "Failed to close stream", e);
                }
            }
        }

        // Compute speaker ID
        float[] id;
        try {
            id = getSpeakerID(reference_data);
            Log.d(LOG, "Reference speaker ID computed.");
        } catch (OrtException e) {
            throw new RuntimeException("Failed to compute speaker ID", e);
        }

        return id;
    }

    private void sendBroadcast(States state) {
        switch (state) {
            case sid_no_reference:
                LogIHAB.log("SpeakerID: no reference");
                break;
        }
        Intent intent = new Intent("StageState");    //action: "msg"
        intent.setPackage(context.getPackageName());
        intent.putExtra("currentState", state.ordinal());
        context.sendBroadcast(intent);
    }

}
