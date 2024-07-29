package com.iha.olmega_mobilesoftware_v2;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction.StageManager;
import com.iha.olmega_mobilesoftware_v2.AFEx.Tools.AudioFileIO;
import com.iha.olmega_mobilesoftware_v2.Core.FileIO;
import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.Core.XMLReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;

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
    public ActiviyRequestCode getCurentActivity() {return curentActivity;}
    private float lastBatteryLevel = 0;
    private boolean AutomaticQuestionaireIsTriggered = false;
    private boolean refreshHandlerIsActive = false;

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
                if (intent.getAction().equals("StageState") && stageManager != null && stageManager.isRunning && curentActivity != ActiviyRequestCode.PreferencesActivity && acitivyStates.profileState != States.values()[intent.getIntExtra("currentState", States.undefined.ordinal())]) {
                    acitivyStates.profileState = States.values()[intent.getIntExtra("currentState", States.undefined.ordinal())];
                    if (acitivyStates.profileState == States.connected) {
                        SharedPreferences prefs = context.getSharedPreferences("olMEGA_MobileSoftware_V2", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("AppRestartForConnection", 0);
                        editor.commit();
                    }
                    Refresh();
                }
                else if (intent.getAction().equals("CalibrationValuesError") && curentActivity != ActiviyRequestCode.PreferencesActivity) {
                    acitivyStates.showCalibrationValuesError = intent.getBooleanExtra("Value", false);
                    Refresh();
                }
            }
        };
        taskHandler.postDelayed(AutomaticQuestionnaireRunnable, 1000);
        IntentFilter filter = new IntentFilter("StageState");
        filter.setPriority(999);
        mContext.registerReceiver(mStageStateReceiver, filter);
        IntentFilter filter2 = new IntentFilter("CalibrationValuesError");
        filter2.setPriority(999);
        mContext.registerReceiver(mStageStateReceiver, filter2);

        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        acitivyStates.batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public void setSystemStatusListener(SystemStatusListener listener) {
        this.mySystemStatusListener = listener;
        Refresh();
    }

    boolean isRFCOMM(NodeList nodes) {
        boolean hasRFCOMM = false;
        for (int i = 0; i < nodes.getLength(); i++) {
            for (int n = 0; n < nodes.item(i).getAttributes().getLength(); n++) {
                if (nodes.item(i).getAttributes().item(n).getNodeValue().equals("StageRFCOMM"))
                    return true;
                else if (hasRFCOMM == false && nodes.item(i).getChildNodes().getLength() > 0)
                    hasRFCOMM = isRFCOMM(nodes.item(i).getChildNodes());
            }
        }
        return hasRFCOMM;
    }

    synchronized public void Refresh() {
        if (lockUntilStageManagerIsRunning == false) {
            acitivyStates.InfoText = mContext.getResources().getString(R.string.pleaseWait);
            acitivyStates.NextQuestText = "";
            acitivyStates.questionaireEnabled = false;
            preferences.configHasErrors = false;
            boolean WriteDataToStorage = true;

            acitivyStates.isCharging = (BatteryManagerStatus == BatteryManager.BATTERY_STATUS_CHARGING || BatteryManagerStatus == BatteryManager.BATTERY_STATUS_FULL);
            if (acitivyStates.lastChargingState != acitivyStates.isCharging && acitivyStates.isCharging)
                LogIHAB.log("StateCharging");
            acitivyStates.BatteryState = (acitivyStates.batteryLevel <= batteryStates[1] ? BatteryStates.Critical : acitivyStates.batteryLevel >= batteryStates[1] && acitivyStates.batteryLevel <= batteryStates[0] ? BatteryStates.Warning : BatteryStates.Normal);
            // Charging State
            if (curentActivity != ActiviyRequestCode.MainActivity && curentActivity != ActiviyRequestCode.HelpActiviy)
                raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
            if ((!preferences.isAdmin() && (!Preferences().isInKioskMode && Preferences().isKioskModeNecessary())) || (acitivyStates.isCharging && (Preferences().usbCutsConnection() || Preferences().usbCutsDataStorage())) || (curentActivity == ActiviyRequestCode.PreferencesActivity)) {
                if (curentActivity != ActiviyRequestCode.PreferencesActivity && !Preferences().isInKioskMode && Preferences().isKioskModeNecessary()) {
                    acitivyStates.InfoText = mContext.getResources().getString(R.string.UnableToStartKioskMode);
                    preferences.configHasErrors = true;
                }
                if (acitivyStates.isCharging && (Preferences().usbCutsConnection() || Preferences().usbCutsDataStorage()))
                    acitivyStates.InfoText = mContext.getResources().getString(R.string.infoCharging);
                if (acitivyStates.isCharging && Preferences().usbCutsDataStorage() == true)
                    WriteDataToStorage = false;
                else if (stageManager != null && stageManager.isRunning) {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter.isEnabled())
                        mBluetoothAdapter.disable();
                    stageManager.stop();
                }
                stageMangerState = StageManagerStates.undefined;
                acitivyStates.profileState = States.undefined;
                acitivyStates.InputProfile = "";
                acitivyStates.showCalibrationValuesError = false;
                raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
            } else if (!new FileIO().scanForQuestionnaire(preferences.selectedQuest()) && preferences.useQuestionnaire()) {
                raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
                acitivyStates.InfoText = mContext.getResources().getString(R.string.noQuestionnaires);
                preferences.configHasErrors = true;
            } else if (acitivyStates.profileState == States.requestDisconnection || stageManager == null || !stageManager.isRunning || !acitivyStates.InputProfile.equals(preferences.inputProfile()) && curentActivity != ActiviyRequestCode.PreferencesActivity) {
                acitivyStates.InputProfile = "";
                acitivyStates.profileState = States.undefined;
                if (getStageMangerConfigFile().exists() && getStageMangerConfigFile().isFile()) {
                    try {
                        acitivyStates.InputProfile = preferences.inputProfile();
                        stageMangerState = StageManagerStates.running;
                        Document doc = startStageManager();
                        if (isRFCOMM(doc.getChildNodes()))
                            acitivyStates.profileState = States.undefined;
                        else
                            acitivyStates.profileState = States.connected;
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
                        LogIHAB.log("StateConnecting");
                        acitivyStates.InfoText = mContext.getResources().getString(R.string.infoConnecting);
                        break;
                    case connected:
                        LogIHAB.log("StateRunning");
                        acitivyStates.InfoText = mContext.getResources().getString(R.string.infoConnected);
                        if (preferences.useQuestionnaire() && !(acitivyStates.isCharging == true && Preferences().usbCutsDataStorage() == true)) {
                            acitivyStates.questionaireEnabled = true;
                            acitivyStates.InfoText = mContext.getResources().getString(R.string.menuText);
                            if (raiseAutomaticQuestionaire_TimerEventAt == Long.MIN_VALUE) { // Preferences().useQuestionnaireTimer() &&
                                XMLReader mXmlReader = new XMLReader(mContext, preferences.selectedQuest());
                                if (mXmlReader.getQuestionnaireHasTimer())
                                    raiseAutomaticQuestionaire_TimerEventAt = System.currentTimeMillis() + mXmlReader.getNewTimerInterval() * 1000;
                                //else
                                //    raiseAutomaticQuestionaire_TimerEventAt = Long.MAX_VALUE;
                            }
                        }
                        break;
                }
            }
            // Battery State
            if (acitivyStates.BatteryState == BatteryStates.Critical)
                acitivyStates.InfoText += "\n\n" + mContext.getResources().getString(R.string.batteryCritical);
            else if (acitivyStates.BatteryState == BatteryStates.Warning)
                acitivyStates.InfoText += "\n\n" + mContext.getResources().getString(R.string.batteryWarning);
            acitivyStates.isAutomaticQuestionaireActive = (raiseAutomaticQuestionaire_TimerEventAt != Long.MAX_VALUE && raiseAutomaticQuestionaire_TimerEventAt != Long.MIN_VALUE && acitivyStates.profileState == States.connected && curentActivity == ActiviyRequestCode.MainActivity);
            if (mySystemStatusListener != null)
                mySystemStatusListener.setAcitivyStates(acitivyStates);
            lockUntilStageManagerIsRunning = false;
            updateAutomaticQuestionnaireTimer();
            acitivyStates.lastChargingState = acitivyStates.isCharging;
            if (stageManager != null && stageManager.isRunning)
                stageManager.setWriteDataToStorage(WriteDataToStorage);
        } else if (refreshHandlerIsActive == false) {
            refreshHandlerIsActive = true;
            Handler refreshHandler = new Handler(Looper.getMainLooper());
            refreshHandler.postDelayed(new Runnable() {
                public synchronized void run() {
                    if (lockUntilStageManagerIsRunning)
                        refreshHandler.postDelayed(this, 100);
                    else {
                        refreshHandlerIsActive = false;
                        Refresh();
                    }
                }
            }, 100);
        }
    }

    public void ResetAutomaticQuestionaireTimer() {
        raiseAutomaticQuestionaire_TimerEventAt = Long.MIN_VALUE;
        acitivyStates.isAutomaticQuestionaireActive = false;
        Refresh();
    }

    private Document startStageManager() {
        Document doc = null;
        acitivyStates.showCalibrationValuesError = false;
        lockUntilStageManagerIsRunning = true;
        if (stageManager != null && stageManager.isRunning)
            stageManager.stop();
        stageManager = new StageManager(mContext, getStageMangerConfigFile());
        if (getStageMangerConfigFile() != null) {
            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getStageMangerConfigFile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            stageManager.start();
        } catch (Exception e) {
            if (stageManager != null && stageManager.isRunning)
                stageManager.stop();
            throw e;
        }
        return doc;
    }

    public void setActiveActivity(ActiviyRequestCode activity) {
        if (activity == ActiviyRequestCode.QuestionnaireActivity)
            LogIHAB.log("StateQuest");
        curentActivity = activity;
        this.Refresh();
    }

    private Runnable AutomaticQuestionnaireRunnable = new Runnable() {
        public synchronized void run() {
            updateAutomaticQuestionnaireTimer();
            taskHandler.postDelayed(this, 1000);
        }
    };

    private void updateAutomaticQuestionnaireTimer() {
        if (mySystemStatusListener != null) {
            if (acitivyStates.isAutomaticQuestionaireActive && raiseAutomaticQuestionaire_TimerEventAt != Long.MIN_VALUE && raiseAutomaticQuestionaire_TimerEventAt != Long.MAX_VALUE) {
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
                } else
                    acitivyStates.NextQuestText = "";
                mySystemStatusListener.updateAutomaticQuestionnaireTimer(acitivyStates.NextQuestText, remaining);
                if (raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() < 10 * 1000 && raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() > 5 * 1000)
                    mContext.startMainActivity(true);
                else if (raiseAutomaticQuestionaire_TimerEventAt - System.currentTimeMillis() <= 0) {
                    if (AutomaticQuestionaireIsTriggered == false)
                        LogIHAB.log("StateProposing");
                    AutomaticQuestionaireIsTriggered = true;
                } else
                    AutomaticQuestionaireIsTriggered = false;
            } else
                mySystemStatusListener.updateAutomaticQuestionnaireTimer("", Long.MIN_VALUE);
        }
    }


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
            if (lastBatteryLevel != acitivyStates.batteryLevel) {
                LogIHAB.log("battery level: " + acitivyStates.batteryLevel / 100);
                lastBatteryLevel = acitivyStates.batteryLevel;
            }
            Refresh();
        }
    }

    public abstract static class SystemStatusListener {
        public void setAcitivyStates(AcitivyStates acitivyStates) {
        }

        public void updateAutomaticQuestionnaireTimer(String Message, long TimeRemaining) {
        }
    }

    public void writePreferencesToLog() {
        LogIHAB.log("Preferences: ");
        LogIHAB.log("   isAdmin: " + this.preferences.isAdmin());
        LogIHAB.log("   usbCutsConnection: " + this.preferences.usbCutsConnection());
        LogIHAB.log("   usbCutsDataStorage: " + this.preferences.usbCutsDataStorage());
        LogIHAB.log("   autoStartActivity: " + this.preferences.autoStartActivity());
        LogIHAB.log("   inputProfile: " + this.preferences.inputProfile());
        LogIHAB.log("   timeoutForTransmitterNotFoundMessage: " + this.preferences.timeoutForTransmitterNotFoundMessage());
        LogIHAB.log("   isKioskModeNecessary: " + this.preferences.isKioskModeNecessary());
        LogIHAB.log("   isPowerOffAllowed: " + this.preferences.isPowerOffAllowed());
        LogIHAB.log("   useQuestionnaire: " + this.preferences.useQuestionnaire());
        LogIHAB.log("   selectedQuest: " + this.preferences.selectedQuest());
        LogIHAB.log("   forceAnswer: " + this.preferences.forceAnswer());
        LogIHAB.log("   showQuestionnaireTimer: " + this.preferences.showQuestionnaireTimer());
        LogIHAB.log("   clientID: " + this.preferences.clientID());

    }

}