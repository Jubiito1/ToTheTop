package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;

public final class StatePacket {

    //Here we collect all the relevant information from the match to update and compare the information and values of
    //the entities

    public static final byte MATCH_RUNNING = 0;
    public static final byte MATCH_FINISHED = 1;

    public static final byte RESULT_NONE = 0;
    public static final byte RESULT_WIN = 1;

    public static final int HEADER_SIZE = 4;
    public static final int PLAYER_HEADER_SIZE = 2;
    public static final int PART_SIZE = 12;
    public static final int PLAYER_TRAILER_SIZE = 2;

    public static final int GRIP_LEFT = 1;
    public static final int GRIP_RIGHT = 1 << 1;
    public static final int FLAG_UNCONSCIOUS = 1;

    private StatePacket() {
    }

    public record Snapshot(byte matchState, byte result, byte environmentMask,
                           PlayerState[] players) {
    }

    public static Snapshot decode(byte[] data, int offset, int length) {
        if (data == null || length < HEADER_SIZE)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        if (buffer.get() != NetworkHeaders.STATE)
            return null;
        if (buffer.get() != ProtocolVersion.CURRENT)
            return null;

        byte matchState = buffer.get();
        int playerCount = buffer.get() & 0xFF;
        if (playerCount > 4)
            return null;

        PlayerState[] players = new PlayerState[playerCount];

        for (int i = 0; i < playerCount; i++) {
            if (buffer.remaining() < PLAYER_HEADER_SIZE)
                return null;

            int slot = buffer.get() & 0xFF;
            int partCount = buffer.get() & 0xFF;
            if (partCount <= 0 || partCount > 32)
                return null;
            if (buffer.remaining() < partCount * PART_SIZE + PLAYER_TRAILER_SIZE)
                return null;

            PartState[] parts = new PartState[partCount];
            for (int part = 0; part < partCount; part++) {
                parts[part] = new PartState(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }

            int grip = buffer.get() & 0xFF;
            int flags = buffer.get() & 0xFF;

            players[i] = new PlayerState(slot, parts,
                    (grip & GRIP_LEFT) != 0,
                    (grip & GRIP_RIGHT) != 0,
                    (flags & FLAG_UNCONSCIOUS) != 0);
        }

        return new Snapshot(matchState, RESULT_NONE, (byte) 0, players);
    }
}
