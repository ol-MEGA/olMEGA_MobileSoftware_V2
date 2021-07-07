package com.iha.olmega_mobilesoftware_v2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.iha.olmega_mobilesoftware_v2.Bluetooth.BluetoothSPP;
import com.iha.olmega_mobilesoftware_v2.Bluetooth.BluetoothState;

import java.util.ArrayList;

public class LinkDeviceHelper extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private BluetoothSPP bt;
    private ArrayList<BluetoothDevice> pairedDevices;
    private LinkHelperBluetoothStates linkHelperBluetoothState;
    private boolean BluetoothIsConnected = false;
    private boolean BluetoothHasData = false;
    private long maxConnectionTrialTimeout = 0, currentConnectionTryTimeout;
    private int currentDeviceId = 0;
    private int timeOut = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_device_helper);
        findViewById(R.id.button_Link).setOnClickListener(v -> linkPossibleDevices());
        findViewById(R.id.buttonClose).setOnClickListener(v -> finish());
        initBluetooth();
    }

    private void initBluetooth() {
        findViewById(R.id.button_Link).setEnabled(false);
        pairedDevices = new ArrayList<>();
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            pairedDevices.add(device);
        }
        if (bt == null)
            bt = new BluetoothSPP(getApplicationContext());
        int delay = 2000;
        if(!bt.isBluetoothEnabled()) {
            bt.enable();
            delay = 10000;
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bt.cancelDiscovery();
            bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                public void onDeviceConnected(String name, String address) { BluetoothIsConnected = true; }
                public void onDeviceDisconnected() {
                    BluetoothIsConnected = false;
                }
                public void onDeviceConnectionFailed() {
                    BluetoothIsConnected = false;
                }
            });
            //bt.setBluetoothStateListener(state -> { });
            bt.setOnDataReceivedListener((data, message) -> BluetoothHasData = true);
            findViewById(R.id.button_Link).setEnabled(true);
        }, delay);
    }

    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.disconnect();
        bt.stopService();
    }

    private void linkPossibleDevices() {
        bt.disconnect();
        bt.stopService();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_OTHER);
            currentDeviceId = 0;
            findViewById(R.id.button_Link).setEnabled(false);
            linkHelperBluetoothState = LinkHelperBluetoothStates.disconnected;
            maxConnectionTrialTimeout = System.currentTimeMillis() + timeOut * 1000;
            currentConnectionTryTimeout = System.currentTimeMillis() + 4 * 1000;
            final Handler connectionHandler = new Handler();
            connectionHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long newTimer = 200;
                    switch (linkHelperBluetoothState) {
                        case disconnected:
                            bt.connect(pairedDevices.get(currentDeviceId).getAddress());
                            linkHelperBluetoothState = LinkHelperBluetoothStates.connecting;
                            break;
                        case connecting:
                            if (System.currentTimeMillis() > currentConnectionTryTimeout) {
                                linkHelperBluetoothState = LinkHelperBluetoothStates.disconnecting;
                            } else if (BluetoothIsConnected) {
                                bt.send("STOREMAC", false);
                                bt.send("STOREMAC", false);
                                linkHelperBluetoothState = LinkHelperBluetoothStates.disconnecting;
                            }
                            break;
                        case disconnecting:
                            if (BluetoothIsConnected)
                                bt.send("STOREMAC", false);
                            //bt.disconnect();
                            linkHelperBluetoothState = LinkHelperBluetoothStates.disconnected;
                            currentDeviceId = (currentDeviceId + 1) % pairedDevices.size();
                            break;
                    }
                    if (System.currentTimeMillis() > maxConnectionTrialTimeout || pairedDevices.isEmpty()){
                        bt.getBluetoothService().stop();
                        bt.disconnect();
                        bt.stopService();
                        Handler handler = new Handler();
                        handler.postDelayed(() -> checkForStableConnection(), 1000);
                    }
                    else
                        connectionHandler.postDelayed(this, newTimer);
                }
            }, 100);
        }, 1000);
    }

    private void checkForStableConnection() {
        BluetoothHasData = false;
        BluetoothIsConnected = false;
        bt.setupService();
        bt.startService(BluetoothState.DEVICE_OTHER);
        findViewById(R.id.button_Link).setEnabled(false);
        maxConnectionTrialTimeout = System.currentTimeMillis() + timeOut * 1000;
        final Handler waitForConnectionHandler = new Handler();
        waitForConnectionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bt.send("GC", false);
                if (BluetoothHasData) {
                    bt.getBluetoothService().stop();
                    bt.disconnect();
                    bt.stopService();
                    new AlertDialog.Builder(LinkDeviceHelper.this, R.style.SwipeDialogTheme)
                            .setTitle(R.string.app_name)
                            .setMessage("Linking was successfull!")
                            .setPositiveButton(R.string.buttonTextOkay, (dialog, which) -> LinkDeviceHelper.this.finish())
                            .setCancelable(false)
                            .show();
                } else if (System.currentTimeMillis() > maxConnectionTrialTimeout)  {
                    bt.getBluetoothService().stop();
                    bt.disconnect();
                    bt.stopService();
                    new AlertDialog.Builder(LinkDeviceHelper.this, R.style.SwipeDialogTheme)
                            .setTitle(R.string.app_name)
                            .setMessage("Linking was not successfull! Please make sure the device has been paired and retry!")
                            .setPositiveButton(R.string.buttonTextOkay, (dialog, which) -> findViewById(R.id.button_Link).setEnabled(true))
                            .setCancelable(false)
                            .show();
                } else
                    waitForConnectionHandler.postDelayed(this, 200);
            }
        }, 100);
    }
}
