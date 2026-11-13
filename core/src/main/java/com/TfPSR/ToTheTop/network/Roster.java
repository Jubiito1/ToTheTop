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

    /** One player of the room. Colours are the raw bytes of the palette. */
    public record Entry(int slot, boolean ready, int red, int green, int blue, String name) {

        /** Slot colour, ready to tint the sprites and the name label. */
        public Color color() {
            return new Color(red / 255f, green / 255f, blue / 255f, 1f);
        }
    }

    private static final Roster EMPTY = new Roster(List.of(), 0, 0);

    private final List<Entry> entries;
    private final int playerCount;
    private final int readyCount;

    private Roster(List<Entry> entries, int playerCount, int readyCount) {
        this.entries = entries;
        this.playerCount = playerCount;
        this.readyCount = readyCount;
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
        if (data == null || length < 4)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        if (buffer.get() != NetworkHeaders.ROSTER)
            return null;
        if (buffer.get() != ProtocolVersion.CURRENT)
            return null;

        int playerCount = buffer.get() & 0xFF;
        int readyCount = buffer.get() & 0xFF;

        List<Entry> entries = new ArrayList<>();
        while (buffer.remaining() >= 6) {
            int slot = buffer.get() & 0xFF;
            boolean ready = buffer.get() != 0;
            int red = buffer.get() & 0xFF;
            int green = buffer.get() & 0xFF;
            int blue = buffer.get() & 0xFF;
            int nameLength = buffer.get() & 0xFF;

            if (buffer.remaining() < nameLength)
                return null;

            byte[] name = new byte[nameLength];
            buffer.get(name);
            entries.add(new Entry(slot, ready, red, green, blue,
                    new String(name, StandardCharsets.UTF_8)));
        }

        return new Roster(List.copyOf(entries), playerCount, readyCount);
    }
}
