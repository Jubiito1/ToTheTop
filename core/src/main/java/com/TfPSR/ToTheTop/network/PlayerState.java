package com.TfPSR.ToTheTop.network;


//Here we manage the states of the player's body
public record PlayerState(int slot,
                          PartState[] parts,
                          boolean leftGripped,
                          boolean rightGripped,
                          boolean unconscious) {
}
