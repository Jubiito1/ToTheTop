// menus/WaitingMenu.java
package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.StartFlag;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.MusicAsset;
import com.TfPSR.ToTheTop.asset.TextureAsset;
import com.TfPSR.ToTheTop.core.GameScreen;
import com.TfPSR.ToTheTop.network.NetworkHeaders;
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
    private final Label roomLabel;
    private final TextButton startButton;
    private final Cell<TextButton> startCell;
    private float time;
    private ThreadClient threadClient;
    private boolean ready = false;

    /** True once the lobby hands its connection to the match screen. */
    private boolean handedOff = false;

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

        // when coming back from a previous match, the flag must start as false
        StartFlag.startFlag = false;

        this.threadClient = new ThreadClient();
        this.threadClient.start();

        Skin skin = assetService.getSkin();
        Label.LabelStyle labelStyle = skin.get("defaultLabelStyle", Label.LabelStyle.class);
        Label.LabelStyle titleStyle = new Label.LabelStyle(labelStyle.font, new Color(1f, 0.78f, 0.38f, 1f));

        Label title = new Label("MULTIPLAYER ROOM", titleStyle);
        title.setFontScale(2f);
        this.statusLabel = new Label("Searching for server", labelStyle);
        this.roomLabel = new Label("Room: ----", labelStyle);

        TextButton readyButton = new TextButton("READY", skin);
        readyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (threadClient.getStatus() == ThreadClient.Status.REJECTED)
                    return; // could not join the room, marking ready makes no sense
                ready = !ready;
                threadClient.setReady(ready);
                readyButton.setText(ready ? "READY (OK)" : "READY");
            }
        });

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                threadClient.disconnect();
                game.setScreen(MainMenu.class);
            }
        });

        // The host is the only one who can start the match.
        this.startButton = new TextButton("START MATCH", skin);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                threadClient.requestStart();
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.top();
        table.add(title).padTop(180);
        table.row();
        table.add(statusLabel).padTop(25);
        table.row();
        table.add(roomLabel).padTop(12);
        table.row();
        table.add(readyButton).width(220).height(55).padTop(30);
        table.row();
        this.startCell = table.add(startButton).width(220).height(55).padTop(15);
        table.row();
        table.add(cancelButton).width(220).height(55).padTop(15);
        stage.addActor(table);
    }

    /** Status text of the room, following what the server answers. */
    private String statusText(String dots) {
        return switch (threadClient.getStatus()) {
            case REJECTED -> rejectText() + dots;
            case ACCEPTED, STARTED -> "Connected  |  " + threadClient.getPlayers() + "/"
                    + threadClient.getMaxPlayers() + " players  |  "
                    + threadClient.getReadyCount() + " ready (min. 2)" + dots;
            default -> "Searching for server" + dots;
        };
    }

    /** Reason of the rejection, so the player knows what to fix. */
    private String rejectText() {
        return switch (threadClient.getRejectReason()) {
            case NetworkHeaders.REJECT_ROOM -> "Wrong room code";
            case NetworkHeaders.REJECT_VERSION -> "Incompatible game version";
            case NetworkHeaders.REJECT_FULL -> "Room is full";
            case NetworkHeaders.REJECT_STARTED -> "The match already started";
            default -> "Could not join the room";
        };
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
        // the ThreadClient writes the flag, this runs on the render thread
        if (StartFlag.startFlag) {
            handedOff = true;
            game.addScreen(new GameScreen(game, assetService, settingsMenu, threadClient));
            game.setScreen(GameScreen.class);
            return;
        }

        if (threadClient.isConnectionLost()) {
            // the server stopped answering: there is no room to go back to
            threadClient.disconnect();
            game.setScreen(MainMenu.class);
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        time += delta;
        statusLabel.setText(statusText(DOTS[(int) (time / 0.4f) % DOTS.length]));

        String code = threadClient.getRoomCode();
        roomLabel.setText("Room: " + (code.isEmpty() ? "----" : code));

        // The button only exists for the host, once joined and with two ready.
        boolean canStart = threadClient.isHost()
                && threadClient.getStatus() == ThreadClient.Status.ACCEPTED
                && threadClient.getReadyCount() >= 2;
        startCell.setActor(canStart ? startButton : null);

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

    @Override
    public void dispose() {
        // The connection belongs to the match screen once the match starts.
        if (!handedOff)
            threadClient.disconnect();
        stage.dispose();
    }
}
