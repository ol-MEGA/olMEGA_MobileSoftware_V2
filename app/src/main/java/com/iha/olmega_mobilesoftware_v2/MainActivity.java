    package com.iha.olmega_mobilesoftware_v2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.os.UserManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.iha.olmega_mobilesoftware_v2.Core.FileIO;
import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.QuestionnaireActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
    private ControlService controlService;
    private boolean mIsBound = false;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;
    private String[] neccessaryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    private int neccessaryPermissionsIdx = 0;
    private boolean isLocked = false;
    private QuestionnaireMotivation questionnaireMotivation = QuestionnaireMotivation.manual;
    private Vibrator vibrator;
    private long automaticQuestTimer = Long.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());
         */
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                StringWriter sw = new StringWriter();
                paramThrowable.printStackTrace(new PrintWriter(sw));
                LogIHAB.log(sw.toString());
                System.exit(2);
            }
        });
        setContentView(R.layout.activity_main);
        MainActivity.this.doBindService();

        findViewById(R.id.logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreferences(controlService.Status().Preferences().isAdmin() || !controlService.Status().Preferences().isInKioskMode);
            }
        });
        findViewById(R.id.Action_Logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createHelpScreen();
            }
        });

        findViewById(R.id.logo).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timerLongClick.start();
                        break;
                    case MotionEvent.ACTION_UP:
                        timerLongClick.cancel();
                        break;
                }
                return false;
            }

            // Timer enabling long click for user access to preferences menu
            private long durationLongClick = 5 * 1000;
            private CountDownTimer timerLongClick = new CountDownTimer(durationLongClick, 200) {
                @Override
                public void onTick(long l) {
                }

                @Override
                public void onFinish() {
                    showPreferences(!controlService.Status().Preferences().isAdmin());
                }
            };
        });

        findViewById(R.id.InfoTextView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                    startQuestionnaire();
                return true;
            }

            ;
        });

        final Handler dateTimeHandler = new Handler(Looper.myLooper());
        dateTimeHandler.postDelayed(new Runnable() {
            public synchronized void run() {
                TextView dateTimeTextView = findViewById(R.id.DateTimeTextView);
                Date currentDate = new Date(System.currentTimeMillis());
                SimpleDateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                dateTimeTextView.setText(dateformat.format(currentDate.getTime()));
                if (wifiActivated) {
                    if (findViewById(R.id.Action_Wifi).getVisibility() == View.VISIBLE)
                        findViewById(R.id.Action_Wifi).setVisibility(View.INVISIBLE);
                    else
                        findViewById(R.id.Action_Wifi).setVisibility(View.VISIBLE);
                }
                if (isLocked == false && questionnaireMotivation == QuestionnaireMotivation.auto) {
                    if (automaticQuestTimer <= 0)
                        automaticQuestTimer = 30 * 60;
                    if (automaticQuestTimer >= 29 * 60) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                    if (findViewById(R.id.InfoTextView).getVisibility() == View.VISIBLE)
                        findViewById(R.id.InfoTextView).setVisibility(View.INVISIBLE);
                    else
                        findViewById(R.id.InfoTextView).setVisibility(View.VISIBLE);
                    automaticQuestTimer = automaticQuestTimer - 1;
                }
                else
                    findViewById(R.id.InfoTextView).setVisibility(View.VISIBLE);
                dateTimeHandler.postDelayed(this, 1000);
            }
        }, 0);
    }

    protected void onStart() {
        super.onStart();
        checkPermission();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // create necessary files
        if (!com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings.exists()) {
            try
            {
                InputStream inputStream = getResources().openRawResource(R.raw.udatersettings);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                OutputStream outStream = new FileOutputStream(com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings);
                outStream.write(buffer);
            } catch (IOException e) { }
        }
    }

    public void checkPermission() {
        if (neccessaryPermissionsIdx < neccessaryPermissions.length) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, neccessaryPermissions[neccessaryPermissionsIdx]) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{neccessaryPermissions[neccessaryPermissionsIdx]}, 1);
            } else {
                neccessaryPermissionsIdx++;
                checkPermission();
            }
        } else {
            File updaterSettings = new File(FileIO.getFolderPath() + File.separator + "UdaterSettings.xml");
            if (!updaterSettings.isFile()) {
                try {
                    InputStream inputStream = getResources().openRawResource(R.raw.udatersettings);
                    byte[] buffer = new byte[inputStream.available()];
                    inputStream.read(buffer);
                    OutputStream outStream = new FileOutputStream(updaterSettings);
                    outStream.write(buffer);
                } catch (IOException e) {
                }
            }
            if (!SystemStatus.AFExConfigFolder.exists())
                SystemStatus.AFExConfigFolder.mkdirs();
            if (SystemStatus.AFExConfigFolder.listFiles() == null || SystemStatus.AFExConfigFolder.listFiles().length == 0) {
                try {
                    File example_mic_in_speaker_out = new File(SystemStatus.AFExConfigFolder.getAbsolutePath() + File.separator + "example_mic_in_speaker_out.xml");
                    InputStream inputStream = getResources().openRawResource(R.raw.example_mic_in_speaker_out);
                    byte[] buffer = new byte[inputStream.available()];
                    inputStream.read(buffer);
                    OutputStream outStream = new FileOutputStream(example_mic_in_speaker_out);
                    outStream.write(buffer);

                    File example_rfcomm_in_audio_out = new File(SystemStatus.AFExConfigFolder.getAbsolutePath() + File.separator + "example_rfcomm_in_audio_out.xml");
                    inputStream = getResources().openRawResource(R.raw.example_rfcomm_in_audio_out);
                    buffer = new byte[inputStream.available()];
                    inputStream.read(buffer);
                    outStream = new FileOutputStream(example_rfcomm_in_audio_out);
                    outStream.write(buffer);
                } catch (IOException e) {
                }
            }
            new FileIO().scanQuestOptions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermission();
                } else {
                    Toast.makeText(this, "All Permissions must be granted", Toast.LENGTH_LONG).show();
                    this.finish();
                }
                return;
            }
        }
    }

    private void setDefaultCosuPolicies(boolean active) {
        // set user restrictions
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, false);
        setUserRestriction(UserManager.DISALLOW_CREATE_WINDOWS, active);
        Log.i(TAG, "KIOSK MODE: " + active);
        // disable keyguard and status bar - needs API 23 (Damnit!)
        //mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        //mDevicePolicyManager.setStatusBarDisab(controlService.Status().Settings().isInKioskMode || controlService.Status().Settings().isAdmin()led(mAdminComponentName, active);
    }

    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction);
        }
    }

    private void setKioskMode(boolean enabled) {
        if (enabled) {
            if (mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())) {
                startLockTask();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    getWindow().setDecorFitsSystemWindows(false);
                    if (getWindow().getInsetsController() != null) {
                        getWindow().getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            } else {
                stopLockTask();
                Toast.makeText(this, "Kiosk not permitted", Toast.LENGTH_LONG).show();
            }
        } else {
            stopLockTask();
        }
    }

    private void startQuestionnaire() {
        TextView InfoTextView = findViewById(R.id.InfoTextView);
        if (controlService != null && InfoTextView.isEnabled() && isLocked == false) {
            isLocked = true;
            controlService.Status().setActiveActivity(ActiviyRequestCode.QuestionnaireActivity);
            automaticQuestTimer = Long.MIN_VALUE;
            InfoTextView.setVisibility(View.VISIBLE);
            InfoTextView.setText(R.string.pleaseWait);
            Intent QuestionaireIntent = new Intent(controlService, QuestionnaireActivity.class);
            QuestionaireIntent.putExtra("forceAnswer", controlService.Status().Preferences().forceAnswer());
            QuestionaireIntent.putExtra("isAdmin", controlService.Status().Preferences().isAdmin());
            QuestionaireIntent.putExtra("clientID", controlService.Status().Preferences().clientID());
            QuestionaireIntent.putExtra("selectedQuest", controlService.Status().Preferences().selectedQuest());
            QuestionaireIntent.putExtra("motivation", questionnaireMotivation.toString());
            startActivityForResult(QuestionaireIntent, ActiviyRequestCode.QuestionnaireActivity.ordinal());
            questionnaireMotivation = QuestionnaireMotivation.manual;
        }
    }

    void doBindService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ControlService.class));
        } else {
            this.startService(new Intent(this, ControlService.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(MainActivity.this, ControlService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        checkWifi();
        isLocked = false;
        if (controlService != null)
            controlService.Status().setActiveActivity(ActiviyRequestCode.MainActivity);
    }

    private boolean wifiActivated = false;
    private void checkWifi() {
        runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                findViewById(R.id.Action_Wifi).setVisibility(View.INVISIBLE);
                wifiActivated = false;
                if (wifiManager.isWifiEnabled() && controlService != null) {
                    wifiActivated = true;
                    if (controlService.Status().Preferences().isAdmin()) {
                        findViewById(R.id.Action_Wifi).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "Warning: Wifi should be disabled for optimal data transmission!", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else
                        wifiManager.setWifiEnabled(false);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //if (controlService != null)
        //    controlService.startForeground();
        doUnbindService();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //doUnbindService();
    }

    void doUnbindService() {
        if (mIsBound) {
            unbindService(mServiceConnection);
            mIsBound = false;
        }
    }

    private void showPreferences(boolean show) {
        if (show && isLocked == false) {
            isLocked = true;
            controlService.Status().setActiveActivity(ActiviyRequestCode.PreferencesActivity);
            TextView InfoTextView = findViewById(R.id.InfoTextView);
            InfoTextView.setVisibility(View.VISIBLE);
            InfoTextView.setText(R.string.pleaseWait);
            Intent intent = new Intent(controlService, PreferencesActivity.class);
            intent.putExtra("isDeviceOwner", controlService.Status().Preferences().isDeviceOwner);
            startActivityForResult(intent, ActiviyRequestCode.PreferencesActivity.ordinal());
        }
    }

    private void createHelpScreen() {
        if (isLocked == false) {
            isLocked = true;
            controlService.Status().setActiveActivity(ActiviyRequestCode.HelpActiviy);
            TextView InfoTextView = findViewById(R.id.InfoTextView);
            InfoTextView.setVisibility(View.VISIBLE);
            InfoTextView.setText(R.string.pleaseWait);
            startActivity(new Intent(this, Help.class));
        }
    }

    // KIOSK MODE
    // adb shell dpm set-device-owner com.iha.olmega_mobilesoftware_v2/.AdminReceiver .
    private void setKioskMode() {
        ComponentName deviceAdmin = new ComponentName(MainActivity.this, AdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            mDevicePolicyManager.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
            mAdminComponentName = deviceAdmin;
            mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            controlService.Status().Preferences().isDeviceOwner = true;
        } catch (Exception e) {
            controlService.Status().Preferences().isDeviceOwner = false;
        }
        if (controlService.Status().Preferences().isDeviceOwner) {
            try {
                setDefaultCosuPolicies(!controlService.Status().Preferences().isAdmin());
                setKioskMode(!controlService.Status().Preferences().isAdmin());
                controlService.Status().Preferences().isInKioskMode = !controlService.Status().Preferences().isAdmin();
            } catch (Exception e) {
                controlService.Status().Preferences().isInKioskMode = false;
            }
        }
    }

    // This disables the Volume Buttons
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.e(TAG, "EVENT: " + event.getKeyCode());
        if (blockedKeys.contains(event.getKeyCode()) && !controlService.Status().Preferences().isAdmin()) {
            return true;
        } else if ((event.getKeyCode() == KeyEvent.KEYCODE_POWER) && !controlService.Status().Preferences().isAdmin()) {
            Log.e(TAG, "POWER BUTTON WAS PRESSED");
            return super.dispatchKeyEvent(event);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            controlService = ((ControlService.LocalBinder) service).getService();
            controlService.startForeground();
            if (!controlService.Status().Preferences().isAdmin())
                setKioskMode();
            checkWifi();
            controlService.Status().setSystemStatusListener(new SystemStatus.SystemStatusListener() {
                public void setAcitivyStates(AcitivyStates acitivyStates) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public synchronized void run() {
                            if (QuestionnaireActivity.thisAppCompatActivity != null && acitivyStates.isCharging && controlService.Status().Preferences().usbCutsConnection())
                                QuestionnaireActivity.thisAppCompatActivity.finish();

                            findViewById(R.id.charging).setVisibility((acitivyStates.isCharging ? 0 : 1) * 8);
                            TextView InfoTextView = (TextView) findViewById(R.id.InfoTextView);
                            InfoTextView.setText(acitivyStates.InfoText);
                            InfoTextView.setEnabled(acitivyStates.questionaireEnabled);
                            if (controlService.Status().Preferences().isAdmin() || !controlService.Status().Preferences().isInKioskMode)
                                findViewById(R.id.logo).setBackgroundResource(R.color.BatteryGreen);
                            else
                                findViewById(R.id.logo).setBackgroundResource(R.color.lighterGray);
                            View battery_bottom = findViewById(R.id.battery_bottom);
                            switch (acitivyStates.BatteryState) {
                                case Normal:
                                    battery_bottom.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.BatteryGreen));
                                    break;
                                case Warning:
                                    battery_bottom.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.BatteryYellow));
                                    break;
                                case Critical:
                                    battery_bottom.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.JadeRed));
                                    break;
                            }
                            // Batterie Level
                            ViewGroup.LayoutParams params = battery_bottom.getLayoutParams();
                            params = findViewById(R.id.battery_bottom).getLayoutParams();
                            params.height = (int) (findViewById(R.id.BatterieView).getHeight() * (acitivyStates.batteryLevel / 100));
                            battery_bottom.setLayoutParams(params);

                            View battery_top = findViewById(R.id.battery_top);
                            params = battery_top.getLayoutParams();
                            params.height = (int) (findViewById(R.id.BatterieView).getHeight() * (1 - acitivyStates.batteryLevel / 100));
                            battery_top.setLayoutParams(params);

                            if (acitivyStates.profileState == States.connected)
                                findViewById(R.id.Action_Record).setBackgroundTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(), R.color.PhantomDarkBlue, null)));
                            else
                                findViewById(R.id.Action_Record).setBackgroundTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(), R.color.JadeGray, null)));
                        }
                    });
                }

                public void updateAutomaticQuestionnaireTimer(String Message, long TimeRemaining) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public synchronized void run() {
                            TextView NextQuestTextView = findViewById(R.id.nextQuestTextView);
                            if (TimeRemaining > 0)
                                NextQuestTextView.setText(Message);
                            else
                                NextQuestTextView.setText("");
                            if (TimeRemaining < 0 && TimeRemaining > Long.MIN_VALUE)
                                questionnaireMotivation = QuestionnaireMotivation.auto;
                        }
                    });
                }
            });
        }

        public void onServiceDisconnected(ComponentName className) {
            controlService.Status().Refresh();
            controlService = null;
        }

    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (controlService != null) {
            if (requestCode == ActiviyRequestCode.QuestionnaireActivity.ordinal() && resultCode == Activity.RESULT_OK) {
                controlService.Status().ResetAutomaticQuestionaireTimer();
            } else if (requestCode == ActiviyRequestCode.PreferencesActivity.ordinal() && resultCode == Activity.RESULT_OK) {
                if (data.getBooleanExtra("unsetDeviceAdmin", false)) {
                    try {
                        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
                        Toast.makeText(this, "Removing DeviceAdmin successful!", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Removing DeviceAdmin not successful!", Toast.LENGTH_LONG).show();
                    }
                }
                if (data.getBooleanExtra("killAppAndService", false)) {
                    if (controlService.Status().Preferences().isInKioskMode)
                        stopLockTask();
                    stopService(new Intent(this, ControlService.class));
                    controlService.stopForeground(true);
                    doUnbindService();
                    Toast.makeText(MainActivity.this, "App and Service killed!", Toast.LENGTH_LONG).show();
                    this.finish();
                    System.exit(1);
                }
                if (data.getStringExtra("installNewApp") != null) {
                    stopService(new Intent(this, ControlService.class));
                    this.finish();
                    Intent intent;
                    File apk = new File(data.getStringExtra("installNewApp"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri apkURI = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", apk);
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(apkURI);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        Uri apkUri = Uri.fromFile(apk);
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (controlService.Status().Preferences().isAdmin() || !controlService.Status().Preferences().isInKioskMode)
            super.onBackPressed();
    }
}