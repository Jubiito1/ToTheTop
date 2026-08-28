package com.TfPSR.ToTheTop;

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

    private final Body hand;
    private final Body forearm;
    private final Body upperArm;
    private final Vector2 shoulderAnchor;
    private final Vector2 distance = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Vector2 force = new Vector2();
    private final float proportionalGain = 100f;
    private final float derivativeGain = 40f;
    private static final float MAX_FORCE = 1000f;

    private RevoluteJoint gripJoint;
    private ArmStates state = ArmStates.FREE;

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

    public void movePlayerToMouse(Vector2 mousePosition) {

        distance.set(mousePosition).sub(hand.getPosition());

        velocity.set(hand.getLinearVelocity());

        force.set(distance).scl(proportionalGain).sub(velocity.x * derivativeGain, velocity.y * derivativeGain);

        if(force.len() > MAX_FORCE) {
            force.setLength(MAX_FORCE);
        }

        hand.applyForceToCenter(force, true);
    }

    public void grab(World world) {
        /*
        if(currentGripPoint == null) {
            return;
        }

        if(state == ArmStates.GRABBING) {
            return;
        }

        gripJoint = JointFactory.createRevoluteJoint(hand, currentGripPoint.getBody(), false, new Vector2(0, 0), new Vector2(0, 0), world);

        state = ArmStates.GRABBING;
        */
    }

    public void release(World world) {

        if(state == ArmStates.FREE) {
            return;
        }

        world.destroyJoint(gripJoint);

        gripJoint = null;
        state = ArmStates.FREE;
    }

    public void update(Vector2 mousePosition) {
        movePlayerToMouse(mousePosition);
    }

    public Body getUpperArm() {
        return upperArm;
    }

    public Vector2 getShoulderAnchor() {
        return shoulderAnchor;
    }

    public ArmStates getState() {
        return state;
    }

}
