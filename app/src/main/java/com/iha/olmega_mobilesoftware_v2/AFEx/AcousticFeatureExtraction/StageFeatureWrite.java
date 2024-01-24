package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO;
import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.NetworkIO;
import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.SingleMediaScanner;
import com.iha.olmega_mobilesoftware_v2.BuildConfig;
import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;

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

    private int hopDuration;
    private int[] relTimestamp;

    private int inStage_hopSizeOut, inStage_blockSizeOut;

    private float featFileSize = 60; // size of feature files in seconds.

    boolean calibValuesReadingDone = false;
    float[] calibValuesInDB = new float[]{0, 0};
    String[] HardwareIDs = new String[]{String.format("%1$16s", ""), String.format("%1$16s", "")};

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
        calibValuesReadingDone = false;
        inStage_hopSizeOut = inStage.hopSizeOut;
        inStage_blockSizeOut = inStage.blockSizeOut;
        //startTime = Stage.startTime;
        //currentTime = startTime;
        relTimestamp = new int[]{0, 0};
        //openFeatureFile();
        super.start();
    }

    void startWithoutThread(){
        calibValuesReadingDone = false;
        inStage_hopSizeOut = inStage.hopSizeOut;
        inStage_blockSizeOut = inStage.blockSizeOut;
        relTimestamp = new int[]{0, 0};
        //openFeatureFile();
    }

    @Override
    protected void cleanup() {
        calibValuesReadingDone = false;
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
        if (startTime == null) {
            startTime = Stage.startTime;
            currentTime = startTime;
        }
        // add length of last feature file to current time
        currentTime = currentTime.plusMillis((long) ((float)(relTimestamp[0]) / (float)mySamplingRate * 1000.0));
        timestamp = timeFormat.format(currentTime);
        if (WriteDataToStorage) {
            try {
                if (!calibValuesReadingDone) {
                    Stage tempStage = this;
                    while (tempStage != null) {
                        tempStage = tempStage.inStage;
                        if (tempStage != null && tempStage.getClass() == StageRFCOMM.class && !Float.isNaN(((StageRFCOMM)tempStage).calibValuesInDB[0]) && !Float.isNaN(((StageRFCOMM)tempStage).calibValuesInDB[1])) {
                            calibValuesInDB = ((StageRFCOMM)tempStage).calibValuesInDB.clone();
                            HardwareIDs = ((StageRFCOMM)tempStage).HardwareIDs.clone();
                            break;
                        }
                    }
                    calibValuesReadingDone = true;
                }
                featureFile = new File(directory + "/" + feature + "_" + timestamp + EXTENSION);
                LogIHAB.log("openFeatureFile: " + feature + "_" + timestamp + EXTENSION);
                //featureFile = new File(directory + "/" + feature + "_" + timeFormat.format(Instant.now()) + EXTENSION);
                featureRAF = new RandomAccessFile(featureFile, "rw");

                // write header
                featureRAF.writeInt(6);               // Feature File Version
                featureRAF.writeInt(0);               // block count, written on close
                featureRAF.writeInt(0);               // feature dimensions, written on close
                featureRAF.writeInt(inStage_blockSizeOut);  // [samples]
                featureRAF.writeInt(inStage_hopSizeOut);    // [samples]

                featureRAF.writeInt(mySamplingRate);

                featureRAF.writeBytes(timestamp.substring(2));  // YYMMDD_HHMMssSSS, 16 bytes (absolute timestamp)
                featureRAF.writeBytes(timeFormat.format(Instant.now()).substring(2));  // YYMMDD_HHMMssSSS, 16 bytes (absolute timestamp)

                featureRAF.writeFloat(calibValuesInDB[0]);
                featureRAF.writeFloat(calibValuesInDB[1]);
                featureRAF.writeBytes(String.format("%1$16s", HardwareIDs[0]).substring(0, 16));
                featureRAF.writeBytes(String.format("%1$17s", HardwareIDs[1]).substring(0, 17));

                featureRAF.writeFloat((float)-1);      // Transmitter Sample Rate

                featureRAF.writeBytes(String.format("%1$20s", BuildConfig.VERSION_NAME).substring(0, 20));

                blockCount = 0;
                hopDuration = inStage_hopSizeOut;
                relTimestamp[0] = relTimestamp[0] % (int)(featFileSize * mySamplingRate);
                relTimestamp[1] = relTimestamp[0] + (inStage_blockSizeOut - 1);
            } catch (IOException e) {
                LogIHAB.log("StageFeatureWrite: " + e.getStackTrace());
                e.printStackTrace();
            }
        }
    }

    protected void appendFeature(float[][] data) {
        //System.out.println("timestamp: " + relTimestamp[1] + " | size: " + featFileSize);
        // start a new feature file?
        if (featureRAF == null || relTimestamp[0] >= featFileSize * mySamplingRate) {
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

        fbuffer.put(new float[]{(float)relTimestamp[0] / mySamplingRate, (float)relTimestamp[1] / mySamplingRate});

        // send UDP packets. Only passes the first array!
        if ((isUdp == 1) && (data[0][0] == 1)) {
            NetworkIO.sendUdpPacket(timeFormatUdp.format(currentTime.plusMillis((long) ((float)relTimestamp[0] / mySamplingRate * 1000.0))));
        }

        for (float[] aData : data) {
            fbuffer.put(aData);
        }

        // round to 4 decimals -> milliseconds * 10^-1.
        relTimestamp[0] = relTimestamp[0] + hopDuration;
        relTimestamp[1] = relTimestamp[1] + hopDuration;

        try {
            if (featureRAF != null) {
                featureRAF.getChannel().write(bbuffer);
                blockCount++;
            }
        } catch (IOException e) {
            LogIHAB.log("StageFeatureWrite: " + e.getStackTrace());
            e.printStackTrace();
        }
    }

    synchronized private void closeFeatureFile() {
        try {
            if (featureRAF != null) {
                featureRAF.seek(4);
                featureRAF.writeInt(blockCount); // block count for this feature file
                featureRAF.writeInt(nFeatures);  // features + timestamps per block
                featureRAF.close();
                if (blockCount == 0 && nFeatures == 0) {
                    featureFile.delete();
                    LogIHAB.log("StageFeatureWrite: empty File '" + featureFile.getName() + " deleted");
                }
                featureRAF = null;
            }
            new SingleMediaScanner(context, featureFile);
        } catch (IOException e) {
            LogIHAB.log("StageFeatureWrite: " + e.getStackTrace());
            e.printStackTrace();
        }
    }
}
