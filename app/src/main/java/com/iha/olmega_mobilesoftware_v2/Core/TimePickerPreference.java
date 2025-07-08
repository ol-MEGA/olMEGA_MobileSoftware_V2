package com.iha.olmega_mobilesoftware_v2.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.iha.olmega_mobilesoftware_v2.MainActivity;
import com.iha.olmega_mobilesoftware_v2.R;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TimePickerPreference extends DialogPreference {
    private SharedPreferences sharedPrefs;
    private String currentHour;
    private String currentMinute;
    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_time_picker);
    }

    @Override
    protected void onClick() {
        MainActivity act = (MainActivity) getContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        currentHour = sharedPrefs.getString("notification_hour", "8");
        currentMinute = sharedPrefs.getString("notification_minute", "0");

        boolean isSystem24Hour = DateFormat.is24HourFormat(getContext());
        int clockFormat = isSystem24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(Integer.parseInt(currentHour))
                .setMinute(Integer.parseInt(currentMinute))
                .setTitleText(getContext().getString(R.string.notification_hour_name))
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("notification_hour", String.valueOf(picker.getHour()));
            editor.putString("notification_minute", String.valueOf(picker.getMinute()));
            editor.apply();
        });

        picker.show(act.getSupportFragmentManager(), "timepicker");
    }
}
