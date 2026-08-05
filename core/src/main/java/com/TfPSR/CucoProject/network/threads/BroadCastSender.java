package com.TfPSR.CucoProject.network.threads;

import com.TfPSR.CucoProject.network.protocol.ConnectPacket;
import com.TfPSR.CucoProject.network.protocol.NetworkConfig;

import java.io.IOException;
import java.net.*;

public class BroadCastSender implements Runnable {
    public static boolean connectionStatus;
    public DatagramSocket socket;
    public DatagramPacket connectionData;
    InetAddress broadcastAddress = InetAddress.getByName("127.0.0.1");

    public BroadCastSender() throws SocketException, UnknownHostException {
        System.out.println("llega");
        connectionStatus = false;
        this.socket = new DatagramSocket();
        socket.setBroadcast(true);

        this.connectionData = new DatagramPacket(
            ConnectPacket.BROAD,
            ConnectPacket.BROAD.length,
            broadcastAddress,
            NetworkConfig.BROADCAST_PORT
        );}

    @Override
    public void run() {

        while(!connectionStatus){
            try {
                Thread.sleep(2000);
                System.out.println("Enviando a: " + connectionData.getAddress() + ":" + connectionData.getPort());
                socket.send(connectionData);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
}
