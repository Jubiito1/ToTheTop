package com.TfPSR.ToTheTop.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Character {

    private Sprite spriteHead;
    private Sprite spriteTorso;
    private Sprite spriteLeftLeg;
    private Sprite spriteRightLeg;

    private static final float HEAD_WIDTH_RATIO = 0.28f;
    private static final float HEAD_HEIGHT_RATIO = 0.13f;

    private static final float CHEST_WIDTH_RATIO = 0.62f;
    private static final float CHEST_HEIGHT_RATIO = 0.37f;

    private static final float ARMS_WIDTH_RATIO = 0.18f;
    private static final float ARMS_HEIGHT_RATIO = 0.45f;

    private static final float LEGS_WIDTH_RATIO = 0.25f;
    private static final float LEGS_HEIGHT_RATIO = 0.5f;


    Vector2 position;

    private final Arm leftArm;
    private final Arm rightArm;
    public Character(Vector2 position, Vector2 size) {
        this.position = position;
        Vector2 headSize = new Vector2(size.x * HEAD_WIDTH_RATIO, size.y * HEAD_HEIGHT_RATIO);
        Vector2 torsoSize = new Vector2(size.x * CHEST_WIDTH_RATIO, size.y * CHEST_HEIGHT_RATIO);
        Vector2 armsSize = new Vector2(size.x * ARMS_WIDTH_RATIO, size.y * ARMS_HEIGHT_RATIO);
        Vector2 legsSize = new Vector2(size.x * LEGS_WIDTH_RATIO, size.y * LEGS_HEIGHT_RATIO);

        // Posición de cada brazo, a los costados del torso (misma fórmula de tu versión original)
        Vector2 leftArmPosition  = new Vector2(position.x - (torsoSize.x / 2) - (armsSize.x / 2),position.y + (size.y / 2) - headSize.y - (armsSize.y / 2));
        Vector2 rightArmPosition = new Vector2(position.x + (torsoSize.x / 2) + (armsSize.x / 2),position.y + (size.y / 2) - headSize.y - (armsSize.y / 2));

        this.spriteHead = createBodyPartSprite(headSize.x, headSize.y, "sprites/head.png");
        this.spriteTorso= createBodyPartSprite(torsoSize.x, torsoSize.y, "sprites/torso.png");
        this.spriteLeftLeg= createBodyPartSprite(legsSize.x, legsSize.y, "sprites/leg.png");
        this.spriteRightLeg= createBodyPartSprite(legsSize.x, legsSize.y, "sprites/leg.png");

        this.leftArm  = new Arm(leftArmPosition, armsSize);
        this.rightArm = new Arm(rightArmPosition, armsSize);
    }


    private Sprite createBodyPartSprite(float width, float height, String spritePath) {
        Texture texture = new Texture(Gdx.files.internal(spritePath));
        Sprite sprite = new Sprite(texture);
        sprite.setSize(width, height);
        sprite.setOrigin(width/2f, height/2f);

        return sprite;
    }

    public void applyPartState(int partIndex, Vector2 pos, float angleDeg) {
        switch (partIndex) {
            case 0 -> {
                applySegment(spriteHead, pos, angleDeg);
                position.set(pos);          // the head is the camera anchor
            }
            case 1 -> applySegment(spriteTorso, pos, angleDeg);
            case 2, 3, 4 -> leftArm.applySegment(partIndex - 2, pos, angleDeg);
            case 5, 6, 7 -> rightArm.applySegment(partIndex - 5, pos, angleDeg);
            case 8 -> applySegment(spriteLeftLeg, pos, angleDeg);
            case 9 -> applySegment(spriteRightLeg, pos, angleDeg);
            default -> {
                // Unknown part: ignore it instead of breaking the frame.
            }
        }
    }

    private void applySegment(Sprite sprite, Vector2 pos, float angleDeg) {
        sprite.setPosition(pos.x - sprite.getWidth() / 2f, pos.y - sprite.getHeight() / 2f);
        sprite.setRotation(angleDeg);
    }

    public void draw(Batch batch) {

        leftArm.draw(batch);
        rightArm.draw(batch);
        spriteHead.draw(batch);
        spriteLeftLeg.draw(batch);
        spriteRightLeg.draw(batch);
        spriteTorso.draw(batch);
    }

    /** Frees the textures of every part; the character is gone from the match. */
    public void dispose() {
        spriteHead.getTexture().dispose();
        spriteTorso.getTexture().dispose();
        spriteLeftLeg.getTexture().dispose();
        spriteRightLeg.getTexture().dispose();
        leftArm.dispose();
        rightArm.dispose();
    }

    /** Tints every part with the colour the roster assigned to this slot. */
    public void setTint(Color color) {
        spriteHead.setColor(color);
        spriteTorso.setColor(color);
        spriteLeftLeg.setColor(color);
        spriteRightLeg.setColor(color);
        leftArm.setTint(color);
        rightArm.setTint(color);
    }

    public Vector2 getPosition() {return position;}
}


