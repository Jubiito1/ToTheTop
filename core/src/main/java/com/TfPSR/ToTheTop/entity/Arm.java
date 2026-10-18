package com.TfPSR.ToTheTop.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Arm {
    private Sprite spriteUpperArm;
    private Sprite spriteForearm;
    private Sprite spriteHand;

    private static final float ARM_HEIGHT_RATIO = 0.47f;
    private static final float FOREARM_HEIGHT_RATIO = 0.4f;
    private static final float HAND_HEIGHT_RATIO = 0.13f;



    public Arm(Vector2 position, Vector2 size) {

        Vector2 armSize = new Vector2(size.x, size.y * ARM_HEIGHT_RATIO);
        Vector2 forearmSize = new Vector2(size.x, size.y * FOREARM_HEIGHT_RATIO);
        Vector2 handSize = new Vector2(size.x, size.y * HAND_HEIGHT_RATIO);

        Vector2 armPosition = new Vector2(position.x, position.y + (size.y / 2) - (armSize.y / 2));
        Vector2 forearmPosition = new Vector2(position.x, position.y - (size.y / 2) + handSize.y + (forearmSize.y / 2));
        Vector2 handPosition = new Vector2(position.x, position.y - (size.y / 2) + (handSize.y / 2));

        this.spriteUpperArm = createBodyPartSprite(armSize.x, armSize.y, "sprites/arm.png");
        this.spriteForearm = createBodyPartSprite(forearmSize.x, forearmSize.y, "sprites/forearm.png");
        this.spriteHand = createBodyPartSprite(handSize.x, handSize.y, "sprites/hand.png");

    }

    private Sprite createBodyPartSprite(float width, float height, String spritePath) {
        Texture texture = new Texture(Gdx.files.internal(spritePath));
        Sprite sprite = new Sprite(texture);
        sprite.setSize(width, height);
        sprite.setOrigin(width/2f, height/2f);
        return sprite;
    }

    public void applyState(Vector2 upperPos, float upperAngle, Vector2 forePos, float foreAngle, Vector2 handPos, float handAngle) {
        applySegment(spriteUpperArm, upperPos, upperAngle);
        applySegment(spriteForearm, forePos, foreAngle);
        applySegment(spriteHand, handPos, handAngle);
    }

    private void applySegment(Sprite sprite, Vector2 pos, float angleDeg) {
        sprite.setPosition(pos.x - sprite.getWidth() / 2f, pos.y - sprite.getHeight() / 2f);
        sprite.setRotation(angleDeg);
    }

    public void draw(Batch batch) {

        spriteUpperArm.draw(batch);
        spriteForearm.draw(batch);
        spriteHand.draw(batch);
    }

}

