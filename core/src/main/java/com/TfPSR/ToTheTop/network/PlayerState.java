package com.TfPSR.ToTheTop.network;

/**
 * One player inside a {@link StatePacket}: its slot, the state of every body
 * part in wire order and the flags the client needs to render it. Mirrors the
 * server record of the same name.
 */
public record PlayerState(int slot,
                          PartState[] parts,
                          boolean leftGripped,
                          boolean rightGripped,
                          boolean unconscious) {
}
