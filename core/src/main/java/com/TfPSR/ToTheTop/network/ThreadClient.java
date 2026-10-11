package com.TfPSR.ToTheTop.network;


import com.TfPSR.ToTheTop.StartFlag;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ThreadClient extends Thread{

    private DatagramSocket conection;
    private InetAddress ipServer;
    private int port = 6767;
    private boolean fin=true;

    public ThreadClient() throws IOException {
        try {
            ipServer = InetAddress.getByName("255.255.255.255");
            conection = new DatagramSocket();
        }catch (SocketException e){
            e.printStackTrace();
        }

    }

    public void sendMessage(String string) throws IOException {
        byte[] buf = string.getBytes();
        DatagramPacket data = new DatagramPacket(buf, buf.length, ipServer, port);
        conection.send(data);
    }

    @Override
    public void run(){
        try{
            sendMessage("Conexion");
            while(fin){
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                try{
                    conection.receive(dp);
                    processMessage(dp);
                }catch (SocketException e){
                    sendMessage("Conexion");
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void processMessage(DatagramPacket dp){
        String message = new String(dp.getData(), dp.getOffset(), dp.getLength(), StandardCharsets.UTF_8);
        if(message.equals("OK")){
            ipServer= dp.getAddress();
        } else if (message.equals("Empieza")) {
            StartFlag.startFlag = true;
        }
    }

}
