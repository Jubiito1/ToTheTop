package com.TfPSR.ToTheTop.entity;

import com.TfPSR.ToTheTop.physics.JointFactory;
import com.TfPSR.ToTheTop.physics.ShapeFactory;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;

public class Arm {
    private static final float ARM_HEIGHT_RATIO = 0.47f;
    private static final float FOREARM_HEIGHT_RATIO = 0.4f;
    private static final float HAND_HEIGHT_RATIO = 0.13f;

    private static final float ARM_MASS_RATIO = 0.03f;
    private static final float FOREARM_MASS_RATIO = 0.02f;
    private static final float HAND_MASS_RATIO = 0.01f;

    private static final float MOUSE_SENSITIVITY = 1f;
    private static final float VELOCITY_GAIN = 50f;
    private static final float MAX_CONTROL_FORCE = 100f;

    private final Body hand;
    private final Body forearm;
    private final Body upperArm;
    private final Vector2 shoulderAnchor;

    private final Vector2 targetVelocity = new Vector2();
    private final Vector2 velocityError = new Vector2();
    private final Vector2 controlForce = new Vector2();

    public Arm(Vector2 position, Vector2 size, float angle, BodyDef.BodyType type, World world, float weight, float friction, float restitution, short groupIndex, Sides side) {

        Vector2 armSize = new Vector2(size.x, size.y * ARM_HEIGHT_RATIO);
        Vector2 forearmSize = new Vector2(size.x, size.y * FOREARM_HEIGHT_RATIO);
        Vector2 handSize = new Vector2(size.x, size.y * HAND_HEIGHT_RATIO);

        Vector2 armPosition = new Vector2(position.x, position.y + (size.y / 2) - (armSize.y / 2));
        Vector2 forearmPosition = new Vector2(position.x, position.y - (size.y / 2) + handSize.y + (forearmSize.y / 2));
        Vector2 handPosition = new Vector2(position.x, position.y - (size.y / 2) + (handSize.y / 2));

        float armMass = weight * ARM_MASS_RATIO;
        float forearmMass = weight * FOREARM_MASS_RATIO;
        float handMass = weight * HAND_MASS_RATIO;

        float armArea = armSize.x * armSize.y;
        float forearmArea = forearmSize.x * forearmSize.y;
        float handArea = handSize.x * handSize.y;

        float armDensity = armMass / armArea;
        float forearmDensity = forearmMass / forearmArea;
        float handDensity = handMass / handArea;

        upperArm = ShapeFactory.createRectangle(armPosition, armSize, angle, type, world, armDensity, friction, restitution, false, groupIndex);
        forearm = ShapeFactory.createRectangle(forearmPosition, forearmSize, angle, type, world, forearmDensity, friction, restitution, false, groupIndex);
        hand = ShapeFactory.createRectangle(handPosition, handSize, angle, type, world, handDensity, friction, restitution, false, groupIndex);

        upperArm.setUserData(null);
        forearm.setUserData(null);
        hand.setUserData(this);

        Vector2 armElbowAnchor = new Vector2(0, - (armSize.y / 2));
        Vector2 forearmElbowAnchor = new Vector2(0, (forearmSize.y / 2));
        Vector2 forearmWristAnchor = new Vector2(0, - (forearmSize.y / 2));
        Vector2 handWristAnchor = new Vector2(0, (handSize.y / 2));

        if (side == Sides.LEFT) {
            this.shoulderAnchor = new Vector2((armSize.x / 2), (armSize.y / 2) - (size.x / 2));
            RevoluteJoint elbowJoint = JointFactory.createRevoluteJoint(upperArm, forearm, false, armElbowAnchor, forearmElbowAnchor, world, -160, 0);
            RevoluteJoint wristAnchor = JointFactory.createRevoluteJoint(forearm, hand, false, forearmWristAnchor, handWristAnchor, world, -45, 45);
        } else {
            this.shoulderAnchor = new Vector2(-(armSize.x / 2), (armSize.y / 2) - (size.x / 2));
            RevoluteJoint elbowJoint = JointFactory.createRevoluteJoint(upperArm, forearm, false, armElbowAnchor, forearmElbowAnchor, world, 0, 160);
            RevoluteJoint wristAnchor = JointFactory.createRevoluteJoint(forearm, hand, false, forearmWristAnchor, handWristAnchor, world, -45, 45);
        }


    }

    public Body getUpperArm() {
        return upperArm;
    }

    public Vector2 getShoulderAnchor() {
        return shoulderAnchor;
    }

    public void update(Vector2 mouseVelocity) {
        targetVelocity.set(mouseVelocity).scl(MOUSE_SENSITIVITY);

        velocityError.set(targetVelocity).sub(hand.getLinearVelocity());

        controlForce.set(velocityError).scl(VELOCITY_GAIN);

        if (controlForce.len() > MAX_CONTROL_FORCE) {
            controlForce.setLength(MAX_CONTROL_FORCE);
        }

        hand.applyForceToCenter(controlForce, true);
    }
}
