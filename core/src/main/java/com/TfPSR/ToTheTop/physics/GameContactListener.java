package com.TfPSR.ToTheTop.physics;

import com.TfPSR.ToTheTop.entity.Arm;
import com.badlogic.gdx.physics.box2d.*;

public class GameContactListener implements ContactListener {


    @Override
    public void beginContact(Contact contact) {
    }

    @Override
    public void endContact(Contact contact) {
    }

    private Arm getArm(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();

        if(dataA instanceof Arm) {
            return (Arm) dataA;
        }

        if(dataB instanceof Arm) {
            return (Arm) dataB;
        }

        return null;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
