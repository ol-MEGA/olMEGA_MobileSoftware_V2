package com.iha.olmega_mobilesoftware_v2.AFEx.Processing;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utilities {

    public static void normalise(float[][] buffer) {

        for (int channel = 0; channel < buffer.length; channel++) {
            float max = 0f;
            for (float sample : buffer[channel]) {
                if (Math.abs(sample) > max) {
                    max = Math.abs(sample);
                }
            }
            if (max > 0) {
                for (int sample = 0; sample < buffer[channel].length; sample++) {
                    buffer[channel][sample] /= max;
                }
            }
        }
    }

    public static void normaliseToDbFS(float[][] buffer, float dbFS) {
        float target = (float) Math.pow(10.0, dbFS / 20.0);
        for (int channel = 0; channel < buffer.length; channel++) {
            float max = 0f;
            for (float sample : buffer[channel]) {
                if (Math.abs(sample) > max) {
                    max = Math.abs(sample);
                }
            }
            if (max > 0) {
                float factor = target / max;
                for (int sample = 0; sample < buffer[channel].length; sample++) {
                    buffer[channel][sample] *= factor;
                }
            }
        }
    }

    public static int nextpow2(int x) {

        return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
    }

    public static float[] hann(int samples) {

        float[] window = new float[samples];

        // calculate window
        for (int i = 0; i < samples; i++) {
            window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * (float) i / samples - 1));
        }
        return window;
    }

    public static File copyAssetToFile(Context context, String assetName) throws IOException {
        File outFile = new File(context.getFilesDir(), assetName);
        if (!outFile.exists()) { // Only copy once
            try (InputStream is = context.getAssets().open(assetName);
                 OutputStream os = new FileOutputStream(outFile)) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        }
        return outFile;
    }

}
