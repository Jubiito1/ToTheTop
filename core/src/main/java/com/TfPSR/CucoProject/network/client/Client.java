package com.TfPSR.CucoProject.network.client;

import com.TfPSR.CucoProject.network.protocol.ConnectPacket;
import com.TfPSR.CucoProject.network.protocol.InputPacket;
import com.TfPSR.CucoProject.network.protocol.NetworkConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    public DatagramSocket socket;
    public DatagramPacket packet;
    public boolean conected;
    public byte idAssigned;

    public Client() throws SocketException {
        this.socket = new DatagramSocket(NetworkConfig.BROADCAST_PORT);
    }

    public void initializeConnection(InetAddress address) throws IOException {
            socket.connect(address, NetworkConfig.SERVER_PORT);
            byte[] buf = new byte[InputPacket.SIZE];
            DatagramPacket dataPacket = new DatagramPacket(buf, buf.length); //Creamos para almacenar la respuesta
            this.packet = new DatagramPacket(ConnectPacket.BYTES, ConnectPacket.SIZE);  //Creamos para enviar peticion
            socket.send(packet);
            socket.receive(dataPacket);

            idAssigned = dataPacket.getData()[1];
    }

    public InetAddress broadcastConnection() throws IOException {
        socket.setBroadcast(true);
        byte[] buf = new byte[InputPacket.SIZE];
        DatagramPacket dataPacket = new DatagramPacket(buf, buf.length);

        socket.receive(dataPacket);

        return dataPacket.getAddress();
    }

    public void inputPerTick(InputPacket inputPacket) throws IOException {
        inputPacket.clientId = idAssigned;
        byte[] inputPacketBytes = inputPacket.toBytes();
        DatagramPacket dataInputPacket = new DatagramPacket(inputPacketBytes, inputPacketBytes.length);
        socket.send(dataInputPacket);
    }

    public void run() {
        try{

            initializeConnection(broadcastConnection());
        }catch (IOException e) {
            e.printStackTrace();
        }
    }



}

