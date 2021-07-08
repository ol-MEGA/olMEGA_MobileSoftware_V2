package com.iha.olmega_mobilesoftware_v2.Core;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.BuildConfig;
import com.iha.olmega_mobilesoftware_v2.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

/**
 * Created by ulrikkowalk on 23.03.17.
 */

public class FileIO {

    public static final String FOLDER_MAIN = "olMEGA";
    private static final String FOLDER_DATA = "data";
    private static final String FOLDER_QUEST = "quest";
    private static final String FOLDER_CALIB = "calibration";
    private static final String FILE_NAME = "questionnairecheckboxgroup.xml";
    private static final String LOG = "FileIO";
    // File the system looks for in order to show preferences, needs to be in main directory
    private static final String FILE_CONFIG = "config";
    private static final String FORMAT_QUESTIONNAIRE = ".xml";
    private static final String FILE_TEMP = "copy_questionnaire_here";
    private boolean isVerbose = false;
    private Context mContext;

    // Create / Find main Folder
    public static String getFolderPath() {
        final File baseDirectory;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            baseDirectory = new File(MainActivity.getAppContext().getExternalFilesDir(null) + File.separator + ".");
        else
            baseDirectory = new File(Environment.getExternalStorageDirectory() + File.separator + FOLDER_MAIN);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }
        return baseDirectory.getAbsolutePath();
    }

    public boolean setupFirstUse(Context context) {

        mContext = context;

        String[] string = scanQuestOptions();
        if (string == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean checkConfigFile() {
        // If for whatever reason rules.ini exists, preferences are shown
        if (scanConfigMode()) {
            //deleteConfigFile();
            return true;
        }
        return false;
    }

    // Check whether preferences unlock file is present in main directory
    private boolean scanConfigMode() {
        File fileConfig = new File(getFolderPath() + File.separator + FILE_CONFIG);

        //new SingleMediaScanner(mContext, fileConfig);
        return fileConfig.exists();
    }

    private boolean deleteConfigFile() {
        File fileConfig = new File(getFolderPath() + File.separator + FOLDER_DATA +
                File.separator + FILE_CONFIG);
        return fileConfig.delete();
    }

    public float[] obtainCalibration() {

        // Obtain working Directory
        File dir = new File(getFolderPath() + "/" + FOLDER_CALIB);
        // Address Basis File in working Directory
        File file = new File(dir, "calib.txt");

        float[] calib = new float[] {0.0f, 0.0f};

        try {
            Scanner sc = new Scanner(file);
            // we just need to use \\Z as delimiter
            sc.useDelimiter(" ");

            calib[0] = Float.valueOf(sc.next());
            calib[1] = Float.valueOf(sc.next());

            for (int i = 0; i < 2; i++) {
                Log.e(LOG, "CALIB: " + calib[i]);
            }

        } catch (Exception e) {}

        return calib;

    }

    public boolean scanForQuestionnaire(String questName) {

        // Scan quest folder (not the nicest way)
        try {
            Runtime.getRuntime().exec("am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard/IHAB/quest");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Obtain working Directory
        File dir = new File(getFolderPath() + File.separator + FOLDER_QUEST);
        if (!dir.exists())
            dir.mkdirs();
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(FORMAT_QUESTIONNAIRE);
            }
        });

        if (files != null) {
            for (int iFile = 0; iFile < files.length; iFile++) {
                if (files[iFile].getName().equals(questName)) {
                    //Log.e(LOG, "Quest file found: " + files[iFile].getName());
                    return true;
                }
            }
        }

        return false;
    }

    // Scan "quest" directory for present questionnaires
    public String[] scanQuestOptions() {

        // Scan quest folder (not the nicest way)
        try {
            Runtime.getRuntime().exec("am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard/IHAB/quest");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: Validate files
        // Obtain working Directory
        File dir = new File(getFolderPath() + File.separator + FOLDER_QUEST);
        // Temporary file targeted by MediaScanner
        File tmp = new File(getFolderPath() + File.separator + FOLDER_QUEST + File.separator + FILE_TEMP);

        if (!dir.exists()) {
            dir.mkdirs();
            try {
                tmp.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //new SingleMediaScanner(mContext, tmp);
            //File fileLog = new File(getFolderPath() + File.separator + ControlService.FILENAME_LOG);
            //new SingleMediaScanner(mContext, fileLog);
        }

        // Scan for files of type XML
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(FORMAT_QUESTIONNAIRE);
            }
        });
        String[] fileList = new String[0];
        if (files != null)
            fileList = new String[files.length];

        try {
            if (fileList.length == 0) {
                return null;
            }

            for (int iFile = 0; iFile < files.length; iFile++) {
                fileList[iFile] = files[iFile].getName();
            }
            return fileList;

        } catch (Exception e) {
            Log.i(LOG,""+e.toString());
            return null;
        }
    }

    // Offline version reads XML Basis Sheet from Raw Folder
    public String readRawTextFile(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader buffReader = new BufferedReader(inputReader);
        String line;
        StringBuilder text = new StringBuilder();
        boolean isComment = false;
        try {
            while ((line = buffReader.readLine()) != null) {

                if (line.trim().startsWith("/*")) {
                    isComment = true;
                }

                if (!line.trim().isEmpty() && !line.trim().startsWith("//") && !isComment) {
                    text.append(line);
                    text.append('\n');
                } else {
                    if (isVerbose) {
                        Log.i(LOG, "Dropping line: " + line.trim());
                    }
                }
                if (!line.trim().startsWith("//") && line.split(" //").length > 1) {
                    text.append(line.split(" //")[0].trim());
                    if (isVerbose) {
                        Log.i(LOG, "Dropping part: " + line.split(" //")[1].trim());
                    }
                }

                if (line.trim().endsWith("*/")) {
                    isComment = false;
                }

            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    // Online with dynamic filename
    public String readRawTextFile(String fileName) {

        try {
            // Obtain working Directory
            File dir = new File(getFolderPath() + "/" + FOLDER_QUEST);
            // Address Basis File in working Directory
            File file = new File(dir, fileName);

            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String line;
            StringBuilder text = new StringBuilder();
            boolean isComment = false;
            try {
                while ((line = buffReader.readLine()) != null) {

                    if (line.trim().startsWith("/*")) {
                        isComment = true;
                    }

                    if (!line.trim().isEmpty() && !line.trim().startsWith("//") && !isComment) {
                        text.append(line);
                        text.append('\n');
                    } else {
                        if (isVerbose) {
                            Log.i(LOG, "Dropping line: " + line.trim());
                        }
                    }
                    if (!line.trim().startsWith("//") && line.split(" //").length > 1) {
                        text.append(line.split(" //")[0].trim());
                        if (isVerbose) {
                            Log.i(LOG, "Dropping part: " + line.split(" //")[1].trim());
                        }
                    }

                    if (line.trim().endsWith("*/")) {
                        isComment = false;
                    }
                }
            } catch (IOException e) {
                return null;
            }

            inputStream.close();
            return text.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Online with predefined filename
    public String readRawTextFile() {

        try {
            // Obtain working Directory
            File dir = new File(getFolderPath() + "/" + FOLDER_QUEST);
            // Address Basis File in working Directory
            File file = new File(dir, FILE_NAME);

            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String line;
            StringBuilder text = new StringBuilder();
            boolean isComment = false;
            try {
                while ((line = buffReader.readLine()) != null) {

                    if (line.trim().startsWith("/*")) {
                        isComment = true;
                    }

                    if (!line.trim().isEmpty() && !line.trim().startsWith("//") && !isComment) {
                        text.append(line);
                        text.append('\n');
                    } else {
                        if (isVerbose) {
                            Log.i(LOG, "Dropping line: " + line.trim());
                        }
                    }
                    if (!line.trim().startsWith("//") && line.split(" //").length > 1) {
                        text.append(line.split(" //")[0].trim());
                        if (isVerbose) {
                            Log.i(LOG, "Dropping part: " + line.split(" //")[1].trim());
                        }
                    }

                    if (line.trim().endsWith("*/")) {
                        isComment = false;
                    }
                }
            } catch (IOException e) {
                return null;
            }

            inputStream.close();
            return text.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public File saveDataToFile(Context context, String filename, String data) {

        //MediaScannerConnection mMs = new MediaScannerConnection(context, this);
        //mMs.connect();

        String sFileName = filename;

        // Obtain working Directory
        File dir = new File(getFolderPath() + "/" + FOLDER_DATA + "/");
        // Address Basis File in working Directory
        File file = new File(dir, sFileName);

        Log.e(LOG, "" + dir);

        // Make sure the path directory exists.
        if (!dir.exists()) {
            dir.mkdirs();
            if (BuildConfig.DEBUG) {
                Log.i(LOG, "Directory created: " + dir);
            }
        }

        String stringToSave = data;

        if (BuildConfig.DEBUG) {
            Log.e(LOG, "writing to File: " + file.getAbsolutePath());
        }

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            myOutWriter.append(stringToSave);

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            new SingleMediaScanner(context, file);

            if (BuildConfig.DEBUG) {
                Log.i(LOG, "Data successfully written.");
            }
            return file;
        } catch (IOException e) {

            if (BuildConfig.DEBUG) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
        return null;
    }

    public boolean saveDataToFileOffline(Context context, String filename, String data) {

        String sFileName = filename;

        // Obtain working Directory
        File dir = new File("C:/Users/ul1021/Desktop/data");
        // Address Basis File in working Directory
        File file = new File(dir, sFileName);

        Log.i(LOG, file.getAbsolutePath());

        // Make sure the path directory exists.
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                if (BuildConfig.DEBUG) {
                    Log.i(LOG, "Directory created: " + dir);
                }

                String stringToSave = data;

                if (BuildConfig.DEBUG) {
                    Log.i(LOG, "writing to File: " + file.getAbsolutePath());
                }

                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

                    myOutWriter.append(stringToSave);
                    myOutWriter.close();

                    fOut.flush();
                    fOut.close();

                    new SingleMediaScanner(context, file);

                    if (BuildConfig.DEBUG) {
                        Log.i(LOG, "Data successfully written.");
                    }
                    return true;
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Log.e("Exception", "File write failed: " + e.toString());
                    }
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(LOG, "Unable to create directory. Shutting down.");
                }
            }
        }
        return false;
    }
}
