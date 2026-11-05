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
    private volatile int slot = -1;         // slot que el server asigno en ACCEPT
    private volatile int session = 0;       // id de sesion que el server asigno en ACCEPT
    private volatile StatePacket.Snapshot latestSnapshot;

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


    public StatePacket.Snapshot getSnapshot() {
        return latestSnapshot;
    }

    public void setReady(boolean ready) {
        wantReady = ready;
        if (status == Status.ACCEPTED)
            trySendSession(ready ? NetworkHeaders.READY : NetworkHeaders.UNREADY);
    }

    /** Sends a packet that carries only header, version and session. */
    private void sendSession(byte header) throws IOException {
        byte[] buf = new byte[]{header, ProtocolVersion.CURRENT, (byte) session};
        DatagramPacket data = new DatagramPacket(buf, buf.length, ipServer, PORT);
        conection.send(data);
    }

    private void send(byte header) throws IOException {
        byte[] buf = new byte[]{header, ProtocolVersion.CURRENT};
        DatagramPacket data = new DatagramPacket(buf, buf.length, ipServer, PORT);
        conection.send(data);
    }

    private void trySendSession(byte header) {
        try {
            sendSession(header);
        } catch (IOException ignored) {
            // si no se puede enviar, el timeout del loop reintenta
        }
    }

    private void trySend(byte header) {
        try {
            send(header);
        } catch (IOException ignored) {
            // si no se puede enviar, el timeout del loop reintenta
        }
    }

    public void sendInput(float mouseX, float mouseY, boolean left, boolean right,
                          boolean a, boolean d, int events) {
        if (!running || status != Status.STARTED)
            return;
        try {
            byte[] buf = InputPacket.encode(mouseX, mouseY, left, right, a, d, session, events);
            conection.send(new DatagramPacket(buf, buf.length, ipServer, PORT));
        } catch (IOException ignored) {
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

                if (status == Status.REJECTED)
                    return;
            } catch (SocketTimeoutException e) {
                // el socket tiene que quedar abierto durante la partida para seguir
                // mandando input; solo se reintenta CONNECT mientras no haya partida
                if (running && (status == Status.SEARCHING || status == Status.ACCEPTED))
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
                if (dp.getLength() >= off + 4 && data[off + 1] == ProtocolVersion.CURRENT) {
                    session = data[off + 2] & 0xFF;
                    slot = data[off + 3] & 0xFF;
                }
                status = Status.ACCEPTED;
                if (wantReady)
                    trySendSession(NetworkHeaders.READY);
            }
            case NetworkHeaders.ROSTER -> {
                if (dp.getLength() >= off + 4 && data[off + 1] == ProtocolVersion.CURRENT) {
                    players = data[off + 2] & 0xFF;
                    readyCount = data[off + 3] & 0xFF;
                }
            }
            case NetworkHeaders.STATE -> latestSnapshot =
                    StatePacket.decode(data, off, dp.getLength()); //If the 'header' of the package is state, we decode it and save it into a lastSnapshot type
            case NetworkHeaders.REJECT -> status = Status.REJECTED;
            case NetworkHeaders.START -> {
                status = Status.STARTED;
                StartFlag.startFlag = true;   // la WaitingMenu la lee en el render thread
            }
        }
    }

    /**
     * Leaves the match: the event travels in the input packet, repeated a few
     * times so a lost datagram does not keep the slot open, and then the socket
     * is closed. The server also frees the slot through {@code DISCONNECT}.
     */
    public void leave() {
        if (status == Status.STARTED) {
            for (int i = 0; i < 3; i++) {
                try {
                    byte[] buf = InputPacket.encode(0f, 0f, false, false, false, false,
                            session, InputPacket.EVENT_LEAVE);
                    conection.send(new DatagramPacket(buf, buf.length, ipServer, PORT));
                } catch (IOException ignored) {
                }
            }
        }
        disconnect();
    }

    /** Cierra el socket y corta el hilo. El server libera el slot con DISCONNECT. */
    public void disconnect() {
        running = false;
        try {
            if (conection != null && !conection.isClosed())
                sendSession(NetworkHeaders.DISCONNECT);
        } catch (IOException ignored) {
            // si no se puede avisar, el server lo detecta por timeout
        }
        if (conection != null)
            conection.close();
    }

}
