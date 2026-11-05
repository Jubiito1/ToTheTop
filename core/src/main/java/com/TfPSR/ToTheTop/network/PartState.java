package com.TfPSR.ToTheTop.network;

/**
 * State of one body part as it arrives from the server: world meters and
 * degrees. Mirrors the server record of the same name.
 */
public record PartState(float x, float y, float angleDegrees) {
}
