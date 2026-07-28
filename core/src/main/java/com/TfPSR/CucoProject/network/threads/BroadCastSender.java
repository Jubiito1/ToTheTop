package com.TfPSR.CucoProject.network.threads;

import com.TfPSR.CucoProject.network.protocol.ConnectPacket;
import com.TfPSR.CucoProject.network.protocol.NetworkConfig;

import java.io.IOException;
import java.net.*;

public class BroadCastSender implements Runnable {
    public boolean connectionStatus;
    public DatagramSocket socket;
    public DatagramPacket connectionData;
    InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

    public BroadCastSender() throws SocketException, UnknownHostException {
        this.connectionStatus = false;
        this.socket = new DatagramSocket();

        this.connectionData = new DatagramPacket(ConnectPacket.BROAD,1,  broadcastAddress, NetworkConfig.BROADCAST_PORT);
    }

    @Override
    public void run() {

        while(!connectionStatus){
            try {
                Thread.sleep(2000);
                socket.send(connectionData);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
}
