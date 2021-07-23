package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.os.Environment;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO;
import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.NetworkIO;
import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.SingleMediaScanner;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

/**
 * Write feature data to disk
 *
 * - Per default the writer assumes that incoming data represents one frame.
 *   If the array is a multidimensional, the data is concatenated.
 *   TODO: Check if allocating for each small buffer is problematic of if it is reasonable to cache
 *     small buffers (e.g. RMS).
 *     Alternatively, all features could be required to concatenate before sending, so the number of
 *     frames can be calculated from the incoming array size.
 *
 * - Length of a feature file is determined by time, corresponding to e.g. 60 seconds of audio data.
 *
 * - Timestamp calculation is based on time set in Stage and relative block sizes. Implement
 *   sanity check to compare calculated to actual time? Take into account the delay of the
 *   processing queue?
 */

public class StageFeatureWrite extends Stage {


    private static final String EXTENSION = ".feat";

    private File featureFile = null;
    private RandomAccessFile featureRAF = null;

    private Instant startTime;
    private Instant currentTime;

    private String timestamp;
    private String feature;

    private int isUdp;
    private int nFeatures;
    private int blockCount;
    private int bufferSize;
    protected int mySamplingRate; // nedded because subsampling

    private float hopDuration;
    private float[] relTimestamp;

    private int inStage_hopSizeOut, inStage_blockSizeOut;

    private float featFileSize = 60; // size of feature files in seconds.

    DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    DateTimeFormatter timeFormatUdp =
            DateTimeFormatter.ofPattern("HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());


    public StageFeatureWrite(HashMap parameter) {
        super(parameter);

        feature = (String) parameter.get("prefix");

        if (parameter.get("udp") == null)
            isUdp = 0;
        else
            isUdp = Integer.parseInt((String) parameter.get("udp"));
        mySamplingRate = samplingrate;
    }

    @Override
    void start(){

        inStage_hopSizeOut = inStage.hopSizeOut;
        inStage_blockSizeOut = inStage.blockSizeOut;

        startTime = Stage.startTime;
        currentTime = startTime;
        relTimestamp = new float[]{0, 0};
        openFeatureFile();

        super.start();
    }

    void startWithoutThread(){
        inStage_hopSizeOut = inStage.hopSizeOut;
        inStage_blockSizeOut = inStage.blockSizeOut;
        startTime = Stage.startTime;
        currentTime = startTime;
        relTimestamp = new float[]{0, 0};
        openFeatureFile();
    }

    @Override
    protected void cleanup() {

        closeFeatureFile();
        super.cleanup();
    }

    void rebuffer() {

        // we do not want rebuffering in a writer stage, just get the data and and pass it on.

        boolean abort = false;

        Log.d(LOG, "----------> " + id + ": Start processing");

        while (!Thread.currentThread().isInterrupted() & !abort) {

            if (hasInQueue()) {
                float[][] data = receive();

                if (data != null) {
                    process(data);
                } else {
                    abort = true;
                }
            }
        }

        closeFeatureFile();

        Log.d(LOG, id + ": Stopped consuming");
    }

    protected void process(float[][] data) {
        appendFeature(data);
    }


    private void openFeatureFile() {

        File directory = new File(AudioFileIO.FEATURE_FOLDER);
        if (!directory.exists()) {
            directory.mkdir();
        }

        if (featureRAF != null) {
            closeFeatureFile();
        }

        // add length of last feature file to current time
        currentTime = currentTime.plusMillis((long) (relTimestamp[1]*1000));
        timestamp = timeFormat.format(currentTime);

        try {

            featureFile = new File(directory +
                    "/" + feature + "_" + timestamp + EXTENSION);
            featureRAF = new RandomAccessFile(featureFile, "rw");

            // write header
            featureRAF.writeInt(2);               // Feature File Version
            featureRAF.writeInt(0);               // block count, written on close
            featureRAF.writeInt(0);               // feature dimensions, written on close
            featureRAF.writeInt(inStage_blockSizeOut);  // [samples]
            featureRAF.writeInt(inStage_hopSizeOut);    // [samples]

            featureRAF.writeInt(mySamplingRate);

            featureRAF.writeBytes(timestamp.substring(2));  // YYMMDD_HHMMssSSS, 16 bytes (absolute timestamp)

            featureRAF.writeFloat((float)0.0);      // calibration value 1, written on close
            featureRAF.writeFloat((float)0.0);      // calibration value 2, written on close

            blockCount = 0;
            relTimestamp[0] = 0;

            hopDuration = (float) inStage_hopSizeOut / mySamplingRate;
            relTimestamp[1] = (float) inStage_blockSizeOut / mySamplingRate;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void  appendFeature(float[][] data) {

        //System.out.println("timestamp: " + relTimestamp[1] + " | size: " + featFileSize);

        // start a new feature file?
        if (relTimestamp[1] >= featFileSize) {
            // Update timestamp based on samples processed. This only considers block- and hopsize
            // of the previous stage. If another stage uses different hopping, averaging or any
            // other mechanism to obscure samples vs. time, this has to be tracked elsewhere!
            openFeatureFile();
        }

        // calculate buffer size from actual data to take care of jagged arrays (e.g. PSDs):
        // (samples in data + 2 timestamps) * 4 bytes
        if (bufferSize == 0) {
            nFeatures = 2; // timestamps
            for (float[] aData : data) {
                //Log.d(LOG, "LENGTH: " + aData.length);
                nFeatures += aData.length;
            }
            bufferSize = nFeatures * 4; // 4 bytes to a float
        }

        ByteBuffer bbuffer = ByteBuffer.allocate(bufferSize);
        FloatBuffer fbuffer = bbuffer.asFloatBuffer();

        fbuffer.put(relTimestamp);

        // send UDP packets. Only passes the first array!
        if ((isUdp == 1) && (data[0][0] == 1)) {
            NetworkIO.sendUdpPacket(timeFormatUdp.format(currentTime.plusMillis((long) (relTimestamp[0] * 1000))));
        }

        for (float[] aData : data) {
            fbuffer.put(aData);
        }

        // round to 4 decimals -> milliseconds * 10^-1.
        relTimestamp[0] = Math.round((relTimestamp[0] + hopDuration) * 10000.0f) / 10000.0f;
        relTimestamp[1] = Math.round((relTimestamp[1] + hopDuration) * 10000.0f) / 10000.0f;

        try {
            if (featureRAF != null)
                featureRAF.getChannel().write(bbuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        blockCount++;
    }


    synchronized private void closeFeatureFile() {
        try {
            float[] calibValuesInDB = new float[]{0, 0};
            Stage tempStage = this;
            while (tempStage != null) {
                tempStage = tempStage.inStage;
                if (tempStage != null && tempStage.getClass() == StageRFCOMM.class && !Float.isNaN(((StageRFCOMM)tempStage).calibValuesInDB[0]) && !Float.isNaN(((StageRFCOMM)tempStage).calibValuesInDB[1])) {
                    calibValuesInDB = ((StageRFCOMM)tempStage).calibValuesInDB.clone();
                    //Log.d(LOG, "calibValues: " + calibValuesInDB[0] + ", " + calibValuesInDB[1]);
                    break;
                }
            }
            if (featureRAF != null) {
                featureRAF.seek(4);
                featureRAF.writeInt(blockCount); // block count for this feature file
                featureRAF.writeInt(nFeatures);  // features + timestamps per block
                featureRAF.seek(40);
                featureRAF.writeFloat(calibValuesInDB[0]);
                featureRAF.writeFloat(calibValuesInDB[1]);
                featureRAF.close();
                if (blockCount == 0 && nFeatures == 0)
                    featureFile.delete();
                featureRAF = null;
            }
            new SingleMediaScanner(context, featureFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
