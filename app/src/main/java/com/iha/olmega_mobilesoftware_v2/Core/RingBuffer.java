package com.iha.olmega_mobilesoftware_v2.Core;

public class RingBuffer {
    private byte[] Data;
    private int idx;

    public RingBuffer(int BufferSize) {
        Data = new byte[BufferSize];
        idx = 0;
    }

    public void addByte(byte data) {
        idx = (idx + 1) % Data.length;
        Data[idx] = data;
    }

    public void setByte(byte data, int currIdx) {
        currIdx = (idx + currIdx) % Data.length;
        if (currIdx < 0) currIdx += Data.length;
        Data[currIdx] = data;
    }

    public byte getByte(int currIdx)
    {
        currIdx = (idx + currIdx) % Data.length;
        if (currIdx < 0) currIdx += Data.length;
        return Data[currIdx];
    }

    public short getShort(int currIdx)
    {
        return (short)(((getByte(currIdx + 1) & 0xFF) << 8) | (getByte(currIdx) & 0xFF));
    }

    public void setShort(short Value, int currIdx) {
        setByte((byte)((Value >> 8) & 0xFF), currIdx + 1);
        setByte((byte)(Value & 0xFF),currIdx);
    }

    public byte[] data(int startIdx, int length) {
        byte[] returnArray = new byte[length];
        startIdx = (idx + startIdx) % Data.length;
        if (startIdx < 0) startIdx += Data.length;
        int tmpLen = Math.min(length, Data.length - startIdx);
        System.arraycopy(Data, startIdx, returnArray, 0, tmpLen);
        if (tmpLen != length)
            System.arraycopy(Data, 0, returnArray, tmpLen, length - tmpLen);
        return returnArray;
    }
}
