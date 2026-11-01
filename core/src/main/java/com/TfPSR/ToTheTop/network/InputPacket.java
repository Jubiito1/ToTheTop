package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;

public final class InputPacket {

    public static final int SIZE = 12;

    public static final int BUTTON_LEFT = 1;
    public static final int BUTTON_RIGHT = 1 << 1;

    public static final int KEY_A = 1;
    public static final int KEY_D = 1 << 1;

    private InputPacket() {
    }

    public static byte[] encode(float mouseX, float mouseY,
                                boolean left, boolean right,
                                boolean a, boolean d) {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        buffer.put(NetworkHeaders.INPUT);
        buffer.put(ProtocolVersion.CURRENT);
        buffer.putFloat(mouseX);
        buffer.putFloat(mouseY);
        buffer.put(buttons(left, right));
        buffer.put(keys(a, d));
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

    private static byte keys(boolean a, boolean d) {
        int mask = 0;
        if (a)
            mask |= KEY_A;
        if (d)
            mask |= KEY_D;
        return (byte) mask;
    }
}
