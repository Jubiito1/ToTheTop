package com.TfPSR.ToTheTop.menu;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.menu.utilities.Button;
import com.TfPSR.ToTheTop.menu.utilities.FontManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


public class MainMenu extends ScreenAdapter {
    private final Main game;
    private final Batch batch;
    private final Viewport viewport;
    private final ShapeRenderer shapeRenderer;
    private BitmapFont font;
    Button buttonStart, buttonConfig, buttonExit;

    public MainMenu(Main game) {
        this.game = game;
        this.batch = game.getBatch();
        this.viewport = new ScreenViewport();
        this.shapeRenderer = new ShapeRenderer();
        FontManager.loadFonts();
        this.font = FontManager.getMainFont();

        generateButtons();
    }

    private void generateButtons() {
        int buttonWidth = 200;
        int buttonHeight = 50;
        int centerX =  Gdx.graphics.getWidth() / 2;
        int startY = Gdx.graphics.getHeight() / 2 + 100;
        int spacing = 70;

        this.buttonStart = new Button(centerX, 100, buttonWidth, buttonHeight, "Start", font, initGame());
        this.buttonConfig = new Button(centerX, 200, buttonWidth, buttonHeight, "Configuration", font, openConfig());
        this.buttonExit = new Button(centerX, 300, buttonWidth, buttonHeight, "Exit", font, closeGame());
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float deltaTime) {
        ScreenUtils.clear(Color.WHITE);

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        buttonStart.update(mouseX, mouseY);
        buttonConfig.update(mouseX, mouseY);
        buttonExit.update(mouseX, mouseY);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();
        font.setColor(Color.BLACK);
        font.draw(batch, "TO THE TOP", Gdx.graphics.getWidth() / 2 - 100, Gdx.graphics.getHeight() - 100);
        batch.end();

        buttonStart.draw(batch, shapeRenderer);
        buttonConfig.draw(batch, shapeRenderer);
        buttonExit.draw(batch, shapeRenderer);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        FontManager.dispose();
    }

    private Runnable closeGame() {

        return null;
    }

    private Runnable openConfig() {
        return null;
    }

    private Runnable initGame() {
        return null;
    }
}
