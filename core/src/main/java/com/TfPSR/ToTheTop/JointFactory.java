package com.TfPSR.CucoProject;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;

public class JointFactory {
    public JointFactory() {}

    public static RevoluteJoint createRevoluteJoint(Body bodyA, Body bodyB, boolean collideConnected, Vector2 anchorA, Vector2 anchorB, World world) {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.bodyA = bodyA;
        jointDef.bodyB = bodyB;
        jointDef.collideConnected = collideConnected;
        jointDef.localAnchorA.set(anchorA);
        jointDef.localAnchorB.set(anchorB);

        return (RevoluteJoint) world.createJoint(jointDef);
    }
    public static RevoluteJoint createRevoluteJoint(Body bodyA, Body bodyB, boolean collideConnected, Vector2 anchorA, Vector2 anchorB, World world, float lowerAngle, float upperAngle) {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.bodyA = bodyA;
        jointDef.bodyB = bodyB;
        jointDef.collideConnected = collideConnected;
        jointDef.localAnchorA.set(anchorA);
        jointDef.localAnchorB.set(anchorB);
        jointDef.enableLimit = true;
        jointDef.lowerAngle = (float) Math.toRadians(lowerAngle);
        jointDef.upperAngle = (float) Math.toRadians(upperAngle);

        return (RevoluteJoint) world.createJoint(jointDef);
    }

}
