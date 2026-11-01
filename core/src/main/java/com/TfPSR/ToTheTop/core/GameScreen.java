package com.TfPSR.ToTheTop.core;

import com.TfPSR.ToTheTop.*;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.TfPSR.ToTheTop.entity.Character;
import com.TfPSR.ToTheTop.input.GameInputProcessor;
import com.TfPSR.ToTheTop.map.GameMap;
import com.TfPSR.ToTheTop.menus.SettingsMenu;
import com.TfPSR.ToTheTop.menus.PauseMenu;
import com.TfPSR.ToTheTop.network.ThreadClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private static final float INPUT_INTERVAL = 1f / 60f;   // ritmo fijo de simulacion del server
    private static final int MAX_INPUTS_PER_FRAME = 5;      // evita una avalancha tras un freeze

    private final Vector2 GRAVEDAD = new Vector2(0, -9.8f);

    private final Main game;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final Batch batch;

    private final Character player;

    private final GameMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;

    private final AssetService assetService;
    private final Sound dash;
    private final InputMultiplexer inputMultiplexer;

    private final ThreadClient network;
    private final Vector2 pointer = new Vector2();       // cursor virtual, en pixeles de pantalla
    private final Vector2 worldPointer = new Vector2();  // mismo punto, en metros de mundo
    private float inputAccumulator;


    private final GameInputProcessor inputProcessor;
    private boolean paused = false;
    private final SettingsMenu settingsMenu;
    private final PauseMenu pauseMenu;

    public GameScreen(Main game, AssetService assetService, SettingsMenu settingsMenu,
                      ThreadClient network) {
        this.game = game;
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.assetService = assetService;
        this.dash = assetService.get(SoundAsset.DASH);
        this.inputMultiplexer = new InputMultiplexer();
        this.settingsMenu = settingsMenu;
        this.pauseMenu = new PauseMenu(assetService, game, this, settingsMenu);
        this.network = network;

        map = new GameMap("maps/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map.getTiledMap(), 1f / Constants.PIXELS_PER_METER);


        player = new Character( map.getPlayerSpawn(), new Vector2(0.6f, 1.80f));

        // el cursor esta capturado: el puntero real no es una posicion valida, asi
        // que se reconstruye acumulando deltas y se arranca desde el centro
        pointer.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);

        inputProcessor = new GameInputProcessor(this);
        inputMultiplexer.addProcessor(pauseMenu.getStage());
        inputMultiplexer.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(inputMultiplexer);

        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta) {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            if(!paused)
                update(delta);
            draw();
    }

    public InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }

    private void draw() {
        mapRenderer.setView(camera);
        mapRenderer.render();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);

        batch.end();

        if (paused) {
            pauseMenu.getStage().act(Gdx.graphics.getDeltaTime());
            pauseMenu.getStage().draw();

            settingsMenu.drawSettingsMenu(Gdx.graphics.getDeltaTime());
        }
    }

    private void update(float delta) {
        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.zoom = 1.5f;
        camera.update();

        sendInput(delta);
    }

    /**
     * Manda el input a ritmo fijo (60 Hz) y no una vez por frame, para que el
     * server reciba un flujo estable aunque el render vaya a otra tasa.
     */
    private void sendInput(float delta) {
        updatePointer();
        inputAccumulator += delta;

        int sent = 0;
        while (inputAccumulator >= INPUT_INTERVAL && sent < MAX_INPUTS_PER_FRAME) {
            inputAccumulator -= INPUT_INTERVAL;
            sent++;
            network.sendInput(
                    worldPointer.x, worldPointer.y,
                    inputProcessor.isLeftMousePressed(),
                    inputProcessor.isRightMousePressed(),
                    inputProcessor.isAPressed(),
                    inputProcessor.isDPressed());
        }

        // si el frame se fue muy largo, se descarta el atraso en vez de acumularlo
        if (inputAccumulator > INPUT_INTERVAL * MAX_INPUTS_PER_FRAME)
            inputAccumulator = 0f;
    }

    /**
     * Convierte el cursor capturado en una posicion absoluta de mundo. Acumula
     * los deltas porque GLFW no reporta un cursor real cuando esta capturado, y
     * lo limita a la pantalla para que el puntero no se escape del viewport.
     */
    private void updatePointer() {
        Vector2 delta = inputProcessor.getMouseDelta();
        pointer.x = MathUtils.clamp(pointer.x + delta.x, 0f, Gdx.graphics.getWidth());
        pointer.y = MathUtils.clamp(pointer.y + delta.y, 0f, Gdx.graphics.getHeight());

        worldPointer.set(pointer);
        viewport.unproject(worldPointer);
    }


    public boolean isPaused(){
        return paused;
    }

    public void pauseGame(){
        if(paused)
            return;

        inputProcessor.resetMouseButtons();
        paused = true;
        pauseMenu.open();
    }

    public void resumeGame(){
        if(!paused)
            return;

        paused = false;
        pauseMenu.close();
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width, height);

        pauseMenu.resize(width, height);
        settingsMenu.resize(width, height);
    }
}
