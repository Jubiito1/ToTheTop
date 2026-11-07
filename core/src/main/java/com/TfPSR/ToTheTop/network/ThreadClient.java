package com.TfPSR.ToTheTop.network;


import com.TfPSR.ToTheTop.StartFlag;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

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
    private volatile String roomCode = "";  // codigo de 4 cifras anunciado por el server
    private volatile int maxPlayers = 4;    // cupo de la sala (ANNOUNCE)
    private volatile boolean matchRunning = false; // la sala ya esta en partida (ANNOUNCE)
    private volatile int rejectReason = 0;  // motivo del ultimo REJECT
    private volatile StatePacket.Snapshot latestSnapshot;

    public ThreadClient() throws IOException {
        ipServer = InetAddress.getByName("255.255.255.255");
        try {
            conection = new DatagramSocket();
            conection.setSoTimeout(TIMEOUT_MS);
            // El primer paso es preguntar quien hay en la red, y eso se manda
            // a la direccion de broadcast.
            conection.setBroadcast(true);
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

    /** Slot the server assigned to this client, or -1 before ACCEPT. */
    public int getSlot() {
        return slot;
    }

    /** Session id the server assigned to this client, or 0 before ACCEPT. */
    public int getSession() {
        return session;
    }

    /** Four-digit room code announced by the server, or "" before ANNOUNCE. */
    public String getRoomCode() {
        return roomCode;
    }

    /** Room capacity announced by the server. */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /** True when the announced room already started its match. */
    public boolean isMatchRunning() {
        return matchRunning;
    }

    /** Reason of the last REJECT, or 0 when there was none. */
    public int getRejectReason() {
        return rejectReason;
    }

    /**
     * Last state snapshot decoded from the server, or {@code null} before the
     * first one arrives. The object is immutable, so the render thread can read
     * it without locking.
     */
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

    /** Asks every server on the network to announce itself. */
    private void sendDiscover() throws IOException {
        byte[] buf = new byte[]{NetworkHeaders.DISCOVER, ProtocolVersion.CURRENT};
        DatagramPacket data = new DatagramPacket(buf, buf.length, ipServer, PORT);
        conection.send(data);
    }

    /**
     * Joins the room once its code is known: CONNECT carries the room code that
     * was announced and the player name (empty until the waiting room adds it).
     */
    private void sendConnect() throws IOException {
        if (roomCode.length() != 4)
            return;

        byte[] buf = ConnectPacket.encode(roomCode, "");
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

    private void trySendDiscover() {
        try {
            sendDiscover();
        } catch (IOException ignored) {
            // si no se puede enviar, el timeout del loop reintenta
        }
    }

    private void trySendConnect() {
        try {
            sendConnect();
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
        // Primero se pregunta quien hay en la red: el CONNECT necesita el codigo
        // de sala, y ese codigo solo llega con el ANNOUNCE.
        trySendDiscover();

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
                // mandando input; solo se reintenta el handshake mientras no haya partida
                if (!running || status == Status.STARTED)
                    continue;
                if (roomCode.length() == 4)
                    trySendConnect();
                else
                    trySendDiscover();
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
            case NetworkHeaders.ANNOUNCE -> {
                if (dp.getLength() >= off + 9 && data[off + 1] == ProtocolVersion.CURRENT) {
                    ipServer = dp.getAddress();   // a partir de aca se habla con el server
                    roomCode = new String(data, off + 2, 4, StandardCharsets.US_ASCII);
                    players = data[off + 6] & 0xFF;
                    matchRunning = data[off + 7] != 0;
                    maxPlayers = data[off + 8] & 0xFF;

                    if (status == Status.SEARCHING)
                        trySendConnect();
                }
            }
            case NetworkHeaders.ACCEPT -> {
                ipServer = dp.getAddress();   // ya no broadcast: se habla con el server
                if (dp.getLength() >= off + 8 && data[off + 1] == ProtocolVersion.CURRENT) {
                    session = data[off + 2] & 0xFF;
                    slot = data[off + 3] & 0xFF;
                    roomCode = new String(data, off + 4, 4, StandardCharsets.US_ASCII);
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
                    StatePacket.decode(data, off, dp.getLength());
            case NetworkHeaders.REJECT -> {
                if (dp.getLength() >= off + 3)
                    rejectReason = data[off + 2] & 0xFF;
                status = Status.REJECTED;
            }
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
