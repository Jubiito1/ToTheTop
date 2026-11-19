package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;

/**
 * Wire format of the match state the server broadcasts at a fixed tick.
 * Mirrors {@code StatePacket} in the server repository; see
 * {@code odd/tasks/server-state-sync.md} there for the full contract.
 *
 * <p>Coordinates are world meters and angles are degrees. Body parts always
 * arrive in this order: head, torso, left upper arm, left forearm, left hand,
 * right upper arm, right forearm, right hand, left leg, right leg.
 *
 * <p>Decoding returns {@code null} for anything malformed: a bad datagram must
 * never be able to break the render loop.
 */
public final class StatePacket {

    public static final byte MATCH_RUNNING = 0;
    public static final byte MATCH_FINISHED = 1;

    public static final byte RESULT_NONE = 0;
    public static final byte RESULT_WIN = 1;
    public static final byte RESULT_LOSE = 2;

    public static final int HEADER_SIZE = 14;
    public static final int PLAYER_HEADER_SIZE = 2;
    public static final int PART_SIZE = 12;
    public static final int PLAYER_TRAILER_SIZE = 2;

    public static final int GRIP_LEFT = 1;
    public static final int GRIP_RIGHT = 1 << 1;
    public static final int FLAG_UNCONSCIOUS = 1;

    private StatePacket() {
    }

    /**
     * One complete snapshot: the overall match state and every player in it.
     * Immutable once built, so it can be handed from the network thread to the
     * render thread without locking.
     *
     * @param timeRemaining seconds left before the match is lost
     * @param timeLimit     seconds the whole match lasts
     * @param breakables    state of every breakable rock in map order: wear
     *                      0..200, or 255 while broken
     */
    public record Snapshot(byte matchState, byte result, byte environmentMask,
                           float timeRemaining, float timeLimit,
                           PlayerState[] players, byte[] breakables) {
    }

    /** Decodes one received datagram, or returns {@code null} when invalid. */
    public static Snapshot decode(byte[] data, int offset, int length) {
        if (data == null || length < HEADER_SIZE)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        if (buffer.get() != NetworkHeaders.STATE)
            return null;
        if (buffer.get() != ProtocolVersion.CURRENT)
            return null;

        byte matchState = buffer.get();
        byte result = buffer.get();
        byte environmentMask = buffer.get();
        float timeRemaining = buffer.getFloat();
        float timeLimit = buffer.getFloat();
        if (!Float.isFinite(timeRemaining) || !Float.isFinite(timeLimit))
            return null;
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

        // Breakable rocks: their count, then one state byte each.
        if (buffer.remaining() < 1)
            return null;
        int breakableCount = buffer.get() & 0xFF;
        if (buffer.remaining() < breakableCount)
            return null;
        byte[] breakables = new byte[breakableCount];
        buffer.get(breakables);

        return new Snapshot(matchState, result, environmentMask, timeRemaining, timeLimit, players, breakables);
    }
}
