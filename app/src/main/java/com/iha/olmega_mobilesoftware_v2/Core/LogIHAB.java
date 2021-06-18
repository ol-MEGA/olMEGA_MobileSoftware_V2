package com.iha.olmega_mobilesoftware_v2.Core;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ul1021 on 03.08.2018.
 */

public class LogIHAB {

    private static String mFileName = "log.txt";
    private static String LOG = "LogIHAB";

    // Write input string to log file
    public static void log(String string) {
        File file = new File(FileIO.getFolderPath() + File.separator + mFileName);
        FileWriter fw = null;
        String formattedString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date()) + " " + string;

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

}
