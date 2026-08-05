package com.TfPSR.CucoProject.network.server;

import com.TfPSR.CucoProject.network.protocol.*;
import com.TfPSR.CucoProject.network.threads.BroadCastSender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class GameServer implements Runnable{
    DatagramSocket socket;
    boolean serverRunning = false;

    public GameServer() {
    }

    public void start() throws IOException {
        this.socket = new DatagramSocket(NetworkConfig.SERVER_PORT);
        System.out.println("The server started to run");
        serverRunning = true;
        while (serverRunning){
            byte[] buf = new byte[InputPacket.SIZE]; // 1 del tipo, 1 del id, el resto de booleanos para las teclas
            DatagramPacket dataPacket = new DatagramPacket(buf, buf.length);
            socket.receive(dataPacket);
            handlePacket(dataPacket, buf[0]);
        }

    }


    public void handlePacket(DatagramPacket data, byte packetType) throws IOException {
        if(packetType == PacketType.CONNECT){
            BroadCastSender.connectionStatus = true;
            System.out.println("El cliente quiere conectarse");

            DatagramPacket answer = new DatagramPacket(AssignIdPacket.AssignId, AssignIdPacket.AssignId.length, data.getAddress(), data.getPort());

            socket.send(answer);

        }else if(packetType == PacketType.INPUT){
            System.out.println("El cliente quiere enviar un paquete");
        }

    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
