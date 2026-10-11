// menus/WaitingScreen.java
package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.StartFlag;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.MusicAsset;
import com.TfPSR.ToTheTop.asset.TextureAsset;
import com.TfPSR.ToTheTop.core.GameScreen;
import com.TfPSR.ToTheTop.network.ThreadClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.IOException;

public class WaitingMenu extends ScreenAdapter {
    private static final String[] DOTS = {".", "..", "...", "...."};

    private final Main game;
    private final AssetService assetService;
    private final SettingsMenu settingsMenu;
    private final Texture backgroundImage;
    private final Music music;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final Batch batch;
    private final Stage stage;
    private final Label statusLabel;
    private float time;
    private ThreadClient threadClient;

    public WaitingMenu(Main game, AssetService assetService, SettingsMenu settingsMenu) throws IOException {
        this.game = game;
        this.assetService = assetService;
        this.settingsMenu = settingsMenu;
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.backgroundImage = assetService.get(TextureAsset.BACKGROUND_IMAGE);
        this.music = assetService.get(MusicAsset.MAIN_MENU_MUSIC);
        this.stage = new Stage(new ScreenViewport());
        this.threadClient = new ThreadClient();
        this.threadClient.start();

        Skin skin = assetService.getSkin();
        Label.LabelStyle labelStyle = skin.get("defaultLabelStyle", Label.LabelStyle.class);
        Label.LabelStyle titleStyle = new Label.LabelStyle(labelStyle.font, new Color(1f, 0.78f, 0.38f, 1f));

        Label title = new Label("BUSCANDO RIVAL", titleStyle);
        title.setFontScale(2f);
        this.statusLabel = new Label("Conectando con el servidor", labelStyle);

        TextButton cancelButton = new TextButton("Cancelar", skin);
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                threadClient.interrupt();
                game.setScreen(MainMenu.class);
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.top();
        table.add(title).padTop(180);
        table.row();
        table.add(statusLabel).padTop(25);
        table.row();
        table.add(cancelButton).width(220).height(55).padTop(30);
        stage.addActor(table);
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(stage);
        assetService.getAudioManager().playMusic(music);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        camera.zoom = 1f;
        camera.update();
    }

    @Override
    public void render(float delta) {
        // la flag la escribe el ThreadClient, esto corre en el render thread
        if (StartFlag.startFlag) {
            game.addScreen(new GameScreen(game, assetService, settingsMenu));
            game.setScreen(GameScreen.class);
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        time += delta;
        statusLabel.setText("Esperando jugador" + DOTS[(int) (time / 0.4f) % DOTS.length]);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(backgroundImage, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        music.stop();
    }
}
