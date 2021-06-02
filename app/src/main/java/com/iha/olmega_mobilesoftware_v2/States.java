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
    QuestionnaireActivity,
    PreferencesActivity
}