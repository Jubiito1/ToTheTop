package com.TfPSR.CucoProject.network.protocol;

public class PacketType {
    //Aqui, vamos a definir los identificadores de los paquetes que se van a enviar y recibir en la red.
    //Usamos byte ya que mas liviano para enviar que usar int u otro tipo de dato.
    //Los identificadores estan asignados y creados en base a su uso.
    public static final byte BROADCAST = 0x00;
    public static final byte CONNECT = 0x01;
    public static final byte ASSIGN_ID = 0x02;
    public static final byte INPUT = 0x03;
}
