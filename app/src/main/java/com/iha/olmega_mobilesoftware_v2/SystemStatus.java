package com.iha.olmega_mobilesoftware_v2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction.StageManager;
import com.iha.olmega_mobilesoftware_v2.Core.FileIO;
import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.Core.XMLReader;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

class AcitivyStates {
    public boolean isCharging = false;
    public boolean questionaireEnabled = false;
    public boolean isAutomaticQuestionaireActive = false;
    public String InfoText = "";
    public String NextQuestText = "";
    public BatteryStates BatteryState = BatteryStates.undefined;
    public float batteryLevel = -1.0f;
    public States profileState = States.undefined;
    public String InputProfile = "";
}

public class SystemStatus {

    private String TAG = this.getClass().getSimpleName();
    private ControlService mContext;
    private BroadcastReceiver mReceiver;
    private int[] batteryStates;
    private StageManager stageManager;
    private StageManagerStates stageMangerState = StageManagerStates.undefined;
    private SystemStatusListener mySystemStatusListener;
    private long raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
    private BroadcastReceiver mStageStateReceiver;
    private Preferences preferences;
    private int BatteryManagerStatus = -1;
    private boolean lockUntilStageManagerIsRunning = false;
    private Handler taskHandler = new Handler(Looper.myLooper());
    private ActiviyRequestCode curentActivity = ActiviyRequestCode.MainActivity;

    private AcitivyStates acitivyStates = new AcitivyStates();

    private File getStageMangerConfigFile() {
        return new File(AFExConfigFolder + File.separator + Preferences().inputProfile());
    }

    public Preferences Preferences() {
        return preferences;
    }

    public static File AFExConfigFolder = new File(FileIO.getFolderPath() + File.separator + "AFEx");

    public SystemStatus(ControlService context) {
        mContext = context;
        mySystemStatusListener = null;
        preferences = new Preferences(context.getApplicationContext());
        batteryStates = mContext.getResources().getIntArray(R.array.batteryStates);
        if (mReceiver != null)
            mContext.unregisterReceiver(mReceiver);
        mReceiver = new BatteryBroadcastReceiver();
        mContext.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mStageStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "receiveBroadcast: " + action);
                switch (action) {
                    case "StageState":
                        if (stageManager != null && stageManager.isRunning && curentActivity != ActiviyRequestCode.PreferencesActivity) {
                            acitivyStates.profileState = States.values()[intent.getIntExtra("currentState", States.connected.ordinal())];
                            Refresh();
                        }
                        break;
                }
            }
        };
        taskHandler.postDelayed(AutomaticQuestionnaireRunnable, 1000);
        IntentFilter filter = new IntentFilter("StageState");
        filter.setPriority(999);
        mContext.registerReceiver(mStageStateReceiver, filter);
    }

    public void setSystemStatusListener(SystemStatusListener listener) {
        this.mySystemStatusListener = listener;
        Refresh();
    }

    public void Refresh() {
        Runnable runnable = new Runnable() {
            @Override
            public synchronized void run() {
                if (lockUntilStageManagerIsRunning == false) {
                    acitivyStates.InfoText = mContext.getResources().getString(R.string.pleaseWait);
                    acitivyStates.NextQuestText = "";
                    acitivyStates.questionaireEnabled = false;
                    preferences.configHasErrors = false;

                    acitivyStates.isCharging = (BatteryManagerStatus == BatteryManager.BATTERY_STATUS_CHARGING || BatteryManagerStatus == BatteryManager.BATTERY_STATUS_FULL);
                    acitivyStates.BatteryState = (acitivyStates.batteryLevel <= batteryStates[1] ? BatteryStates.Critical : acitivyStates.batteryLevel >= batteryStates[1] && acitivyStates.batteryLevel <= batteryStates[0] ? BatteryStates.Warning : BatteryStates.Normal);
                    // Charging State
                    if ((!preferences.isAdmin() && (!Preferences().isInKioskMode && Preferences().isKioskModeNecessary())) || (acitivyStates.isCharging && Preferences().usbCutsConnection()) || (curentActivity == ActiviyRequestCode.PreferencesActivity)){
                        raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
                        if (curentActivity != ActiviyRequestCode.PreferencesActivity) {
                            acitivyStates.InfoText = "Unable to start Kiosk-Mode...\n\nPlease check DeviceOwner Settings! ";
                            preferences.configHasErrors = true;
                        }
                        if (acitivyStates.isCharging && Preferences().usbCutsConnection())
                            acitivyStates.InfoText = mContext.getResources().getString(R.string.infoCharging);
                        stageMangerState = StageManagerStates.undefined;
                        acitivyStates.profileState = States.undefined;
                        acitivyStates.InputProfile = "";
                        if (stageManager != null && stageManager.isRunning)
                            stageManager.stop();
                    }
                    else if (!new FileIO().scanForQuestionnaire(preferences.selectedQuest()) && preferences.useQuestionnaire()) {
                        raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
                        acitivyStates.InfoText = mContext.getResources().getString(R.string.noQuestionnaires);
                        preferences.configHasErrors = true;
                    }
                    else if (stageManager == null || !stageManager.isRunning || !acitivyStates.InputProfile.equals(preferences.inputProfile()) && curentActivity != ActiviyRequestCode.PreferencesActivity) {
                        acitivyStates.InputProfile = "";
                        acitivyStates.profileState = States.undefined;
                        if (getStageMangerConfigFile().exists() && getStageMangerConfigFile().isFile()) {
                            try {
                                acitivyStates.InputProfile = preferences.inputProfile();
                                acitivyStates.profileState = States.connected;
                                stageMangerState = StageManagerStates.running;
                                startStageManager();
                            } catch (Exception e) {
                                StringWriter sw = new StringWriter();
                                e.printStackTrace(new PrintWriter(sw));
                                LogIHAB.log(sw.toString());
                                stageMangerState = StageManagerStates.ConfigFileNotValid;
                                e.printStackTrace();
                            }
                        } else
                            stageMangerState = StageManagerStates.noConfigSelected;
                    }
                    switch (stageMangerState) {
                        case undefined:
                            break;
                        case ConfigFileNotValid:
                            acitivyStates.profileState = States.undefined;
                            acitivyStates.InfoText = "Stage Manager Config-File '" + getStageMangerConfigFile().getAbsoluteFile() + "' not valid!";
                            preferences.configHasErrors = true;
                            break;
                        case noConfigSelected:
                            acitivyStates.profileState = States.undefined;
                            acitivyStates.InfoText = "No Input Profile Config selected!";
                            preferences.configHasErrors = true;
                            break;
                        case running:
                            break;
                    }

                    if (curentActivity == ActiviyRequestCode.MainActivity && stageManager != null && stageManager.isRunning && acitivyStates.profileState != States.undefined) {
                        switch (acitivyStates.profileState) {
                            case init:
                                acitivyStates.InfoText = "Initializing";
                                break;
                            case connecting:
                                acitivyStates.InfoText = mContext.getResources().getString(R.string.infoConnecting);
                                break;
                            case connected:
                                acitivyStates.InfoText = mContext.getResources().getString(R.string.infoConnected);
                                if (preferences.useQuestionnaire()) {
                                    acitivyStates.questionaireEnabled = true;
                                    acitivyStates.InfoText = mContext.getResources().getString(R.string.menuText);
                                    if (raiseAutomaticQuestionaire_TimerEventAt == Long.MIN_VALUE) { // Preferences().useQuestionnaireTimer() &&
                                        XMLReader mXmlReader = new XMLReader(mContext, preferences.selectedQuest());
                                        if (mXmlReader.getQuestionnaireHasTimer()) {
                                            raiseAutomaticQuestionaire_TimerEventAt = System.currentTimeMillis() + mXmlReader.getNewTimerInterval() * 1000;
                                        }
                                    }
                                }
                                //acitivyStates.InfoText = mContext.getResources().getString(R.string.pleaseWait);
                                break;
                        }
                    }
                    // Battery State
                    if (acitivyStates.BatteryState == BatteryStates.Critical)
                        acitivyStates.InfoText += "\n\n" + mContext.getResources().getString(R.string.batteryCritical);
                    else if (acitivyStates.BatteryState == BatteryStates.Warning)
                        acitivyStates.InfoText += "\n\n" + mContext.getResources().getString(R.string.batteryWarning);
                }
                acitivyStates.isAutomaticQuestionaireActive = (raiseAutomaticQuestionaire_TimerEventAt != Long.MIN_VALUE && acitivyStates.profileState == States.connected) ;
                if (mySystemStatusListener != null)
                    mySystemStatusListener.setAcitivyStates(acitivyStates);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void ResetAutomaticQuestionaireTimer() {
        raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
        Refresh();
    }
    private void startStageManager() {
        lockUntilStageManagerIsRunning = true;
        if (stageManager != null && stageManager.isRunning)
            stageManager.stop();
        stageManager = new StageManager(mContext, getStageMangerConfigFile());
        try {
            stageManager.start();
        } catch (Exception e) {
            if (stageManager != null && stageManager.isRunning)
                stageManager.stop();
            throw e;
        } finally {
            lockUntilStageManagerIsRunning = false;
        }
    }

    public void setActiveActivity(ActiviyRequestCode activity) {
        curentActivity = activity;
        this.Refresh();
    }

    private Runnable AutomaticQuestionnaireRunnable = new Runnable() {
        public void run() {
            if (mySystemStatusListener != null) {
                if (acitivyStates.isAutomaticQuestionaireActive) {
                    String mCountDownString = mContext.getResources().getString(R.string.timeRemaining);
                    String[] mTempTextCountDownRemaining = mCountDownString.split("%");
                    long remaining = (raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis()) / 1000;
                    long hoursRemaining = remaining / 60 / 60;
                    long minutesRemaining = (remaining - hoursRemaining * 60 * 60) / 60;
                    long secondsRemaining = remaining - hoursRemaining * 60 * 60 - minutesRemaining * 60;
                    if (preferences.showQuestionnaireTimer()) {
                        acitivyStates.NextQuestText = String.format("%s%02d%s%02d%s%02d%s",
                                mTempTextCountDownRemaining[0], hoursRemaining,
                                mTempTextCountDownRemaining[1], minutesRemaining,
                                mTempTextCountDownRemaining[2], secondsRemaining,
                                mTempTextCountDownRemaining[3]);
                    }
                    else
                        acitivyStates.NextQuestText = "";
                    mySystemStatusListener.updateAutomaticQuestionnaireTimer(acitivyStates.NextQuestText, remaining);
                    if (raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() < 10 * 1000 && raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() > 5 * 1000)
                        mContext.startMainActivity(true);
                    else if (raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() <= 0) {
                        mySystemStatusListener.startAutomaticQuestionnaire();
                    }
                }
                else
                    mySystemStatusListener.updateAutomaticQuestionnaireTimer("", Long.MIN_VALUE);
            }
            taskHandler.postDelayed(this, 1000);
        }
    };

/*
    private void startAutomaticQuestionnaireTimer(long timer) {
        if (AutomaticQuestionnaireTimer != null) {
            AutomaticQuestionnaireTimer.cancel();
            AutomaticQuestionnaireTimer = null;
        }
        Looper.prepare();
        AutomaticQuestionnaireTimer = new CountDownTimer(timer, 1000) {
            public void onTick(long millisUntilFinished) {
                Looper.loop();
                Log.d("TIMER", "TEST");
                Refresh();
                if (raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() < 10 * 1000 && raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() > 5 * 1000)
                    mContext.startMainActivity(true);
            }

            public void onFinish() {
                if (mySystemStatusListener != null)
                    mySystemStatusListener.startAutomaticQuestionnaire();
                raiseAutomaticQuestionaire_TimerEventAt = -1;
            }
        }.start();
    }
*/

    public void onDestroy() {
        if (mReceiver != null)
            mContext.unregisterReceiver(mReceiver);
        mContext.unregisterReceiver(mStageStateReceiver);
        taskHandler.removeCallbacks(AutomaticQuestionnaireRunnable);
        if (stageManager != null && stageManager.isRunning)
            stageManager.stop();
        Preferences().onDestroy();
    }

    private class BatteryBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BatteryManagerStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            acitivyStates.batteryLevel = intent.getIntExtra("level", 0);
            Refresh();
        }
    }

    public abstract static class SystemStatusListener {
        public void setAcitivyStates(AcitivyStates acitivyStates) { }
        public void startAutomaticQuestionnaire() { }
        public void updateAutomaticQuestionnaireTimer(String Message, long TimeRemaining) { }
    }

}