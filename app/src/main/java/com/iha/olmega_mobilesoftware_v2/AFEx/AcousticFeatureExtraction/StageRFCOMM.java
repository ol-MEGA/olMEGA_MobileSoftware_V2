package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Bluetooth.BluetoothSPP;
import com.iha.olmega_mobilesoftware_v2.Bluetooth.BluetoothState;
import com.iha.olmega_mobilesoftware_v2.Core.RingBuffer;
import com.iha.olmega_mobilesoftware_v2.States;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class StageRFCOMM extends Stage {
    final static String LOG = "StageRFCOMM";

    private BluetoothSPP bt;
    byte checksum = 0;
    private boolean IsBluetoothConnectionPingNecessary = false;
    private ConnectedThread mConnectedThread;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int block_size = 64;
    private static final int RECORDER_SAMPLERATE = 16000;
    private int AudioBufferSize = block_size * 4;
    private long lostBlockCount = 0;
    float[][] dataOut;
    private int frames;
    boolean isStopped = false;

    public StageRFCOMM(HashMap parameter) {
        super(parameter);
        sendBroadcast(States.init);
        Log.d(LOG, "Setting up StageRFCOMM");
        hasInput = false;
        int blocksize_ms = 25;
        frames = blocksize_ms * samplingrate / 100;
        dataOut = new float[channels][frames];

        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        mBluetoothAdapter.cancelDiscovery();
        initBluetooth();
    }

    @Override
    protected void process(float[][] temp) {
    }

    public void stop() {
        isStopped = true;
        stopRecording();
        if (bt != null)
            bt.stopService();
    }

    private void sendBroadcast(States state) {
        Intent intent = new Intent("StageState");    //action: "msg"
        intent.setPackage(context.getPackageName());
        intent.putExtra("currentState", state.ordinal());
        context.sendBroadcast(intent);
    }

    private void initBluetooth()
    {
        Log.d(LOG, "initBluetooth()");
        bt = new BluetoothSPP(context);
        if (bt.isBluetoothEnabled()) {
            bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
                public void onServiceStateChanged(int state) {
                    if (isStopped == false) {
                        //PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
                        //PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag");
                        if (state == BluetoothState.STATE_CONNECTED) {
                            //if (!wakeLock.isHeld()) {
                            //    wakeLock.acquire();
                            //}
                            Log.d(LOG, "STATE: CONNECTED");
                            mConnectedThread = new ConnectedThread(bt.getBluetoothService().getConnectedThread().getInputStream());
                            mConnectedThread.setPriority(Thread.MAX_PRIORITY);
                            mConnectedThread.start();
                        } else {
                            if (mConnectedThread != null) {
                                mConnectedThread = null;
                            }
                            //if (wakeLock.isHeld()) wakeLock.release();
                            if (state == BluetoothState.STATE_CONNECTING) {
                                sendBroadcast(States.connecting);
                                Log.d(LOG, "STATE: CONNECTING.");
                            } else if (state == BluetoothState.STATE_LISTEN) {
                                sendBroadcast(States.connecting);
                                Log.d(LOG, "STATE: LISTEN.");
                            } else if (state == BluetoothState.STATE_NONE) {
                                sendBroadcast(States.connecting);
                                Log.d(LOG, "STATE: NONE.");
                            }
                        }
                    }
                }
            });
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_OTHER);
        } else {
            bt.enable();
            initBluetooth();
        }
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (IsBluetoothConnectionPingNecessary) {
                    bt.send(" ", false);
                }
            }
        }, 0, 100);
    }

    private void AudioTransmissionEnd() {
        Log.d(LOG, "AudioTransmissionEnd()");
        stopRecording();
    }

    private void stopRecording() {
        Log.d(LOG, "stopRecording()");
        //stop();
    }

    private void startRecording() {
        Log.d(LOG, "startRecording()");
        lostBlockCount = 0;
        checksum = 0;
        sendBroadcast(States.connected);
        //ControlService.setIsRecording(true);
    }

    private void AudioTransmissionStart() {
        Log.d(LOG, "AudioTransmissionStart()");
    }

    int i = 0;
    private void writeData(byte[] data) {
        short[] buffer = new short[data.length/2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);
        for (int k = 0; k < buffer.length / 2; k++) {
            dataOut[0][i] = buffer[k * 2];
            dataOut[1][i] = buffer[k * 2 + 1];
            i++;
            if (i >= frames) {
                send(dataOut);
                dataOut = new float[channels][frames];
                i = 0;
            }
        }
    }

    enum initState {UNINITIALIZED, WAITING_FOR_CALIBRATION_VALUES, WAITING_FOR_AUDIOTRANSMISSION, INITIALIZED, STOP}

    class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        public initState initializeState;
        boolean isRunning = true;
        public boolean useCalib = true;
        private double[] calibValues = new double[]{Double.NaN, Double.NaN};
        private double[] calibValuesInDB = new double[]{Double.NaN, Double.NaN};

        ConnectedThread(InputStream stream) {
            mmInStream = stream;
        }

        public void run() {
            RingBuffer ringBuffer = new RingBuffer(AudioBufferSize * 2);
            int alivePingTimeout = 100, i, lastBlockNumber = 0, currBlockNumber = 0, additionalBytesCount = 0;
            byte[] data = new byte[1024], emptyAudioBlock = new byte[AudioBufferSize];
            byte checksum = 0;
            int timeoutBlockLimit = 500, millisPerBlock = block_size * 1000 / RECORDER_SAMPLERATE;
            lostBlockCount = 0;
            initializeState = initState.UNINITIALIZED;
            Long lastBluetoothPingTimer = System.currentTimeMillis(), lastEmptyPackageTimer = System.currentTimeMillis(), lastStreamTimer = System.currentTimeMillis();
            try {
                while (isRunning) {
                    if (mmInStream.available() >= data.length) {
                        mmInStream.read(data, 0, data.length);
                        for (i = 0; i < data.length; i++) {
                            ringBuffer.addByte(data[i]);
                            checksum ^= ringBuffer.getByte(0);
                            if (ringBuffer.getByte(-2) == (byte) 0x00 && ringBuffer.getByte(-3) == (byte) 0x80) {
                                switch (initializeState) {
                                    case UNINITIALIZED:
                                        //Log.d(LOG, "ConnectedThread::RUN::UNINITIALIZED");
                                        int protocollVersion = (((ringBuffer.getByte(-4) & 0xFF) << 8) | (ringBuffer.getByte(-5) & 0xFF));
                                        switch (protocollVersion) { // Check Protocol-Version
                                            case 1:
                                                calibValuesInDB[0] = 0.0;
                                                calibValuesInDB[1] = 0.0;
                                                calibValues[0] = 1.0;
                                                calibValues[1] = 1.0;
                                                additionalBytesCount = 12;
                                                initializeState = initState.WAITING_FOR_AUDIOTRANSMISSION;
                                                break;
                                            case 2:
                                                calibValuesInDB[0] = Double.NaN;
                                                calibValuesInDB[1] = Double.NaN;
                                                calibValues[0] = Double.NaN;
                                                calibValues[1] = Double.NaN;
                                                additionalBytesCount = 12;
                                                initializeState = initState.WAITING_FOR_CALIBRATION_VALUES;
                                                break;
                                            default:
                                                Log.d(LOG, "Unknown Protocoll-Version");
                                        }
                                        break;
                                    case WAITING_FOR_CALIBRATION_VALUES:
                                        //Log.d(LOG, "ConnectedThread::RUN::WAITING_FOR_CALIBRATION_VALUES");
                                        if (ringBuffer.getByte(-15) == (byte) 0xFF && ringBuffer.getByte(-16) == (byte) 0x7F && ringBuffer.getByte(-14) == (byte) 'C' && (ringBuffer.getByte(-13) == (byte) 'L' || ringBuffer.getByte(-13) == (byte) 'R')) {
                                            byte[] values = new byte[8];
                                            byte ValuesChecksum = ringBuffer.getByte(-13);
                                            for (int count = 0; count < 8; count++) {
                                                values[count] = ringBuffer.getByte(-12 + count);
                                                ValuesChecksum ^= values[count];
                                            }
                                            if (ValuesChecksum == ringBuffer.getByte(-4)) {
                                                if (ringBuffer.getByte(-13) == 'L')
                                                    calibValuesInDB[0] = ByteBuffer.wrap(values).getDouble();
                                                else if (ringBuffer.getByte(-13) == 'R')
                                                    calibValuesInDB[1] = ByteBuffer.wrap(values).getDouble();
                                                if (!Double.isNaN(calibValuesInDB[0]) && !Double.isNaN(calibValuesInDB[1])) {
                                                    if (calibValuesInDB[0] <= calibValuesInDB[1]) {
                                                        calibValues[0] = Math.pow(10, (calibValuesInDB[0] - calibValuesInDB[1]) / 20.0);
                                                        calibValues[1] = 1;
                                                    } else {
                                                        calibValues[0] = 1;
                                                        calibValues[1] = Math.pow(10, (calibValuesInDB[1] - calibValuesInDB[0]) / 20.0);
                                                    }
                                                    Log.d(LOG, "START AUDIOTRANSMISSION");
                                                    initializeState = initState.WAITING_FOR_AUDIOTRANSMISSION;
                                                }
                                            }
                                        } else if (System.currentTimeMillis() - lastStreamTimer > 1000) {
                                            bt.send("GC", false);
                                            lastStreamTimer = System.currentTimeMillis();
                                        }
                                        break;
                                    case WAITING_FOR_AUDIOTRANSMISSION:
                                        //Log.d(LOG, "ConnectedThread::RUN::WAITING_FOR_AUDIOTRANSMISSION");
                                        if (ringBuffer.getByte(2 - (AudioBufferSize + additionalBytesCount)) == (byte) 0xFF && ringBuffer.getByte(1 - (AudioBufferSize + additionalBytesCount)) == (byte) 0x7F) {
                                            if (ringBuffer.getByte(0) == (checksum ^ ringBuffer.getByte(0))) {
                                                startRecording();
                                                AudioTransmissionStart();
                                                initializeState = initState.INITIALIZED;
                                                currBlockNumber = ((ringBuffer.getByte(-6) & 0xFF) << 8) | (ringBuffer.getByte(-7) & 0xFF);
                                                lastBlockNumber = currBlockNumber;
                                                for (Stage consumer : consumerSet) {
                                                    consumer.start();
                                                }
                                                Log.d(LOG, "ConnectedThread::RUN::INITIALIZED");
                                            }
                                            checksum = 0;
                                        }
                                        break;
                                    case INITIALIZED:
                                        if (ringBuffer.getByte(2 - (AudioBufferSize + additionalBytesCount)) == (byte) 0xFF && ringBuffer.getByte(1 - (AudioBufferSize + additionalBytesCount)) == (byte) 0x7F) {
                                            if (ringBuffer.getByte(0) == (checksum ^ ringBuffer.getByte(0))) {
                                                currBlockNumber = ((ringBuffer.getByte(-6) & 0xFF) << 8) | (ringBuffer.getByte(-7) & 0xFF);
                                                if (currBlockNumber < lastBlockNumber && lastBlockNumber - currBlockNumber > currBlockNumber + (65536 - lastBlockNumber))
                                                    currBlockNumber += 65536;
                                                if (lastBlockNumber < currBlockNumber) {
                                                    lostBlockCount += currBlockNumber - lastBlockNumber - 1;
                                                    while (lastBlockNumber < currBlockNumber - 1) {
                                                        Log.d(LOG, "CurrentBlock: " + currBlockNumber + "\tLostBlocks: " + lostBlockCount);
                                                        writeData(emptyAudioBlock);
                                                        lastBlockNumber++;
                                                    }
                                                    lastBlockNumber = currBlockNumber % 65536;
                                                    for (int idx = 0; idx < AudioBufferSize / 2; idx++) {
                                                        if (useCalib)
                                                            ringBuffer.setShort((short) (ringBuffer.getShort(3 - (AudioBufferSize + additionalBytesCount) + idx * 2) * calibValues[idx % 2]), 3 - (AudioBufferSize + additionalBytesCount) + idx * 2);
                                                        else
                                                            ringBuffer.setShort((short) (ringBuffer.getShort(3 - (AudioBufferSize + additionalBytesCount) + idx * 2)), 3 - (AudioBufferSize + additionalBytesCount) + idx * 2);
                                                    }
                                                    writeData(ringBuffer.data(3 - (AudioBufferSize + additionalBytesCount), AudioBufferSize));
                                                    lastStreamTimer = System.currentTimeMillis();
                                                } else
                                                    Log.d(LOG, "CurrentBlock: " + currBlockNumber + "\tTOO SLOW!");
                                            }
                                            checksum = 0;
                                        }
                                        break;
                                    case STOP:
                                        Log.d(LOG, "ConnectedThread::RUN::STOP");
                                        if (initializeState == initState.INITIALIZED)
                                            AudioTransmissionEnd();
                                        initializeState = initState.UNINITIALIZED;
                                        bt.getBluetoothService().connectionLost();
                                        bt.getBluetoothService().start(false);
                                }
                            }
                        }
                        lastEmptyPackageTimer = System.currentTimeMillis();
                    } else if (initializeState == initState.INITIALIZED && System.currentTimeMillis() - lastEmptyPackageTimer > timeoutBlockLimit) {
                        for (long count = 0; count < timeoutBlockLimit / millisPerBlock; count++) {
                            lostBlockCount++;
                            lastBlockNumber++;
                            writeData(emptyAudioBlock);
                        }
                        Log.d(LOG, "Transmission Timeout\t");
                        lastEmptyPackageTimer = System.currentTimeMillis();
                    }
                    if (initializeState == initState.INITIALIZED) {
                        if (System.currentTimeMillis() - lastBluetoothPingTimer > alivePingTimeout) {
                            bt.send(" ", false);
                            lastBluetoothPingTimer = System.currentTimeMillis();
                        }
                        if (System.currentTimeMillis() - lastStreamTimer > 5 * 1000) // 5 seconds
                        {
                            if (initializeState == initState.INITIALIZED) AudioTransmissionEnd();
                            initializeState = initState.UNINITIALIZED;
                            bt.getBluetoothService().connectionLost();
                            bt.getBluetoothService().start(false);
                        }
                    }
                }
            } catch (IOException e) {
            }
            stopRecording();
        }
    }
}