package com.TfPSR.ToTheTop.network;


import com.TfPSR.ToTheTop.StartFlag;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ThreadClient extends Thread {
    public enum Status { SEARCHING, ACCEPTED, REJECTED, STARTED }

    private static final int PORT = 6767;
    private static final int TIMEOUT_MS = 3000;

    private final DatagramSocket conection;
    private InetAddress ipServer;
    private volatile boolean running = true;

    private volatile Status status = Status.SEARCHING;
    private volatile int players = 0;      // ultima cantidad de conectados (ROSTER)
    private volatile int readyCount = 0;    // ultima cantidad de listos (ROSTER)
    private volatile boolean wantReady = false; // estado pedido por la UI

    public ThreadClient() throws IOException {
        ipServer = InetAddress.getByName("255.255.255.255");
        try {
            conection = new DatagramSocket();
            conection.setSoTimeout(TIMEOUT_MS);
        } catch (SocketException e) {
            e.printStackTrace();
            throw new IOException("No se pudo crear el socket del cliente", e);
        }
    }

    public Status getStatus() {
        return status;
    }

    public int getPlayers() {
        return players;
    }

    public int getReadyCount() {
        return readyCount;
    }

    /**
     * Marca/desmarca al jugador como listo. Si la conexion todavía no fue aceptada
     * guarda el pedido y lo reenvia apenas llega el ACCEPT (evita perder el click).
     */
    public void setReady(boolean ready) {
        wantReady = ready;
        if (status == Status.ACCEPTED)
            trySend(ready ? NetworkHeaders.READY : NetworkHeaders.UNREADY);
    }

    private void send(byte header) throws IOException {
        byte[] buf = new byte[]{header};
        DatagramPacket data = new DatagramPacket(buf, buf.length, ipServer, PORT);
        conection.send(data);
    }

    /** Envia sin lanzar excepcion, para usar desde el render thread. */
    private void trySend(byte header) {
        try {
            send(header);
        } catch (IOException ignored) {
            // si no se puede enviar, el timeout del loop reintenta
        }
    }

    @Override
    public void run() {
        // primer CONNECT: el loop solo lo repite si nadie contesta (timeout)
        trySend(NetworkHeaders.CONNECT);

        while (running) {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                conection.receive(dp);
                processMessage(dp);

                if (status == Status.STARTED) {
                    // la partida arranco: se cierra el socket y el hilo muere
                    conection.close();
                    return;
                }
                if (status == Status.REJECTED)
                    return;
            } catch (SocketTimeoutException e) {
                // nadie contesto: se reintenta (descubrimiento o keepalive)
                if (running && status != Status.REJECTED)
                    trySend(NetworkHeaders.CONNECT);
            } catch (SocketException e) {
                if (running)
                    e.printStackTrace();
                return;
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
                return;
            }
        }
    }

    private void processMessage(DatagramPacket dp) {
        if (dp.getLength() < 1)
            return;

        byte[] data = dp.getData();
        int off = dp.getOffset();

        switch (data[off]) {
            case NetworkHeaders.ACCEPT -> {
                ipServer = dp.getAddress();   // ya no broadcast: se habla con el server
                status = Status.ACCEPTED;
                if (wantReady)
                    trySend(NetworkHeaders.READY);
            }
            case NetworkHeaders.ROSTER -> {
                if (dp.getLength() >= 3) {
                    players = data[off + 1] & 0xFF;
                    readyCount = data[off + 2] & 0xFF;
                }
            }
            case NetworkHeaders.REJECT -> status = Status.REJECTED;
            case NetworkHeaders.START -> {
                status = Status.STARTED;
                StartFlag.startFlag = true;   // la WaitingMenu la lee en el render thread
            }
        }
    }

    /** Cierra el socket y corta el hilo. El server libera el slot con DISCONNECT. */
    public void disconnect() {
        running = false;
        try {
            if (conection != null && !conection.isClosed())
                send(NetworkHeaders.DISCONNECT);
        } catch (IOException ignored) {
            // si no se puede avisar, el server lo detecta por timeout
        }
        if (conection != null)
            conection.close();
    }

}
