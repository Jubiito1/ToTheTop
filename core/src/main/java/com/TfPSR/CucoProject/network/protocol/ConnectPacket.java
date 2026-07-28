package com.TfPSR.CucoProject.network.protocol;

public class ConnectPacket {
    //Paquete incial, para solicitar la conexion
    public static final int SIZE = 1;
    public static final byte[] BYTES = {PacketType.CONNECT};
    public static final byte[] BROAD = {PacketType.BROADCAST};
}
