package com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes;

import android.util.Log;

/**
 * Created by ulrikkowalk on 31.03.17.
 */

public class StartAndStop {

    private String TAG = this.getClass().getSimpleName();
    private int Start_hour, Start_minute, Stop_hour, Stop_minute;

    public StartAndStop(int start_hour, int start_minute, int stop_hour, int stop_minute) {
        Start_hour = start_hour;
        Start_minute = start_minute;
        Stop_hour = stop_hour;
        Stop_minute = stop_minute;

        Log.i(TAG, "Entry set: " + Start_hour + ":" + Start_minute + ", " + Stop_hour + ":" + Stop_minute);
    }

    public int getStart_Hour() { return Start_hour; }

    public int getStart_minute() { return Start_minute; }

    public int getStop_Hour() { return Stop_hour; }

    public int getStop_minute() { return Stop_minute; }
}
