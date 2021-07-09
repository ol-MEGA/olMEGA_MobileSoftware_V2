package com.iha.olmega_mobilesoftware_v2.AFEx.Tools;

import android.os.Environment;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Core.FileIO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AudioFileIO {

    protected static final String LOG = "IOClass";

    public static final String MAIN_FOLDER = FileIO.getFolderPath();
    public static final String CACHE_FOLDER = MAIN_FOLDER + File.separator + "cache";
    public static final String FEATURE_FOLDER = MAIN_FOLDER + File.separator + "features";
    public static final String CACHE_WAVE = "wav";
    public static final String CACHE_RAW = "raw";
    public static final String STAGE_CONFIG = "features.xml";

    public String filename;

    int samplerate = 0;
    int channels = 0;
    int format = 0;
    boolean isWave = false;

    File file = null;
    DataOutputStream stream = null;

    public AudioFileIO(String filename) {
        this.filename = filename;
    }

    // main folder
    public static String getMainPath() {
        File directory = new File(MAIN_FOLDER);
        if (!directory.exists()) {
            directory.mkdir();
        }
        return directory.getAbsolutePath();
    }

    // cache folder
    public String getCachePath() {
        File baseDirectory = new File(CACHE_FOLDER);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdir();
        }
        return baseDirectory.getAbsolutePath();
    }

    // build filename
    public String getFilename(boolean wavHeader) {

        String tmp = filename;

        if (filename == null) {
            tmp = Timestamp.getTimestamp(3);
        }

        String filename = new StringBuilder()
                .append(getCachePath())
                .append(File.separator)
                .append(tmp)
                .append(".")
                .append(getExtension(wavHeader))
                .toString();

        return filename;
    }

    // file extension depending on format
    public String getExtension(Boolean isWave) {

        return isWave ? CACHE_WAVE : CACHE_RAW;
    }

    // open output stream w/o filename
    public DataOutputStream openDataOutStream(int _samplerate, int _channels, int _format, boolean _isWave) {

        samplerate = _samplerate;
        channels = _channels;
        format = _format;
        isWave = _isWave;

        filename = getFilename(isWave);
        file = new File(filename);

        return openFileStream();
    }

    public DataOutputStream openFileStream() {

        try {
            FileOutputStream os = new FileOutputStream(file, false);
            stream = new DataOutputStream(new BufferedOutputStream(os));

            // Write zeros. This will be filled with a proper header on close.
            // Alternatively, FileChannel might be used.
            if (isWave) {
                int nBytes = 44; // length of the WAV (RIFF) header
                byte[] zeros = new byte[nBytes];
                for (int i = 0; i < nBytes; i++) {
                    zeros[i] = 0;
                }
                stream.write(zeros);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream;

    }

    // close the output stream
    public void closeDataOutStream() {
        try {
            stream.flush();
            stream.close();

            if (isWave) {
                writeWavHeader();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // open input stream
    public FileInputStream openInputStream(String filepath) {
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return inputStream;
    }

    // close the input stream
    public void closeInStream(BufferedInputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // delete a file
    public static boolean deleteFile(String filename) {

        boolean success;

        File file = new File(filename);

        if (file.exists()) {
            success = file.delete();
            if (!success) {
                Log.d(LOG, "Failed to delete " + filename);
            }
        } else {
            Log.d(LOG, "Failed to delete " + filename + ": File does not exist");
            success = false;
        }

        return success;
    }

    // write WAV (RIFF) header
    public void writeWavHeader() {

        byte[] GROUP_ID = "RIFF".getBytes();
        byte[] RIFF_TYPE = "WAVE".getBytes();
        byte[] FORMAT_ID = "fmt ".getBytes();
        byte[] DATA_ID = "data".getBytes();
        short FORMAT_TAG = 1; // PCM
        int FMT_LENGTH = 16;

        short bitsize = 16; // TODO

        try {

            RandomAccessFile raFile = new RandomAccessFile(file, "rw");

            int fileLength = (int) raFile.length(); // [bytes]
            int chunkSize = fileLength - 8;
            int dataSize = fileLength - 44;
            short blockAlign = (short) ((channels) * (bitsize % 8));
            int bytesPerSec = samplerate * blockAlign;

            // RIFF-Header
            raFile.write(GROUP_ID);
            raFile.writeInt(Integer.reverseBytes(chunkSize));
            raFile.write(RIFF_TYPE);

            // fmt
            raFile.write(FORMAT_ID);
            raFile.writeInt(Integer.reverseBytes(FMT_LENGTH));
            raFile.writeShort(Short.reverseBytes(FORMAT_TAG));
            raFile.writeShort(Short.reverseBytes((short) channels));
            raFile.writeInt(Integer.reverseBytes(samplerate));
            raFile.writeInt(Integer.reverseBytes(bytesPerSec));
            raFile.writeShort(Short.reverseBytes(blockAlign));
            raFile.writeShort(Short.reverseBytes(bitsize));

            // data
            raFile.write(DATA_ID);
            raFile.writeInt(Integer.reverseBytes(dataSize));

            raFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
