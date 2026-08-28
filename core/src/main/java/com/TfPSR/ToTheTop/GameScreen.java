package com.TfPSR.ToTheTop;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.SoundAsset;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
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
    private final Body groundBody;
    private final Character player;
    private final AssetService assetService;
    private final Sound dash;

    public  GameScreen(Main game, AssetService assetService) {
        this.game = game;
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.assetService = assetService;
        this.debugRenderer = new Box2DDebugRenderer();
        this.dash = assetService.get(SoundAsset.DASH);

        world = new World(GRAVEDAD, true);
        world.setContactListener(new GameContactListener());

        player = new Character( new Vector2(2f, 4f), new Vector2(0.6f, 1.80f), 80f, world, dash);

        Gdx.input.setInputProcessor(
            new GameInputProcessor(player)
        );

        groundBody = ShapeFactory.createRectangle(new Vector2(0f, 0f), new Vector2(10f, 1f), 0, BodyDef.BodyType.StaticBody, world, 0.4f, 1f, 0, false, (short) 0);

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
        batch.setProjectionMatrix(camera.combined);
        debugRenderer.render(world, camera.combined);
    }

    private void update(float delta) {
        camera.position.set(0, 2, 0);
        camera.zoom = 1f;
        camera.update();

        player.update(findMousePosition(), world);

        world.step(delta, 10, 4);
    }
}
