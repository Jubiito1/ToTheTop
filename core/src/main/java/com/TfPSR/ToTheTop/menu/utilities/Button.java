package com.TfPSR.ToTheTop.menu.utilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;


public class Button {
    private Rectangle bounds;
    private String label;
    private BitmapFont font;
    private Runnable action;
    private boolean isHovered= false;
    private GlyphLayout glyphLayout;

    private Color normalColor = new Color(0.2f, 0.2f, 0.2f, 1f); // Gray dark
    private Color hoverColor = new Color(0.4f, 0.4f, 0.4f, 1f);  // Gray light
    private Color currentColor;

    public Button(int x, int y, int width, int height, String label, BitmapFont font, Runnable action){
        this.bounds = new Rectangle(x, y, width, height);
        this.label = label;
        this.font = font;
        this.action = action;
        this.currentColor = normalColor;
        this.glyphLayout = new GlyphLayout();
    }

    public void update(float mouseX, float mouseY){
        isHovered = bounds.contains(mouseX, mouseY);
        currentColor = isHovered ? hoverColor : normalColor;
    }

    public void draw(Batch batch, ShapeRenderer shapeRenderer){
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(currentColor);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();

        batch.begin();
        glyphLayout.setText(font, label);
        float textX = bounds.x + (bounds.width - glyphLayout.width) / 2;
        float textY = bounds.y + (bounds.height + glyphLayout.height) / 2;

        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, textX, textY);
        batch.end();

    }

    public boolean isClicked(float mouseX, float mouseY){
        return bounds.contains(mouseX, mouseY);
    }

    public void perfomAction(){
        if(action !=null){
            action.run();
        }
    }
}
