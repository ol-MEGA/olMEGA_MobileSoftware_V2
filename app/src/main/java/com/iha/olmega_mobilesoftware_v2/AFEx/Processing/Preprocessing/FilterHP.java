package com.iha.olmega_mobilesoftware_v2.AFEx.Processing.Preprocessing;

/*
 * High-Pass Filter
 *
 * Based on Robert Bristow-Johnson's Audio-EQ Cookbook:
 * http://www.musicdsp.org/files/Audio-EQ-Cookbook.txt
 */

public class FilterHP {

    private final float b0;
    private final float b1;
    private final float b2;

    private final float a1;
    private final float a2;

    private float x0;
    private float x1;
    private float x2;
    private float y0;
    private float y1;
    private float y2;


    public FilterHP(int fs, int f0) {

        float w0 = (float) (2.0f * Math.PI * f0 / fs);
        float w0sin = (float) Math.sin(w0);
        float w0cos = (float) Math.cos(w0);
        float q = (float) Math.sin(Math.PI / 4.0f);
        float alpha = w0sin / (2.0f * q);

        float a0 = 1 + alpha;
        b0 = (1 + w0cos) / 2 / a0;
        b1 = -(1 + w0cos) / a0;
        b2 = (1 + w0cos) / 2 / a0;
        a1 = -2 * w0cos / a0;
        a2 = (1 - alpha) / a0;

        reset();
    }

    public void reset() {

        x0 = 0;
        x1 = 0;
        x2 = 0;

        y0 = 0;
        y1 = 0;
        y2 = 0;
    }

    public void filter(float[] data) {

        int nSamples = data.length;

        for (int kk = 0; kk < nSamples; kk++) {

            x2 = x1;
            x1 = x0;
            x0 = data[kk];

            y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

            y2 = y1;
            y1 = y0;
            data[kk] = y0;
        }
    }

}
