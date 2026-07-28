package com.TfPSR.CucoProject.network.protocol;

import java.nio.ByteBuffer;

public class InputPacket {
    //Aqui armamos el paquete que enviaremos por red, sera solamente las teclas presionadas y la posicion del mouse

    public static final int SIZE = 14;

    public float positionMouseX;
    public float positionMouseY;
    public boolean leftClick;
    public boolean rightClick;
    public boolean keyD;
    public boolean keyA;
    public byte clientId;

    public byte[] toBytes(){
        ByteBuffer buf = ByteBuffer.allocate(SIZE);
        buf.put(PacketType.INPUT);
        buf.put(clientId);
        buf.put((byte) positionMouseX);
        buf.put((byte) positionMouseY);
        buf.put(leftClick? (byte) 1: (byte) 0);
        buf.put(rightClick? (byte) 1: (byte) 0);
        buf.put(keyD? (byte) 1: (byte) 0);
        buf.put(keyA? (byte) 1: (byte) 0);

        return buf.array();
    }

}
