package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;

public final class InputPacket {

    public static final int SIZE = 14;

    public static final int BUTTON_LEFT = 1;
    public static final int BUTTON_RIGHT = 1 << 1;

    public static final int KEY_A = 1;
    public static final int KEY_D = 1 << 1;
    public static final int KEY_W = 1 << 2;

    /** One-off events: the client repeats each bit for a few ticks. */
    public static final int EVENT_IMPULSE = 1;
    public static final int EVENT_PAUSE = 1 << 1;
    public static final int EVENT_RESUME = 1 << 2;
    public static final int EVENT_LEAVE = 1 << 3;

    private InputPacket() {
    }

    public static byte[] encode(float mouseX, float mouseY,
                                boolean left, boolean right,
                                boolean a, boolean d, boolean w,
                                int session, int events) {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        buffer.put(NetworkHeaders.INPUT);
        buffer.put(ProtocolVersion.CURRENT);
        buffer.put((byte) session);
        buffer.putFloat(mouseX);
        buffer.putFloat(mouseY);
        buffer.put(buttons(left, right));
        buffer.put(keys(a, d, w));
        buffer.put((byte) events);
        return buffer.array();
    }

    private static byte buttons(boolean left, boolean right) {
        int mask = 0;
        if (left)
            mask |= BUTTON_LEFT;
        if (right)
            mask |= BUTTON_RIGHT;
        return (byte) mask;
    }

    private static byte keys(boolean a, boolean d, boolean w) {
        int mask = 0;
        if (a)
            mask |= KEY_A;
        if (d)
            mask |= KEY_D;
        if (w)
            mask |= KEY_W;
        return (byte) mask;
    }
}
