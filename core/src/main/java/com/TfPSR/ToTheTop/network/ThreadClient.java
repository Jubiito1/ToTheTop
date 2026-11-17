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

    /** Socket poll: nothing blocks forever, so the keepalive can run. */
    private static final int POLL_MS = 250;

    /** How often a handshake is retried while there is no room yet. */
    private static final long HANDSHAKE_INTERVAL_MS = 2000;

    /** How often a joined client tells the server it is still there. */
    private static final long PING_INTERVAL_MS = 1000;

    /** Silence after which the server is considered gone. */
    private static final long SERVER_TIMEOUT_MS = 5000;

    private final DatagramSocket conection;
    private InetAddress ipServer;
    private volatile boolean running = true;

    private volatile Status status = Status.SEARCHING;
    private volatile int players = 0;      // last count of connected players (ROSTER)
    private volatile int readyCount = 0;    // last count of ready players (ROSTER)
    private volatile boolean wantReady = false; // state requested by the UI
    private volatile int slot = -1;         // slot the server assigned in ACCEPT
    private volatile int session = 0;       // session id the server assigned in ACCEPT
    private volatile String roomCode = "";  // 4-digit code announced by the server
    private volatile int maxPlayers = 4;    // capacity of the room (ANNOUNCE)
    private volatile boolean matchRunning = false; // the room is already in a match (ANNOUNCE)
    private volatile int rejectReason = 0;  // reason of the last REJECT
    private volatile Roster roster = Roster.empty(); // who is in the room and in which colour
    private volatile StatePacket.Snapshot latestSnapshot;

    /** Keepalive bookkeeping: last packet received, last ping sent, last handshake. */
    private volatile long lastPacketAt = System.currentTimeMillis();
    private long lastPingAt;
    private long lastHandshakeAt;

    /** Set when the server stops answering, so the screens can go back to the menu. */
    private volatile boolean connectionLost = false;

    public ThreadClient() throws IOException {
        ipServer = InetAddress.getByName("255.255.255.255");
        try {
            conection = new DatagramSocket();
            conection.setSoTimeout(POLL_MS);
            // The first step is asking who is on the network, and that goes
            // to the broadcast address.
            conection.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
            throw new IOException("Could not create the client socket", e);
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

    /** Slot 0 is the host: only it asks the server to start the match. */
    public boolean isHost() {
        return slot == 0;
    }

    /** True when the server stopped answering and the connection must be abandoned. */
    public boolean isConnectionLost() {
        return connectionLost;
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

    /** Last roster announced by the server; empty before the first one. */
    public Roster getRoster() {
        return roster;
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

    /** Hosts only: asks the server to start the match with the ready players. */
    public void requestStart() {
        if (status == Status.ACCEPTED && isHost())
            trySendSession(NetworkHeaders.START_REQUEST);
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
            // if it cannot be sent, the loop timeout retries
        }
    }

    private void trySendDiscover() {
        try {
            sendDiscover();
        } catch (IOException ignored) {
            // if it cannot be sent, the loop timeout retries
        }
    }

    private void trySendConnect() {
        try {
            sendConnect();
        } catch (IOException ignored) {
            // if it cannot be sent, the loop timeout retries
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
        // First ask who is on the network: the CONNECT needs the room code,
        // and that code only arrives with the ANNOUNCE.
        lastHandshakeAt = System.currentTimeMillis();
        trySendDiscover();

        while (running) {
            try {
                byte[] buf = new byte[2048];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                conection.receive(dp);
                processMessage(dp);

                if (status == Status.REJECTED)
                    return;
            } catch (SocketTimeoutException e) {
                // the socket stays open during the match to keep sending
                // input; the timeout only paces the keepalive
            } catch (SocketException e) {
                if (running)
                    e.printStackTrace();
                return;
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
                return;
            }

            if (!running)
                return;

            keepalive(System.currentTimeMillis());
            if (connectionLost)
                return;
        }
    }

    /**
     * Resends the handshake while the room is not joined yet, pings the server
     * once it is, and gives up after five seconds of silence.
     *
     * <p>Once ACCEPTED the handshake stops: a lost ACCEPT leaves the client
     * SEARCHING, so it keeps retrying, while a joined client relies on the
     * keepalive instead of reopening the join every two seconds.
     */
    private void keepalive(long now) {
        if (status == Status.SEARCHING && now - lastHandshakeAt >= HANDSHAKE_INTERVAL_MS) {
            lastHandshakeAt = now;
            if (roomCode.length() == 4)
                trySendConnect();
            else
                trySendDiscover();
        }

        if (status != Status.ACCEPTED && status != Status.STARTED)
            return;

        if (now - lastPingAt >= PING_INTERVAL_MS) {
            lastPingAt = now;
            trySendSession(NetworkHeaders.PING);
        }

        if (now - lastPacketAt >= SERVER_TIMEOUT_MS)
            connectionLost = true;
    }

    private void processMessage(DatagramPacket dp) {
        if (dp.getLength() < 1)
            return;

        // Any packet from the server proves the connection is still alive.
        lastPacketAt = System.currentTimeMillis();

        byte[] data = dp.getData();
        int off = dp.getOffset();

        switch (data[off]) {
            case NetworkHeaders.ANNOUNCE -> {
                if (dp.getLength() >= off + 9 && data[off + 1] == ProtocolVersion.CURRENT) {
                    ipServer = dp.getAddress();   // from here on we talk to the server
                    roomCode = new String(data, off + 2, 4, StandardCharsets.US_ASCII);
                    players = data[off + 6] & 0xFF;
                    matchRunning = data[off + 7] != 0;
                    maxPlayers = data[off + 8] & 0xFF;

                    if (status == Status.SEARCHING)
                        trySendConnect();
                }
            }
            case NetworkHeaders.ACCEPT -> {
                ipServer = dp.getAddress();   // no more broadcast: we talk to the server
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
                Roster decoded = Roster.decode(data, off, dp.getLength());
                if (decoded != null) {
                    roster = decoded;
                    players = decoded.playerCount();
                    readyCount = decoded.readyCount();
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
                StartFlag.startFlag = true;   // the WaitingMenu reads it on the render thread
            }
            case NetworkHeaders.PONG -> {
                // the keepalive was already counted when the datagram arrived
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

    /** Closes the socket and stops the thread. The server frees the slot on DISCONNECT. */
    public void disconnect() {
        running = false;
        try {
            if (conection != null && !conection.isClosed())
                sendSession(NetworkHeaders.DISCONNECT);
        } catch (IOException ignored) {
            // if it cannot be told, the server detects it by timeout
        }
        if (conection != null)
            conection.close();
    }

}
