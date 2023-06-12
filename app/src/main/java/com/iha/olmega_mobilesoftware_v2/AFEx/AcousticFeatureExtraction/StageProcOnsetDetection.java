package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

public class StageProcOnsetDetection extends Stage {

    final static String LOG = "StageProcOnsetDetection";

    private double T;

    private double[] bandSplit_State_1;
    private double[] bandSplit_State_2;

    private double bandSplit_Frequency = 800.0f;
    private double bandSplit_Q = 1.0f/Math.sqrt(2);

    private double bandSplit_Radians;
    private double bandSplit_wa;
    private double bandSplit_G;
    private double bandSplit_R;
    private double bandSplit_MulState1;
    private double bandSplit_MulIn;

    // [lp, bp, hp][L, R]
    private float[] energyRatio_Tau_Fast_ms;
    private float[] energyRatio_Tau_Slow_ms;
    private float[][] energyRatio_State_Fast;
    private float[][] energyRatio_State_Slow;
    private float[] energyRatio_Alpha_Fast;
    private float[] energyRatio_Alpha_Slow;
    private float[] energyRatio_Alpha_Fast_MinusOne;
    private float[] energyRatio_Alpha_Slow_MinusOne;
    // [LP, BP, HP, WB]
    private float[] detectOnsets_ThreshBase;
    private float[] detectOnsets_ThreshRaise;
    private float[] detectOnsets_Param1;
    private float[] detectOnsets_Decay;

    private float rms_rec;
    private float alpha;

    public StageProcOnsetDetection(HashMap parameter) {
        super(parameter);

        T = 1.0f / samplingrate;
        alpha = 0.1f / samplingrate;

        bandSplit_State_1 = new double[2];
        bandSplit_State_2 = new double[2];

        bandSplit_Radians = this.bandSplit_Frequency * 2 * Math.PI;
        bandSplit_wa = (2.0f / T) * Math.tan(bandSplit_Radians * T / 2.0f);
        bandSplit_G = bandSplit_wa * T / 2.0f;
        bandSplit_R = 0.5f / bandSplit_Q;
        bandSplit_MulState1 = 2.0f * bandSplit_R + bandSplit_G;
        bandSplit_MulIn = 1.0f / (1.0f + 2.0f * bandSplit_R * bandSplit_G + bandSplit_G * bandSplit_G);

        // [lp, bp, hp, wb][L, R]
        energyRatio_Tau_Fast_ms = new float[] {4.0f, 2.0f, 2.0f, 2.0f}; //1.0f, 1.0f, 1.0f, 1.0f
        energyRatio_Tau_Slow_ms = new float[] {160.0f, 20.0f, 10.0f, 5.0f}; //80.0f, 40.0f, 20.0f, 5.0f
        energyRatio_State_Fast = new float[4][2];
        energyRatio_State_Slow = new float[4][2];
        energyRatio_Alpha_Fast = new float[] {
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[0] * 0.001 * samplingrate))),
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[1] * 0.001 * samplingrate))),
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[2] * 0.001 * samplingrate))),
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[3] * 0.001 * samplingrate)))};
        energyRatio_Alpha_Slow = new float[] {
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[0] * 0.001 * samplingrate))),
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[1] * 0.001 * samplingrate))),
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[2] * 0.001 * samplingrate))),
                (float) (1.0f - Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[3] * 0.001 * samplingrate)))};
        energyRatio_Alpha_Fast_MinusOne = new float[] {
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[0] * 0.001 * samplingrate))),
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[1] * 0.001 * samplingrate))),
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[2] * 0.001 * samplingrate))),
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Fast_ms[3] * 0.001 * samplingrate)))};
        energyRatio_Alpha_Slow_MinusOne = new float[] {
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[0] * 0.001 * samplingrate))),
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[1] * 0.001 * samplingrate))),
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[2] * 0.001 * samplingrate))),
                (float) (- Math.exp(-1.0f / (this.energyRatio_Tau_Slow_ms[3] * 0.001 * samplingrate)))};
        // [LP, BP, HP, WB]
        detectOnsets_ThreshBase = new float[] {8.0f, 4.0f, 4.0f, 16.0f}; //8.0f, 8.0f, 8.0f, 8.0f
        detectOnsets_ThreshRaise = new float[] {0.0f, 0.0f, 0.0f, 0.0f}; //0.0f, 0.0f, 0.0f, 0.0f
        detectOnsets_Param1 = new float[] {8.0f, 4.0f, 6.0f, 32.0f}; //4.0f, 4.0f, 4.0f, 4.0f
        detectOnsets_Decay = new float[] {(float) Math.pow(2000.0f, (-1.0f / samplingrate)),
                (float) Math.pow(8000.0f, (-1.0f / samplingrate)),
                (float) Math.pow(100.0f, (-1.0f / samplingrate)),
                (float) Math.pow(100.0f, (-1.0f / samplingrate))}; // 8192, 8192, 8192, 8192

    }

    @Override
    protected void cleanup() {
        Log.d(LOG, "Stopped " + LOG);
        super.cleanup();
    }

    @Override
    protected void process(float[][] buffer) {

        float[] block_left = new float[blockSize];
        float[] block_right = new float[blockSize];

        for (int iSample = 0; iSample < blockSize; iSample++) {
            block_left[iSample] = buffer[0][iSample];
            block_right[iSample] = buffer[1][iSample];
        }

        float data = onsetDetection(block_left, block_right);

        float[][] dataOut = new float[1][1];
        dataOut[0][0] = data;
        send(dataOut);
    }

    protected float onsetDetection(float[] block_left, float[] block_right) {
        // left
        float[][] bands_left = bandSplit(block_left, 0);
        // right
        float[][] bands_right = bandSplit(block_right, 1);

        return detectOnsets(bands_left, bands_right, block_left, block_right);
    }

    protected float detectOnsets(float[][] bands_left, float[][] bands_right, float[] block_left, float[] block_right) {

        float[][] flags = new float[4][2];
        float flag = 0.0f;
        boolean onsetFound = false;
        float[] threshold = {0.0f, 0.0f, 0.0f, 0.0f};
        float rms = 0.5f * (rms(block_left) + rms(block_right));
        this.rms_rec = this.alpha * rms + (1.0f - this.alpha) * this.rms_rec;

        float[] energy_lp_left = energyRatio(getChannel(bands_left, 0), 0, 0);
        float[] energy_bp_left = energyRatio(getChannel(bands_left, 1), 1, 0);
        float[] energy_hp_left = energyRatio(getChannel(bands_left, 2), 2, 0);
        float[] energy_wb_left = energyRatio(block_left, 3, 0);
        float[] energy_lp_right = energyRatio(getChannel(bands_right, 0), 0, 1);
        float[] energy_bp_right = energyRatio(getChannel(bands_right, 1), 1, 1);
        float[] energy_hp_right = energyRatio(getChannel(bands_right, 2), 2, 1);
        float[] energy_wb_right = energyRatio(block_right, 3, 1);

        for (int iSample = 0; iSample < blockSize; iSample++) {

            threshold = addElementwise(this.detectOnsets_ThreshBase, this.detectOnsets_ThreshRaise);
            flags = new float[4][2];

            if (!onsetFound) {

                if (energy_lp_left[iSample] > threshold[0]) {
                    flags[0][0] = 1.0f;
                }
                if (energy_bp_left[iSample] > threshold[1]) {
                    flags[1][0] = 1.0f;
                }
                if (energy_hp_left[iSample] > threshold[2]) {
                    flags[2][0] = 1.0f;
                }
                if (energy_wb_left[iSample] > threshold[3]) {
                    flags[3][0] = 1.0f;
                }
                if (energy_lp_right[iSample] > threshold[0]) {
                    flags[0][1] = 1.0f;
                }
                if (energy_bp_right[iSample] > threshold[1]) {
                    flags[1][1] = 1.0f;
                }
                if (energy_hp_right[iSample] > threshold[2]) {
                    flags[2][1] = 1.0f;
                }
                if (energy_wb_right[iSample] > threshold[3]) {
                    flags[3][1] = 1.0f;
                }

                // If more than one band has registered a peak then return 1, else 0
                if ((flags[0][0] + flags[1][0] + flags[2][0] + flags[3][0] +
                        flags[0][1] + flags[1][1] + flags[2][1] + flags[3][1]) > 1.0f) {
                    flag = 1.0f;
                    onsetFound = true;
                    this.detectOnsets_ThreshRaise = multiplyElementwise(
                            detectOnsets_Param1, threshold);
                }
            }
            multiplyWithArray(detectOnsets_ThreshRaise, detectOnsets_Decay);
        }
        return flag;
    }

    protected float rms(float[] signal) {
        float out = 0;
        for (int iSample = 0; iSample < signal.length; iSample++) {
            out += signal[iSample] * signal[iSample];
        }
        out /= signal.length;
        return (float) Math.sqrt(out);
    }

    protected float[] getChannel(float[][] in, int chan) {
        float[] out = new float[in.length];
        for (int iSample = 0; iSample < in.length; iSample++) {
            out[iSample] = in[iSample][chan];
        }
        return out;
    }

    protected void multiplyWithArray(float[] a, float[] b) {
        for (int iCol = 0; iCol < a.length; iCol++) {
            a[iCol] *= b[iCol];
        }
    }

    protected float[] multiplyElementwise(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int iCol = 0; iCol < a.length; iCol++) {
            out[iCol] = a[iCol] * b[iCol];
        }
        return out;
    }

    protected float[] addElementwise(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int iCol = 0; iCol < a.length; iCol++) {
            out[iCol] = a[iCol] + b[iCol];
        }
        return out;
    }

    protected float[][] bandSplit(float[] signal, int chan) {

        /**
         *  Perform a bandsplit via state variable filter
         *
         *  input:  float[blocklen] signal
         *          int chan [0, 1] left/right for correct filter states
         *  output: float[blocklen][3] output (blocklen x [lp_L, bp_L, hp_L])
         *
         */

        float ylp, ybp, yhp, tmp_1, tmp_2;

        float[][] output = new float[this.blockSize][3];

        for (int iSample = 0; iSample < this.blockSize; iSample ++) {

            yhp = (float) ((signal[iSample] - bandSplit_MulState1 * bandSplit_State_1[chan] -
                    bandSplit_State_2[chan]) * bandSplit_MulIn);


            tmp_1 = (float) (yhp * bandSplit_G);

            ybp =  (float) (bandSplit_State_1[chan] + tmp_1);

            bandSplit_State_1[chan] = ybp + tmp_1;

            tmp_2 = (float) (ybp * bandSplit_G);

            ylp = (float) (bandSplit_State_2[chan] + tmp_2);

            bandSplit_State_2[chan] = ylp + tmp_2;

            output[iSample][0] = ylp;
            output[iSample][1] = ybp;
            output[iSample][2] = yhp;

        }

        return output;
    }

    protected float[] energyRatio(float[] signal, int band, int chan) {

        /**
         * Energy Ratio of signal
         *
         * Signal input format is float[blocklen] (single channel)
         *
         * Signal output format is float[blocklen] (single channel)
         *
         * band, chan: needed to target specific filter states (lp, bp, hp, wb)
         *
         */

        float[] signal_square = multiplyElementwise(signal, signal);

        float[] signal_filtered_Fast = filter(signal_square, energyRatio_Alpha_Fast, energyRatio_Alpha_Fast_MinusOne, band, chan, 0);
        float[] signal_filtered_Slow = filter(signal_square, energyRatio_Alpha_Slow, energyRatio_Alpha_Slow_MinusOne, band, chan, 1);

        float[] ratio = new float[this.blockSize];

        for (int iSample = 0; iSample < blockSize; iSample++) {
            ratio[iSample] = signal_filtered_Fast[iSample] / signal_filtered_Slow[iSample];
        }

        return ratio;
    }

    protected float[] filter(float[] signal, float[] coeff_b, float[] coeff_a, int band, int chan, int fast_slow) {

        /**
         *  single channel IIR filter
         *
         *  Input format of signal is float[blocklen]
         *
         *  coeff_b is b[0]
         *  coeff_a is a[1]; a[0] = 1
         *
         *  band: [lp, bp, hp]
         *  chan: [1, 2] (L, R)
         *  fast_slow = [0, 1] either fast or slow
         *
         *  (layout to account for state memory)
         *
         */

        float[] output = new float[signal.length];
        float tmp;

        if (fast_slow == 0) {
            for (int iSample = 0; iSample < signal.length; iSample++) {
                tmp = coeff_b[band] * signal[iSample] - coeff_a[band] * energyRatio_State_Fast[band][chan];
                output[iSample] = tmp;
                energyRatio_State_Fast[band][chan] = tmp;
            }
        } else{
            for (int iSample = 0; iSample < signal.length; iSample++) {
                tmp = coeff_b[band] * signal[iSample] - coeff_a[band] * energyRatio_State_Slow[band][chan];
                output[iSample] = tmp;
                energyRatio_State_Slow[band][chan] = tmp;
            }
        }

        return output;
    }

}


