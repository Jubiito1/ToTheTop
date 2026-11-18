package com.TfPSR.ToTheTop.network;

import com.badlogic.gdx.graphics.Color;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Roster announced by the server: who is in the room, with the colour the
 * server assigned to its slot and the name it registered on CONNECT.
 *
 * <p>Immutable once built, so the network thread can replace it while the
 * render thread reads the previous one without locking.
 */
public final class Roster {

    /** Room phases, as the server sends them. */
    public static final int PHASE_LOBBY = 0;
    public static final int PHASE_RUNNING = 1;
    public static final int PHASE_FINISHED = 2;

    /** ROSTER value of the host slot when the room is empty. */
    private static final int NO_HOST = 0xFF;

    /**
     * One player of the room. Colours are the raw bytes of the palette.
     * {@code inLobby} is true after a match once the player left the result screen.
     */
    public record Entry(int slot, boolean ready, boolean inLobby, int red, int green, int blue, String name) {

        /** Slot colour, ready to tint the sprites and the name label. */
        public Color color() {
            return new Color(red / 255f, green / 255f, blue / 255f, 1f);
        }
    }

    private static final Roster EMPTY = new Roster(List.of(), 0, 0, -1, PHASE_LOBBY);

    private final List<Entry> entries;
    private final int playerCount;
    private final int readyCount;
    private final int hostSlot;
    private final int phase;

    private Roster(List<Entry> entries, int playerCount, int readyCount, int hostSlot, int phase) {
        this.entries = entries;
        this.playerCount = playerCount;
        this.readyCount = readyCount;
        this.hostSlot = hostSlot;
        this.phase = phase;
    }

    /** Roster used before the first packet arrives. */
    public static Roster empty() {
        return EMPTY;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int playerCount() {
        return playerCount;
    }

    public int readyCount() {
        return readyCount;
    }

    /** Slot that may start the match, or -1 with an empty room. */
    public int hostSlot() {
        return hostSlot;
    }

    /** {@link #PHASE_LOBBY}, {@link #PHASE_RUNNING} or {@link #PHASE_FINISHED}. */
    public int phase() {
        return phase;
    }

    /** Players still looking at the result screen of the last match. */
    public int playersOnResults() {
        if (phase != PHASE_FINISHED)
            return 0;
        int count = 0;
        for (Entry entry : entries) {
            if (!entry.inLobby())
                count++;
        }
        return count;
    }

    /** Entry of one slot, or {@code null} when the slot is free. */
    public Entry entry(int slot) {
        for (Entry entry : entries) {
            if (entry.slot() == slot)
                return entry;
        }
        return null;
    }

    /** Decodes one received datagram, or returns {@code null} when invalid. */
    public static Roster decode(byte[] data, int offset, int length) {
        if (data == null || length < 6)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        if (buffer.get() != NetworkHeaders.ROSTER)
            return null;
        if (buffer.get() != ProtocolVersion.CURRENT)
            return null;

        int playerCount = buffer.get() & 0xFF;
        int readyCount = buffer.get() & 0xFF;
        int host = buffer.get() & 0xFF;
        int phase = buffer.get() & 0xFF;
        if (phase > PHASE_FINISHED)
            return null;

        List<Entry> entries = new ArrayList<>();
        while (buffer.remaining() >= 6) {
            int slot = buffer.get() & 0xFF;
            int flags = buffer.get() & 0xFF;
            boolean ready = (flags & 1) != 0;
            boolean inLobby = (flags & 2) != 0;
            int red = buffer.get() & 0xFF;
            int green = buffer.get() & 0xFF;
            int blue = buffer.get() & 0xFF;
            int nameLength = buffer.get() & 0xFF;

            if (buffer.remaining() < nameLength)
                return null;

            byte[] name = new byte[nameLength];
            buffer.get(name);
            entries.add(new Entry(slot, ready, inLobby, red, green, blue,
                    new String(name, StandardCharsets.UTF_8)));
        }

        return new Roster(List.copyOf(entries), playerCount, readyCount,
                host == NO_HOST ? -1 : host, phase);
    }
}
