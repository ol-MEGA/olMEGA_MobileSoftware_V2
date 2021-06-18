package com.iha.olmega_mobilesoftware_v2.Core;

import android.os.Environment;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.Timestamp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by ul1021 on 03.08.2018.
 */

public class LogIHAB {

    private static String mFileName = "log2.txt";
    private static String LOG = "LogIHAB";
    private static String FOLDER_MAIN = FileIO.FOLDER_MAIN;

    // Write input string to log file
    public static void log(String string) {

        File file = new File(getFolderPath() + File.separator + mFileName);
        FileWriter fw = null;

        String formattedString = Timestamp.getTimestamp(4) + " " + string;
        //Log.e(LOG, "formatted string: "+formattedString);

        try
        {
            fw = new FileWriter(file, true);
            fw.append(formattedString);
            fw.append(System.getProperty("line.separator") );
        }
        catch ( IOException e ) {
            Log.e(LOG,  "Error writing file." );
        }
        finally {
            if ( fw != null )
                try { fw.close(); } catch ( IOException e ) { e.printStackTrace(); }
        }
    }

    public static void setName(String string) {
        mFileName = string;
    }

    // Create / Find main Folder
    private static String getFolderPath() {
        final File baseDirectory = new File(Environment.getExternalStorageDirectory() +
                File.separator + FOLDER_MAIN);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdir();
        }
        return baseDirectory.getAbsolutePath();
    }

}
