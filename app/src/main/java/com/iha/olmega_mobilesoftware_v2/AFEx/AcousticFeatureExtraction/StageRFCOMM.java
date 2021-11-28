package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.iha.olmega_mobilesoftware_v2.Bluetooth.BluetoothSPP;
import com.iha.olmega_mobilesoftware_v2.Bluetooth.BluetoothState;
import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.Core.RingBuffer;
import com.iha.olmega_mobilesoftware_v2.R;
import com.iha.olmega_mobilesoftware_v2.States;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

enum initState {UNINITIALIZED, WAITING_FOR_CALIBRATION_VALUES, WAITING_FOR_AUDIOTRANSMISSION, INITIALIZED}

public class StageRFCOMM extends Stage {
    final static String LOG = "StageRFCOMM";

    byte checksum = 0;
    private boolean restartStages = false;
    private BluetoothSPP bt;
    private static final int block_size = 64;
    private int alivePingTimeout = 100, ValidBlocksFeatureBufferIdx = 0, BufferIdx = 0, frames, lastBlockNumber = 0, currBlockNumber = 0, additionalBytesCount = 0, lostBlockCount, AudioBufferSize = block_size * 4, millisPerBlock = block_size * 1000 / 16000;
    private long lastEmptyPackageTimer, lastStreamTimer, lastBluetoothPingTimer;
    public float[] calibValues = new float[]{Float.NaN, Float.NaN};
    public float[] calibValuesInDB = new float[]{Float.NaN, Float.NaN};
    private RingBuffer ringBuffer;
    private initState initializeState;
    byte[] emptyAudioBlock;
    float[][] dataOut;
    float[][] ValidBlocksFeature;
    private long lastStateChange = System.currentTimeMillis();
    int ReconnectTrials = 0;
    StageFeatureWrite myStageFeatureWrite;

    public StageRFCOMM(HashMap parameter) {
        super(parameter);
        sendBroadcast(States.init);
        hasInput = false;
        int blocksize_ms = 25;
        frames = blocksize_ms * samplingrate / 100;
        dataOut = new float[channels][frames];
        ValidBlocksFeature = new float[1][(int)Math.ceil(frames / block_size) + 1];
        ringBuffer = new RingBuffer(AudioBufferSize * 2);
        emptyAudioBlock = new byte[AudioBufferSize];
    }

    public void start() {
        super.start();
        sendBroadcast(States.init);
        initBluetooth();
    }

    boolean loop = true;
    private void initBluetooth() {
        LogIHAB.log("Bluetooth: Setting up StageRFCOMM");
        //Log.d(LOG, "Bluetooth: Setting up StageRFCOMM");
        if (bt == null)
            bt = new BluetoothSPP(context);
        int delay = 2000;
        if(!bt.isBluetoothEnabled()) {
            LogIHAB.log("Bluetooth: Enable Bluetooth Adapter");
            bt.enable();
            delay = 5000;
        }
        loop = true;
        Handler showInitLoopHandler = new Handler(Looper.getMainLooper());
        showInitLoopHandler.postDelayed(new Runnable() {
            public synchronized void run() {
                if (StageRFCOMM.this.loop){
                    sendBroadcast(States.init);
                    showInitLoopHandler.postDelayed(this, 100);
                }
                else
                    sendBroadcast(States.connecting);
            }
        }, 0);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            StageRFCOMM.this.loop = false;
            if (bt != null) {
                bt.cancelDiscovery();
                bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                    public void onDeviceConnected(String name, String address) {
                        setState(initState.UNINITIALIZED);
                        sendBroadcast(States.connecting);
                        lastBlockNumber = 0;
                        currBlockNumber = 0;
                        additionalBytesCount = 0;
                        lostBlockCount = 0;
                        checksum = 0;
                        lastEmptyPackageTimer = System.currentTimeMillis();
                        lastStreamTimer = System.currentTimeMillis();
                        lastBluetoothPingTimer = System.currentTimeMillis();
                    }

                    public void onDeviceDisconnected() {
                        setState(initState.UNINITIALIZED);
                        sendBroadcast(States.connecting);
                        LogIHAB.log("Bluetooth: disconnected");
                    }

                    public void onDeviceConnectionFailed() {
                    }
                });
                bt.setBluetoothStateListener(state -> {
                    if (state == BluetoothState.STATE_LISTEN || state == BluetoothState.STATE_NONE) {
                        setState(initState.UNINITIALIZED);
                        sendBroadcast(States.connecting);
                    }
                });
                bt.setOnDataReceivedListener((data, message) -> DataReceived(data));
                bt.setupService();
                Handler TimeHandler = new Handler(Looper.getMainLooper());
                TimeHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (initializeState == initState.WAITING_FOR_CALIBRATION_VALUES) {
                            if (System.currentTimeMillis() - lastStreamTimer > 1000 && bt != null) {
                                if (bt != null)
                                    bt.send("GC", false);
                                lastStreamTimer = System.currentTimeMillis();
                            }
                        }
                        if ((System.currentTimeMillis() - lastStateChange > 60 * 1000 && initializeState == initState.UNINITIALIZED) ||
                                (System.currentTimeMillis() - lastStateChange > 10 * 1000 && initializeState == initState.WAITING_FOR_AUDIOTRANSMISSION) ||
                                (System.currentTimeMillis() - lastStateChange > 10 * 1000 && initializeState == initState.WAITING_FOR_CALIBRATION_VALUES)) {
                            lastStateChange = System.currentTimeMillis();
                            setState(initState.UNINITIALIZED);
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (mBluetoothAdapter.isEnabled() && bt != null) {
                                bt.stopService();
                                ReconnectTrials += 1;
                                if (ReconnectTrials >= 5 || initializeState == initState.WAITING_FOR_CALIBRATION_VALUES || initializeState == initState.WAITING_FOR_AUDIOTRANSMISSION) {
                                    LogIHAB.log("Bluetooth: Disable Bluetooth Adapter");
                                    mBluetoothAdapter.disable();
                                    ReconnectTrials = 0;
                                }
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    initBluetooth();
                                }, 1000);
                            }
                        } else if (initializeState == initState.INITIALIZED) {
                            if (System.currentTimeMillis() - lastEmptyPackageTimer > 200) {
                                for (long count = 0; count < 200 / millisPerBlock; count++) {
                                    lostBlockCount++;
                                    lastBlockNumber++;
                                    writeData(emptyAudioBlock, true);
                                    Log.d(LOG, "emptyAudioBlock (in Loop)");
                                }
                                lastEmptyPackageTimer = System.currentTimeMillis();
                            }
                            if (System.currentTimeMillis() - lastBluetoothPingTimer > alivePingTimeout) {
                                if (bt != null)
                                    bt.send(" ", false);
                                lastBluetoothPingTimer = System.currentTimeMillis();
                            }
                            if (System.currentTimeMillis() - lastStreamTimer >= 5 * 1000) // 5 seconds
                            {
                                LogIHAB.log("Bluetooth: Transmission Timeout");
                                setState(initState.UNINITIALIZED);
                                sendBroadcast(States.connecting);
                                if (bt != null) {
                                    bt.getBluetoothService().connectionLost();
                                    bt.getBluetoothService().start(false);
                                }
                            }
                            for (Stage consumer : consumerSet) {
                                if (consumer.thread == null || !consumer.thread.isAlive() || consumer.thread.isInterrupted()) {
                                    restartStages = true;
                                }
                            }
                        }
                        if (bt != null)
                            TimeHandler.postDelayed(this, 100);
                    }
                }, 100);
                bt.startService(BluetoothState.DEVICE_OTHER);
                if (bt.isBluetoothEnabled())
                    sendBroadcast(States.connecting);
            }
        }, delay);
    }

    private void sendBroadcast(States state) {
        switch (state) {
            case init:
                LogIHAB.log("Bluetooth: initializing");
                break;
            case connecting:
                LogIHAB.log("Bluetooth: connecting");
                break;
            case connected:
                LogIHAB.log("Bluetooth: connected");
                if (bt != null && bt.getBluetoothService() != null)
                    LogIHAB.log("Bluetooth: Device '" + bt.getBluetoothService().BluetoothDevice_MAC + "'");
                break;
        }
        Intent  intent = new Intent("StageState");    //action: "msg"
        intent.setPackage(context.getPackageName());
        intent.putExtra("currentState", state.ordinal());
        context.sendBroadcast(intent);
    }

    @Override
    protected void process(float[][] temp) {
    }

    public void stop() {
        if (myStageFeatureWrite != null)
            myStageFeatureWrite.stop();
        if (bt != null)
            bt.stopService();
        bt = null;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    private void setState(initState state) {
        if (initializeState != state) {
            lastStateChange = System.currentTimeMillis();
            initializeState = state;
        }
    }

    synchronized private void DataReceived(byte[] data) {
        for (byte sample : data) {
            ringBuffer.addByte(sample);
            checksum ^= ringBuffer.getByte(0);
            if (ringBuffer.getByte(-2) == (byte) 0x00 && ringBuffer.getByte(-3) == (byte) 0x80) {
                switch (initializeState) {
                    case UNINITIALIZED:
                        int protocollVersion = (((ringBuffer.getByte(-4) & 0xFF) << 8) | (ringBuffer.getByte(-5) & 0xFF));
                        switch (protocollVersion) { // Check Protocol-Version
                            case 1:
                                calibValuesInDB[0] = (float)0.0;
                                calibValuesInDB[1] = (float)0.0;
                                calibValues[0] = (float)1.0;
                                calibValues[1] = (float)1.0;
                                additionalBytesCount = 12;
                                setState(initState.WAITING_FOR_AUDIOTRANSMISSION);
                                sendBroadcast(States.connecting);
                                break;
                            case 2:
                                calibValuesInDB[0] = Float.NaN;
                                calibValuesInDB[1] = Float.NaN;
                                calibValues[0] = Float.NaN;
                                calibValues[1] = Float.NaN;
                                additionalBytesCount = 12;
                                if (bt != null)
                                    bt.send("GC", false);
                                setState(initState.WAITING_FOR_CALIBRATION_VALUES);
                                sendBroadcast(States.connecting);
                                break;
                        }
                        break;
                    case WAITING_FOR_CALIBRATION_VALUES:
                        if (ringBuffer.getByte(-15) == (byte) 0xFF && ringBuffer.getByte(-16) == (byte) 0x7F && ringBuffer.getByte(-14) == (byte) 'C' && (ringBuffer.getByte(-13) == (byte) 'L' || ringBuffer.getByte(-13) == (byte) 'R')) {
                            byte[] values = new byte[8];
                            byte ValuesChecksum = ringBuffer.getByte(-13);
                            for (int count = 0; count < 8; count++) {
                                values[count] = ringBuffer.getByte(-12 + count);
                                ValuesChecksum ^= values[count];
                            }
                            if (ValuesChecksum == ringBuffer.getByte(-4)) {
                                if (ringBuffer.getByte(-13) == 'L')
                                    calibValuesInDB[0] = (float)ByteBuffer.wrap(values).getDouble();
                                else if (ringBuffer.getByte(-13) == 'R')
                                    calibValuesInDB[1] = (float)ByteBuffer.wrap(values).getDouble();
                                if (!Float.isNaN(calibValuesInDB[0]) && !Float.isNaN(calibValuesInDB[1])) {
                                    calibValues[0] = (float)Math.pow(10, calibValuesInDB[0] / 20.0);
                                    calibValues[1] = (float)Math.pow(10, calibValuesInDB[1] / 20.0);
                                    if (calibValuesInDB[0] <= 0 || calibValuesInDB[1] <= 0){
                                        Intent  intent = new Intent("CalibrationValuesError");    //action: "msg"
                                        intent.setPackage(context.getPackageName());
                                        intent.putExtra("Value", true);
                                        context.sendBroadcast(intent);
                                    }
                                    setState(initState.WAITING_FOR_AUDIOTRANSMISSION);
                                    sendBroadcast(States.connecting);
                                }
                            }
                        }
                        break;
                    case WAITING_FOR_AUDIOTRANSMISSION:
                        //Log.d(LOG, "ConnectedThread::RUN::WAITING_FOR_AUDIOTRANSMISSION");
                        if (ringBuffer.getByte(2 - (AudioBufferSize + additionalBytesCount)) == (byte) 0xFF && ringBuffer.getByte(1 - (AudioBufferSize + additionalBytesCount)) == (byte) 0x7F) {
                            if (ringBuffer.getByte(0) == (checksum ^ ringBuffer.getByte(0))) {
                                currBlockNumber = ((ringBuffer.getByte(-6) & 0xFF) << 8) | (ringBuffer.getByte(-7) & 0xFF);
                                lastBlockNumber = currBlockNumber;
                                setState(initState.INITIALIZED);
                                sendBroadcast(States.connected);
                                restartStages = true;
                            }
                            checksum = 0;
                        }
                        break;
                    case INITIALIZED:
                        if (ringBuffer.getByte(2 - (AudioBufferSize + additionalBytesCount)) == (byte) 0xFF && ringBuffer.getByte(1 - (AudioBufferSize + additionalBytesCount)) == (byte) 0x7F) {
                            if (ringBuffer.getByte(0) == (checksum ^ ringBuffer.getByte(0))) {
                                if (restartStages) {
                                    if (myStageFeatureWrite != null)
                                        myStageFeatureWrite.stop();
                                    //Stage.startTime = Instant.now();
                                    HashMap<String, String> parameters = new HashMap<String, String>();
                                    parameters.put("id", "9999999");
                                    parameters.put("prefix", "VALIDBLOCKS");
                                    parameters.put("nfeatures", "1");
                                    parameters.put("blocksize", "1");
                                    parameters.put("hopsize", "1");
                                    myStageFeatureWrite = new StageFeatureWrite(parameters);
                                    myStageFeatureWrite.mySamplingRate = samplingrate / block_size;
                                    this.hopSizeOut = 1;
                                    this.blockSizeOut = 1;
                                    myStageFeatureWrite.inStage = this;
                                    myStageFeatureWrite.startWithoutThread();
                                    this.hopSizeOut = 400;
                                    this.blockSizeOut = 400;
                                    for (Stage consumer : consumerSet) {
                                        consumer.start();
                                    }
                                    restartStages = false;
                                }
                                currBlockNumber = ((ringBuffer.getByte(-6) & 0xFF) << 8) | (ringBuffer.getByte(-7) & 0xFF);
                                if (currBlockNumber < lastBlockNumber && lastBlockNumber - currBlockNumber > currBlockNumber + (65536 - lastBlockNumber))
                                    currBlockNumber += 65536;
                                if (lastBlockNumber < currBlockNumber) {
                                    lostBlockCount += currBlockNumber - lastBlockNumber - 1;
                                    while (lastBlockNumber < currBlockNumber - 1) {
                                        //Log.d(LOG, "CurrentBlock: " + currBlockNumber + "\tLostBlocks: " + lostBlockCount);
                                        writeData(emptyAudioBlock, true);
                                        //Log.d(LOG, "emptyAudioBlock " + lastBlockNumber);
                                        lastBlockNumber++;
                                    }
                                    lastBlockNumber = currBlockNumber % 65536;
                                    writeData(ringBuffer.data(3 - (AudioBufferSize + additionalBytesCount), AudioBufferSize), false);
                                    lastStreamTimer = System.currentTimeMillis();
                                    lastEmptyPackageTimer = System.currentTimeMillis();
                                }
                            }
                            checksum = 0;
                        }
                        break;
                }
            }
        }
    }

    synchronized private void writeData(byte[] data, boolean isEmpty) {
        if (!isEmpty)
            emptyAudioBlock = data.clone();
        if (myStageFeatureWrite != null) {
            ValidBlocksFeature[0][0] = 1;
            if (isEmpty)
                ValidBlocksFeature[0][0] = 0;
        }
        ValidBlocksFeatureBufferIdx++;
        short[] buffer = new short[data.length/2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);
        for (int k = 0; k < buffer.length / 2; k++) {
            dataOut[0][BufferIdx] = ((float)buffer[k * 2] * calibValues[0]) / (float)Short.MAX_VALUE;
            dataOut[1][BufferIdx] = ((float)buffer[k * 2 + 1] * calibValues[1]) / (float)Short.MAX_VALUE;
            BufferIdx++;
            if (BufferIdx == frames) {
                if (Stage.startTime == null)
                    Stage.startTime = Instant.now();
                send(dataOut);
                dataOut = new float[channels][frames];
                float[][] tempFloat = new float[1][1];
                if (myStageFeatureWrite != null) {
                    for (int i = 0; i < ValidBlocksFeatureBufferIdx; i++){
                        tempFloat[0][0] = ValidBlocksFeature[0][i];
                        myStageFeatureWrite.process(tempFloat.clone());
                    }
                    ValidBlocksFeature = new float[1][ValidBlocksFeature[0].length];
                    ValidBlocksFeatureBufferIdx = 0;
                }
                BufferIdx = 0;
            }
        }
    }
}