package com.TfPSR.ToTheTop.core;

import com.TfPSR.ToTheTop.*;
import com.TfPSR.ToTheTop.entity.Character;
import com.TfPSR.ToTheTop.input.GameInputProcessor;
import com.TfPSR.ToTheTop.map.GameMap;
import com.TfPSR.ToTheTop.map.Rocks;
import com.TfPSR.ToTheTop.physics.GameContactListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private final Vector2 GRAVEDAD = new Vector2(0, -9.8f);

    private final Main game;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final Batch batch;

    private final World world;
    private final Box2DDebugRenderer debugRenderer;
    private final Character player;

    private final GameMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;

    private final Rocks rocks;

    private final GameInputProcessor inputProcessor;

    public GameScreen(Main game) {
        this.game = game;
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.debugRenderer = new Box2DDebugRenderer();
        map = new GameMap("maps/map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map.getTiledMap(), 1f / Constants.PIXELS_PER_METER);

        world = new World(GRAVEDAD, true);
        world.setContactListener(new GameContactListener());

        rocks = new Rocks(map.getSurfaceObjects(), world);

        player = new Character( map.getPlayerSpawn(), new Vector2(0.6f, 1.80f), 80f, world);

        inputProcessor = new GameInputProcessor();
        Gdx.input.setInputProcessor(inputProcessor);

        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        update(delta);
        draw();
    }

    private void draw() {
        mapRenderer.setView(camera);
        mapRenderer.render();
        batch.begin();
        player.draw(batch);

        batch.setProjectionMatrix(camera.combined);
        debugRenderer.render(world, camera.combined);
        batch.end();
    }

    private void update(float delta) {
        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.zoom = 1.5f;
        camera.update();

        Vector2 mouseVelocity = getMouseVelocity(delta);
        player.update(mouseVelocity, inputProcessor.isLeftMousePressed(), inputProcessor.isRightMousePressed());

        world.step(delta, 10, 4);
    }

    private Vector2 getMouseVelocity(float delta) {
        Vector2 mouseDelta = inputProcessor.getMouseDelta();

        float worldWidth = viewport.getWorldWidth() * camera.zoom;
        float worldHeight = viewport.getWorldHeight() * camera.zoom;

        float velocityX = (mouseDelta.x / viewport.getScreenWidth()) * worldWidth / delta;
        float velocityY = -(mouseDelta.y / viewport.getScreenHeight()) * worldHeight / delta;

        return new Vector2(velocityX, velocityY);
    }
}
