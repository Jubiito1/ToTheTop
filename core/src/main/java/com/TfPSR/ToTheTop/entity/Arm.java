package com.TfPSR.ToTheTop.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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

    /**
     * Colour of a hand that is holding a rock. A hand that grabbed and a hand
     * that only hit the rock look the same otherwise, so the player could not
     * tell whether the release worked.
     */
    private static final Color GRIP_TINT = new Color(1f, 0.92f, 0.35f, 1f);

    /** Colour of the slot, kept to restore the hand when it lets go. */
    private final Color tint = new Color(Color.WHITE);

    /** True while the server reports this hand holding a rock. */
    private boolean gripping;



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

    /**
     * Places one segment of the arm. The state packet sends the three bodies in
     * order: 0 upper arm, 1 forearm, 2 hand.
     */
    public void applySegment(int segment, Vector2 pos, float angleDeg) {
        Sprite sprite = switch (segment) {
            case 0 -> spriteUpperArm;
            case 1 -> spriteForearm;
            case 2 -> spriteHand;
            default -> null;
        };
        if (sprite == null)
            return;

        sprite.setPosition(pos.x - sprite.getWidth() / 2f, pos.y - sprite.getHeight() / 2f);
        sprite.setRotation(angleDeg);
    }

    /** Called with the grip mask of every state packet. */
    public void setGripping(boolean gripping) {
        this.gripping = gripping;
    }

    public void draw(Batch batch) {
        spriteHand.setColor(gripping ? GRIP_TINT : tint);

        spriteUpperArm.draw(batch);
        spriteForearm.draw(batch);
        spriteHand.draw(batch);
    }

    /** Tints the three segments with the colour of its slot. */
    public void setTint(Color color) {
        tint.set(color);
        spriteUpperArm.setColor(color);
        spriteForearm.setColor(color);
        spriteHand.setColor(color);
    }

    /** Frees the textures of the three segments. */
    public void dispose() {
        spriteUpperArm.getTexture().dispose();
        spriteForearm.getTexture().dispose();
        spriteHand.getTexture().dispose();
    }

}

