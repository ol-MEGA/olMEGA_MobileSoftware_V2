package com.iha.olmega_mobilesoftware_v2.AFEx.Tools;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class NetworkIO {

    public static void sendUdpPacket(final String timestamp) {

        Thread thread = new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    // prepare data
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    out.write(timestamp.getBytes());
                    //out.write("_".getBytes());
                    //out.write(data.getBytes());
                    //out.write(floatToBytes(data));
                    byte[] buffer = out.toByteArray();

                    //System.out.println("Data: " + buffer.length+ " | " + buffer.toString());
                    //System.out.println("Localhost: " + InetAddress.getLocalHost());
                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket dp = new DatagramPacket(buffer, 0, buffer.length, InetAddress.getLocalHost(), 40007);
                    ds.send(dp);
                } catch (Exception e) {
                    System.out.println("Send failed. " + e.toString());
                }
            }
        }));

        thread.start();

    }

    public static byte[] floatToBytes(float[] data) {

        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);

        for (float value : data)
            buffer.putFloat(value);

        return buffer.array();
    }


}
