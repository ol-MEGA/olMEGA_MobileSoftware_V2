package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Utilities;
import android.util.Log;
import org.jtransforms.fft.FloatFFT_1D;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Feature extraction: averaged octave levels (see tau)
 *
 * Use the following feature definition:
 * <stage feature="StageProcOctaves" id="20" blocksize="400" hopsize="200" blockout="8000" hopout="8000"/>
 *
 */
public class StageProcOctaves extends Stage {

    private static final String LOG = "StageProcOctaves";
    private final Octaves oct;
    private boolean initialized;

    public StageProcOctaves(HashMap parameter) {
        super(parameter);
        oct = new Octaves();
    }

    @Override
    void start() {
        initialized = false;
        super.start();
    }

    @Override
    protected void cleanup() {
        Log.d(LOG, "Stopped " + LOG);
    }

    @Override
    protected void process(float[][] buffer) {
        oct.calculate(buffer);
    }


    private class Octaves {

        // parameters
        private final float tau = 0.5f;
        private final double[] F_CENTER = {31.5, 63, 125, 250, 500, 1000, 2000, 4000, 8000};

        // fft/window
        private final int nfft;
        private final FloatFFT_1D fft;
        private final float[] window;
        final float scale;
        private final float alpha;
        private final int blocks_tau;
        private int blockCounter = 0;

        // buffer
        private final float[][] data;       // [channels][2*N] FFT Input
        private final float[][] mag;       // [channels][N/2+1] |X[k]|**2
        private final float[][] p_temp;     // [chanels][bands] smoothed power
        private final float[][] out_rms;    // [channels][bands]
        private final int[][] BINS;         // [band][indices]

        Octaves() {

            nfft = Utilities.nextpow2(blockSize);
            fft = new FloatFFT_1D(nfft);

            // Hann Window & energy
            window = Utilities.hann(blockSize);
            float win_energy = 0f;
            for (float v : window) win_energy += v * v;
            scale = 1f / (nfft * win_energy); // scaling (Parceval & Hann)

            // params for exp. smoothing
            alpha = (float) Math.exp(-hopSize / (samplingrate * tau));
            blocks_tau = Math.max(1, (int) Math.round((tau * samplingrate) / hopSize));

            int bins = nfft / 2 + 1;
            data = new float[channels][2 * nfft];
            mag = new float[channels][bins];
            p_temp = new float[channels][F_CENTER.length];
            out_rms = new float[channels][F_CENTER.length];

            BINS = getBins();

            Log.d(LOG, "Init OK: nfft=" + nfft + " bins=" + bins + " blocks_tau=" + blocks_tau);
        }

        void calculate(float[][] input) {

            for (int ch = 0; ch < channels; ch++) {

                Arrays.fill(data[ch], 0f);
                for (int n = 0; n < blockSize; n++) {
                    data[ch][n] = input[ch][n] * window[n];
                }

                fft.realForwardFull(data[ch]);

                int K = nfft / 2 + 1;
                for (int k = 0; k < K; k++) {
                    float re = data[ch][2 * k];
                    float im = data[ch][2 * k + 1];
                    float p = re * re + im * im;

                    if (k > 0 && k < nfft / 2) p *= 2f;
                    mag[ch][k] = p;
                }
            }

            for (int ch = 0; ch < channels; ch++) {
                for (int b = 0; b < F_CENTER.length; b++) {
                    float sum = 0f;
                    int[] idx = BINS[b];
                    for (int k : idx) {
                        if (k < mag[ch].length)
                            sum += mag[ch][k];
                    }

                    float rms = scale * sum;
                    // recursive averaging & store data for next average
                    if (!initialized)
                        p_temp[ch][b] = (1 - alpha) * rms;
                    else
                        p_temp[ch][b] = alpha * p_temp[ch][b] + (1 - alpha) * rms;

                    out_rms[ch][b] = (float) Math.sqrt(Math.max(0f, p_temp[ch][b]));
                }
            }

            initialized = true;

            blockCounter++;
            if (blockCounter >= blocks_tau) {
                send(out_rms);
                blockCounter = 0;
            }
        }

        private int[][] getBins() {
            double binWidth = (double) samplingrate / nfft;
            int maxK = nfft / 2;

            int[][] bins = new int[F_CENTER.length][];
            for (int i = 0; i < F_CENTER.length; i++) {
                double f1 = F_CENTER[i] / Math.sqrt(2);
                double f2 = F_CENTER[i] * Math.sqrt(2);

                int k1 = (int) Math.floor(f1 / binWidth);
                int k2 = (int) Math.ceil(f2 / binWidth);

                k1 = Math.max(0, k1);
                k2 = Math.min(maxK, k2);

                int len = Math.max(0, k2 - k1 + 1);
                int[] band = new int[len];
                for (int k = 0; k < len; k++) band[k] = k1 + k;

                bins[i] = band;
            }
            return bins;
        }
    }
}
