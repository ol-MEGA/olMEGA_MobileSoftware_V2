package com.iha.olmega_mobilesoftware_v2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.iha.olmega_mobilesoftware_v2.Core.FileIO;

import java.io.File;
import java.io.Serializable;

public class Preferences {
    SharedPreferences sharedPreferences;

    public boolean isInKioskMode = false;
    public boolean isDeviceOwner = false;
    public boolean configHasErrors = false;
    //private Context mContext = null;

    public Preferences(Context mcontext) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mcontext);
        //sharedPreferences.edit().putBoolean("killAppAndService", false).commit();
        //sharedPreferences.edit().putString("installNewApp", "").commit();
        //mContext = mcontext;
    }

    //public void clearUnsetDeviceAdmin() {sharedPreferences.edit().putBoolean("unsetDeviceAdmin", false).commit();}

    public static File UdaterSettings = new File(FileIO.getFolderPath() + File.separator + "UdaterSettings.xml");

    public boolean isAdmin() {return sharedPreferences.getBoolean("isAdmin", false);}
    public boolean usbCutsConnection() {return sharedPreferences.getBoolean("usbCutsConnection", true);}
    public boolean usbCutsDataStorage() {return sharedPreferences.getBoolean("usbCutsDataStorage", false);}
    public boolean autoStartActivity() {return sharedPreferences.getBoolean("autoStartActivity", true);}
    public boolean forceAnswer() {return sharedPreferences.getBoolean("forceAnswer", true);}
    public boolean isKioskModeNecessary() {return sharedPreferences.getBoolean("isKioskModeNecessary", true);}
    //public boolean forceAnswerDialog() {return sharedPreferences.getBoolean("forceAnswerDialog", true);}
    //public boolean useQuestionnaireTimer() {return sharedPreferences.getBoolean("useQuestionnaireTimer", true);}
    //public boolean unsetDeviceAdmin() {return sharedPreferences.getBoolean("unsetDeviceAdmin", false);}
    //public boolean killAppAndService() {return sharedPreferences.getBoolean("killAppAndService", false);}
    //public String installNewApp() {return sharedPreferences.getString("installNewApp", "");}
    public boolean showQuestionnaireTimer() {return sharedPreferences.getBoolean("showQuestionnaireTimer", true);}
    public boolean useQuestionnaire() {return sharedPreferences.getBoolean("useQuestionnaire", false);}
    public String clientID()  {return sharedPreferences.getString("clientID", "0000");}
    public String selectedQuest()  {return sharedPreferences.getString("selectedQuest", "");}
    public String inputProfile() {
        return sharedPreferences.getString("inputProfile", "");
    }
    public int rebootConnectionFailsTime()  {
        try {
            return Integer.parseInt(sharedPreferences.getString("rebootConnectionFailsTime", "5"));
        } catch(NumberFormatException nfe) {
            return 5;
        }
    }

    public void onDestroy() {
    }
}
