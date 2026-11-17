package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.input.GameInputProcessor;
import com.TfPSR.ToTheTop.network.ThreadClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Modal shown when the authoritative server declares the match won.
 * It owns its own stage and keeps the final game world visible behind the
 * overlay, just like the pause menu.
 */
public class VictoryScreen {
    private final Stage stage;
    private final AssetService assetService;
    private final ThreadClient network;
    private final Main game;
    private final GameInputProcessor inputProcessor;
    private boolean visible;

    public VictoryScreen(AssetService assetService, Main game, ThreadClient network,
                         GameInputProcessor inputProcessor) {
        this.assetService = assetService;
        this.game = game;
        this.network = network;
        this.inputProcessor = inputProcessor;
        this.stage = createStage();
    }

    /** Builds the overlay, the title and the button that returns to the menu. */
    private Stage createStage() {
        Skin skin = assetService.getSkin();
        Stage stage = new Stage(new ScreenViewport());

        Image overlay = new Image(skin.getDrawable("white"));
        overlay.setColor(0f, 0f, 0f, 0.7f);
        overlay.setFillParent(true);
        stage.addActor(overlay);

        Label.LabelStyle baseStyle = skin.get("defaultLabelStyle", Label.LabelStyle.class);
        Label.LabelStyle titleStyle = new Label.LabelStyle(baseStyle.font, Color.WHITE);
        Label title = new Label("Victory!", titleStyle);
        title.setFontScale(2f);

        TextButton menuButton = new TextButton("Back to menu", skin);
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                network.disconnect();
                Gdx.input.setCursorCatched(false);
                game.setScreen(MainMenu.class);
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.add(title).padBottom(32);
        table.row();
        table.add(menuButton).width(280).height(70);
        stage.addActor(table);
        stage.getRoot().setVisible(false);
        return stage;
    }

    public boolean isVisible() {
        return visible;
    }

    /** Shows the modal once and gives its button exclusive input. */
    public void show() {
        if (visible)
            return;

        visible = true;
        inputProcessor.resetMouseButtons();
        Gdx.input.setCursorCatched(false);
        stage.getRoot().setVisible(true);
        Gdx.input.setInputProcessor(stage);
    }

    public void act(float delta) {
        stage.act(delta);
    }

    public void draw() {
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /** Frees the stage of this modal; its textures are owned by the assets. */
    public void dispose() {
        stage.dispose();
    }
}
