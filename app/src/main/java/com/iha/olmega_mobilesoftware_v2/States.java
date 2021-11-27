package com.iha.olmega_mobilesoftware_v2;

public enum States {
    undefined,
    init,
    connecting,
    connected
}

enum BatteryStates {
    undefined,
    Normal,
    Warning,
    Critical
}

enum StageManagerStates {
    undefined,
    noConfigSelected,
    ConfigFileNotValid,
    running
}

enum QuestionnaireMotivation {
    manual,
    auto
}

enum ActiviyRequestCode {
    MainActivity,
    HelpActiviy,
    QuestionnaireActivity,
    PreferencesActivity,
    LinkDeviceHelper,
    DEVICE_ADMIN
}

enum LinkHelperBluetoothStates {
    connecting,
    connected,
    disconnecting,
    disconnected,
}

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
    public boolean showCalibrationValuesError = false;
}