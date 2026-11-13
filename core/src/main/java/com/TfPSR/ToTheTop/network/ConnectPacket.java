package com.TfPSR.ToTheTop.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Wire format of the join request, mirroring the server decoder.
 *
 * Layout: header, protocol version, four ASCII room-code bytes, name length,
 * name in UTF-8. The code is the one the server announced, so a client only
 * takes a slot in the room it meant to join.
 */
public final class ConnectPacket {

    public static final int NAME_MAX_BYTES = 16;

    private ConnectPacket() {
    }

    /**
     * @param roomCode four-digit code announced by the server
     * @param name     player name, cut to {@link #NAME_MAX_BYTES} bytes
     */
    public static byte[] encode(String roomCode, String name) {
        if (roomCode == null || roomCode.length() != 4)
            throw new IllegalArgumentException("roomCode must have four characters");

        byte[] nameBytes = truncate(name == null ? "" : name);

        ByteBuffer buffer = ByteBuffer.allocate(7 + nameBytes.length);
        buffer.put(NetworkHeaders.CONNECT);
        buffer.put(ProtocolVersion.CURRENT);
        buffer.put(roomCode.getBytes(StandardCharsets.US_ASCII), 0, 4);
        buffer.put((byte) nameBytes.length);
        buffer.put(nameBytes);
        return buffer.array();
    }

    /** Cuts the name to the byte budget without splitting a UTF-8 character. */
    private static byte[] truncate(String name) {
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= NAME_MAX_BYTES)
            return bytes;

        int end = NAME_MAX_BYTES;
        while (end > 0 && (bytes[end] & 0xC0) == 0x80)
            end--;

        byte[] cut = new byte[end];
        System.arraycopy(bytes, 0, cut, 0, end);
        return cut;
    }
}
