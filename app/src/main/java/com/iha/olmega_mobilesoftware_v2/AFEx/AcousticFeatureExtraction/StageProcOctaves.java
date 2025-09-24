package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;
import com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Utilities;

import android.util.Log;
import org.jtransforms.fft.FloatFFT_1D;
import java.util.HashMap;

/**
 * Feature extraction: averaged octave levels (see tau)
 *
 * Use the following feature definition:
 * <stage feature="StageProcOctaves" id="20" blocksize="400" hopsize="200"/>
 *
 */

public class StageProcOctaves extends Stage {

    final static String LOG = "StageProcOctaves";

    Octaves oct;

    public StageProcOctaves(HashMap parameter) {
        super(parameter);
        oct = new Octaves();
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

        float tau = 0.5f;
        float alpha, win_energy = 0;
        float[] window;
        float[][] data, magnitude, rms_temp;
        int nfft, samples, blocks_tau, block = 0;
        FloatFFT_1D fft;
        private final double[] F_CENTER = {31.5, 63, 125, 250, 500, 1000, 2000, 4000, 8000};
        private final int[][] BINS = bins();

        Octaves() {

            nfft = Utilities.nextpow2(blockSize);
            System.out.println("----------------> NFFT: " + nfft);

            // window & energy
            window = Utilities.hann(blockSize);
            for (float i : window) {
                win_energy += i * i;
            }

            fft = new FloatFFT_1D(nfft);
            alpha = (float) Math.exp(-hopSize / (samplingrate * tau));
            // write a block every tau ms, e.g. every 10th block for t=0.125 and hopSize = 0.0125
            blocks_tau = (int) Math.round((tau * 1000) / (hopSize * 1000.0 / samplingrate));
            samples = blockSize;
        }

        void calculate(float[][] input) {

            data = new float[channels][nfft * 2];
            magnitude = new float[channels][nfft / 2 + 1];
            float[][] rms = new float[channels][F_CENTER.length];

            for (int iChannel = 0; iChannel < data.length; iChannel++) {

                // window
                for (int i = 0; i < samples; i++) {
                    data[iChannel][i] = input[iChannel][i] * window[i];
                }

                // FFT
                fft.realForwardFull(data[iChannel]);

                // magnitude
                for (int i = 0; i < magnitude.length; i++) {
                    float re = data[iChannel][2 * i];
                    float im = data[iChannel][2 * i + 1];
                    magnitude[iChannel][i] = re * re + im * im;
                }

                // RMS in bands
                for (int band = 0; band < F_CENTER.length; band++) {
                    float sum = 0.0f;
                    for (int bin : BINS[band]) {
                        if (bin < magnitude[iChannel].length)
                            sum += magnitude[iChannel][bin];
                    }
                    rms[iChannel][band] = (float) Math.sqrt(sum);
                }
            }

            if (rms_temp == null) {
                // 1st block? Initialize rms_temp with (1-alpha) * P
                rms_temp = new float[channels][F_CENTER.length];
                for (int iChannel = 0; iChannel < channels; iChannel++) {
                    for (int band = 0; band < F_CENTER.length; band++) {
                        rms_temp[iChannel][band] = (1 - alpha) * rms[iChannel][band];
                    }
                }
            } else {
                // recursive averaging & store data for next average (rms_temp)
                for (int band = 0; band < F_CENTER.length; band++) {
                    for (int iChannel = 0; iChannel < channels; iChannel++) {
                        rms_temp[iChannel][band] = alpha * rms_temp[iChannel][band] + (1 - alpha) * rms[iChannel][band];
                    }
                }
            }

            // count blocks
            block++;
            // write every n-th block (blocks_tau) to output
            if (block == blocks_tau) {
                send(rms_temp);
                // TODO: remove debugging output:
                /*float[] out = new float[F_CENTER.length];
                for (int i = 0; i < F_CENTER.length; i++) {
                    out[i] =  20.0f * (float) Math.log10(rms_temp[0][i] + 1e-12);
                }
                Log.d(LOG, Arrays.toString(out)); */
                block = 0;
            }
        }

        private int[][] bins() {

            double binWidth = (double) samplingrate / nfft;
            int[][] bins = new int[F_CENTER.length][];

            for (int i = 0; i < F_CENTER.length; i++) {
                double f1 = F_CENTER[i] / Math.sqrt(2); // f_min
                double f2 = F_CENTER[i] * Math.sqrt(2); // f_max

                int bin1 = (int) Math.floor(f1 / binWidth);
                int bin2 = (int) Math.ceil(f2 / binWidth);

                int[] band = new int[Math.max(0, bin2 - bin1 + 1)];
                for (int k = 0; k < band.length; k++) {
                    band[k] = bin1 + k;
                }
                bins[i] = band;
            }
            return bins;
        }
    }

}
