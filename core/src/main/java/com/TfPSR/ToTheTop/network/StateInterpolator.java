package com.TfPSR.ToTheTop.network;

/**
 * Blends two consecutive {@link StatePacket.Snapshot}s so the client renders a
 * smooth motion instead of a new pose every received packet.
 *
 * <p>Players are matched by slot. A slot that exists only in one of the two
 * snapshots is rendered as it is in the newest one; parts are interpolated only
 * when both snapshots describe the same count. Angles take the shortest arc, so
 * a limb crossing 180 degrees does not spin the long way around.
 */
public final class StateInterpolator {

    private StateInterpolator() {
    }

    public static StatePacket.Snapshot interpolate(StatePacket.Snapshot previous,
                                                   StatePacket.Snapshot current,
                                                   float alpha) {
        if (current == null)
            return previous;
        if (previous == null)
            return current;

        float t = Math.max(0f, Math.min(1f, alpha));
        PlayerState[] blended = new PlayerState[current.players().length];

        for (int i = 0; i < current.players().length; i++) {
            PlayerState next = current.players()[i];
            PlayerState before = findBySlot(previous, next.slot());
            blended[i] = before == null ? next : blend(before, next, t);
        }

        return new StatePacket.Snapshot(current.matchState(), current.result(),
                current.environmentMask(), current.timeRemaining(), current.timeLimit(), blended,
                current.breakables());
    }

    private static PlayerState findBySlot(StatePacket.Snapshot snapshot, int slot) {
        for (PlayerState player : snapshot.players()) {
            if (player.slot() == slot)
                return player;
        }
        return null;
    }

    private static PlayerState blend(PlayerState before, PlayerState next, float t) {
        int count = Math.min(before.parts().length, next.parts().length);
        PartState[] parts = new PartState[count];

        for (int i = 0; i < count; i++) {
            PartState a = before.parts()[i];
            PartState b = next.parts()[i];
            parts[i] = new PartState(
                    a.x() + (b.x() - a.x()) * t,
                    a.y() + (b.y() - a.y()) * t,
                    shortestArc(a.angleDegrees(), b.angleDegrees(), t));
        }

        return new PlayerState(next.slot(), parts,
                next.leftGripped(), next.rightGripped(), next.unconscious());
    }

    /** Moves {@code from} towards {@code to} along the shortest circumference. */
    static float shortestArc(float from, float to, float t) {
        float delta = ((to - from + 540f) % 360f) - 180f;
        return from + delta * t;
    }
}
