package com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Preprocessing;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

public class MelFilterbank {

    private static final int N_MELS = 80;
    private static final int N_FFT = 512;
    private static final int WIN_LENGTH = 400;
    private static final int HOP_LENGTH = 160;
    private static final int N_FREQ_BINS = N_FFT / 2 + 1;

    private float[][] melFilter;
    private float[] windowFunc;

    // JTransforms FFT object (reusable)
    private FloatFFT_1D fft;

    // Reusable buffers to avoid allocations in hot loop
    private float[] fftBuffer;
    private float[] magSpec;

    // Sparse mel filterbank representation for faster computation
    private int[][] melFilterNonZeroIndices;  // [mel][index_in_sparse_array]
    private float[][] melFilterNonZeroValues; // [mel][index_in_sparse_array]

    public MelFilterbank(int sampleRate) {
        // Generate the Mel filterbank
        melFilter = melFilter(sampleRate, N_FFT, N_MELS);

        // Generate the window function
        windowFunc = generateHammingWindow(WIN_LENGTH);

        // Initialize JTransforms FFT
        fft = new FloatFFT_1D(N_FFT);

        // Pre-allocate reusable buffers
        fftBuffer = new float[N_FFT * 2]; // JTransforms needs 2*N for complex data
        magSpec = new float[N_FREQ_BINS];

        // Create sparse representation of mel filterbank
        createSparseFilterbank();
    }

    public float[][] compute(float[] audio) {
        int numFrames = (audio.length - WIN_LENGTH) / HOP_LENGTH + 1;
        float[][] melSpectrogram = new float[N_MELS][numFrames];

        // Process audio in blocks
        for (int i = 0; i < numFrames; i++) {
            int start = i * HOP_LENGTH;

            // Clear FFT buffer
            Arrays.fill(fftBuffer, 0f);

            // Apply windowing directly to FFT buffer (avoids extra array copy)
            int copyLen = Math.min(WIN_LENGTH, audio.length - start);
            for (int j = 0; j < copyLen; j++) {
                fftBuffer[j] = audio[start + j] * windowFunc[j];
            }

            // Compute FFT using JTransforms
            // Input: [real0, real1, ..., realN-1, 0, 0, ...]
            // Output: [real0, imag0, real1, imag1, ..., realN-1, imagN-1]
            fft.realForwardFull(fftBuffer);

            // Compute magnitude spectrum and square it
            computeMagnitudeSquared(fftBuffer, magSpec);

            // Apply Mel filterbank using sparse representation
            applyMelFilterbankSparse(magSpec, melSpectrogram, i);

            // Apply log scaling
            for (int m = 0; m < N_MELS; m++) {
                melSpectrogram[m][i] = 10.f * (float) Math.log10(melSpectrogram[m][i] + 1e-6);
            }
        }

        return melSpectrogram;
    }

    /**
     * Compute squared magnitude from FFT output
     * JTransforms output format: [real0, imag0, real1, imag1, ...]
     */
    private void computeMagnitudeSquared(float[] fftData, float[] magSpec) {
        // DC component (k=0): only real part
        magSpec[0] = fftData[0] * fftData[0];

        // k=1 to k=N/2-1: complex pairs
        for (int k = 1; k < N_FREQ_BINS - 1; k++) {
            float real = fftData[2 * k];
            float imag = fftData[2 * k + 1];
            magSpec[k] = real * real + imag * imag;
        }

        // Nyquist component (k=N/2): only real part (stored at index 1)
        magSpec[N_FREQ_BINS - 1] = fftData[1] * fftData[1];
    }

    /**
     * Create sparse representation of mel filterbank
     * Most values are zero, so we only store non-zero indices and values
     */
    private void createSparseFilterbank() {
        melFilterNonZeroIndices = new int[N_MELS][];
        melFilterNonZeroValues = new float[N_MELS][];

        for (int m = 0; m < N_MELS; m++) {
            // Count non-zero elements
            int nonZeroCount = 0;
            for (int f = 0; f < N_FREQ_BINS; f++) {
                if (melFilter[m][f] > 0) {
                    nonZeroCount++;
                }
            }

            // Store only non-zero indices and values
            melFilterNonZeroIndices[m] = new int[nonZeroCount];
            melFilterNonZeroValues[m] = new float[nonZeroCount];

            int idx = 0;
            for (int f = 0; f < N_FREQ_BINS; f++) {
                if (melFilter[m][f] > 0) {
                    melFilterNonZeroIndices[m][idx] = f;
                    melFilterNonZeroValues[m][idx] = melFilter[m][f];
                    idx++;
                }
            }
        }
    }

    /**
     * Apply mel filterbank using sparse representation
     * Much faster than iterating over all frequency bins
     */
    private void applyMelFilterbankSparse(float[] magSpec, float[][] melSpectrogram, int frameIdx) {
        for (int m = 0; m < N_MELS; m++) {
            float sum = 0f;
            int[] indices = melFilterNonZeroIndices[m];
            float[] values = melFilterNonZeroValues[m];

            for (int i = 0; i < indices.length; i++) {
                sum += magSpec[indices[i]] * values[i];
            }

            melSpectrogram[m][frameIdx] = sum;
        }
    }

    private static float[] generateHammingWindow(int size) {
        float[] window = new float[size];
        for (int i = 0; i < size; i++) {
            window[i] = 0.54f - 0.46f * (float) Math.cos(2 * Math.PI * i / (size - 1));
        }
        return window;
    }

    private static float[][] melFilter(int sampleRate, int n_fft, int n_mels) {
        float fMin = 0.f;
        float fMax = sampleRate / 2.f;
        int nFreqs = n_fft / 2 + 1;

        // Frequency bins
        float[] allFreqs = linspace(0.f, fMax, nFreqs);

        // Mel frequency bins
        float mMin = hz2mel(fMin);
        float mMax = hz2mel(fMax);
        float[] fPts = linspace(mMin, mMax, n_mels + 2);
        for (int mel = 0; mel < n_mels + 2; mel++) {
            fPts[mel] = mel2hz(fPts[mel]);
        }

        // Differences between filter midpoints
        float[] fDiff = new float[n_mels + 1];
        for (int mel = 0; mel < n_mels + 1; mel++) {
            fDiff[mel] = fPts[mel + 1] - fPts[mel];
        }

        // Slopes
        float[][] slopes = new float[nFreqs][n_mels + 2];
        for (int freq = 0; freq < nFreqs; freq++) {
            for (int mel = 0; mel < n_mels + 2; mel++) {
                slopes[freq][mel] = fPts[mel] - allFreqs[freq];
            }
        }

        // Down slopes and up slopes
        float[][] downSlopes = new float[nFreqs][n_mels];
        float[][] upSlopes = new float[nFreqs][n_mels];
        for (int freq = 0; freq < nFreqs; freq++) {
            for (int mel = 0; mel < n_mels; mel++) {
                downSlopes[freq][mel] = -slopes[freq][mel] / fDiff[mel];
                upSlopes[freq][mel] = slopes[freq][mel + 2] / fDiff[mel + 1];
            }
        }

        // Create filterbank
        float[][] melFilter = new float[n_mels][nFreqs];
        for (int mel = 0; mel < n_mels; mel++) {
            for (int freq = 0; freq < nFreqs; freq++) {
                float val = (float) Math.max(0.0, Math.min(downSlopes[freq][mel], upSlopes[freq][mel]));
                melFilter[mel][freq] = val;
            }
        }

        return melFilter;
    }

    // Helper functions
    private static float hz2mel(float hz) {
        return 2595.f * (float) Math.log10(1.0 + hz / 700.0);
    }

    private static float mel2hz(float mel) {
        return 700.f * (float) (Math.pow(10.0, mel / 2595.0) - 1.0);
    }

    private static float[] linspace(float start, float end, int num) {
        float[] result = new float[num];
        float step = (end - start) / (num - 1);
        for (int i = 0; i < num; i++) {
            result[i] = start + step * i;
        }
        return result;
    }
}
