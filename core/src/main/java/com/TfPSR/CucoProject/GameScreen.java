package com.TfPSR.CucoProject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
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

        Gdx.input.setInputProcessor(
            new GameInputProcessor(player)
        );

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        Cursor invisibleCursor = Gdx.graphics.newCursor(pixmap, 0, 0);

        Gdx.graphics.setCursor(invisibleCursor);

        pixmap.dispose();
    }

    public Vector2 findMousePosition() {
        Vector2 mousePosition = new Vector2(Gdx.input.getX(), Gdx.input.getY());

        viewport.unproject(mousePosition);

        return mousePosition;
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

        batch.setProjectionMatrix(camera.combined);
        debugRenderer.render(world, camera.combined);
    }

    private void update(float delta) {
        player.update(findMousePosition(), world);
        world.step(delta, 10, 4);
        camera.position.set(20, 10, 0);
        camera.zoom = 2f;
        camera.update();

    }
}
