package com.TfPSR.ToTheTop.core;

import com.TfPSR.ToTheTop.*;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.entity.Character;
import com.TfPSR.ToTheTop.input.GameInputProcessor;
import com.TfPSR.ToTheTop.map.GameMap;
import com.TfPSR.ToTheTop.map.TerrainRenderer;
import com.TfPSR.ToTheTop.menus.MainMenu;
import com.TfPSR.ToTheTop.menus.SettingsMenu;
import com.TfPSR.ToTheTop.menus.PauseMenu;
import com.TfPSR.ToTheTop.menus.MatchEndScreen;
import com.TfPSR.ToTheTop.network.InputPacket;
import com.TfPSR.ToTheTop.network.PartState;
import com.TfPSR.ToTheTop.network.PlayerState;
import com.TfPSR.ToTheTop.network.Roster;
import com.TfPSR.ToTheTop.network.StateInterpolator;
import com.TfPSR.ToTheTop.network.StatePacket;
import com.TfPSR.ToTheTop.network.ThreadClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private static final float INPUT_INTERVAL = 1f / 60f;   // fixed simulation rate of the server
    private static final int MAX_INPUTS_PER_FRAME = 5;      // avoids a burst after a freeze

    /**
     * World meters the virtual pointer moves per raw mouse count. A fixed
     * factor keeps the hand speed identical on every window size, resolution
     * and camera zoom; only the mouse DPI (a user setting) changes it.
     */
    private static final float POINTER_METERS_PER_COUNT = 0.0125f;
    private static final int EVENT_REPEAT_TICKS = 3;        // a lost datagram does not lose the event
    private static final int MAX_PLAYERS = 4;
    private static final Vector2 CHARACTER_SIZE = new Vector2(0.6f, 1.80f);
    private static final float SNAPSHOT_INTERVAL = 1f / 60f;

    /** Height above the head anchor where the roster name is drawn, in meters. */
    private static final float NAME_OFFSET_Y = 0.3f;

    private final Main game;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final Batch batch;

    /** One character per slot, created from the first snapshot that mentions it. */
    private final Character[] players = new Character[MAX_PLAYERS];
    private final Vector2 spawn;

    /** Colour and name label of every slot, rebuilt when the roster changes. */
    private final Color[] slotTints = new Color[MAX_PLAYERS];
    private final GlyphLayout[] slotLabels = new GlyphLayout[MAX_PLAYERS];
    private final Matrix4 labelProjection = new Matrix4();
    private final Vector3 labelPoint = new Vector3();
    private final BitmapFont nameFont;
    private Roster roster;

    private final GameMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final TerrainRenderer terrainRenderer;

    private final AssetService assetService;
    private final InputMultiplexer inputMultiplexer;

    private final ThreadClient network;
    private final Vector2 worldPointer = new Vector2();  // virtual cursor, in world meters
    private float inputAccumulator;
    private int pendingEvents;                           // events to repeat over the next ticks
    private int eventTicksRemaining;

    /** The two newest snapshots and how far rendering is between them. */
    private StatePacket.Snapshot previousSnapshot;
    private StatePacket.Snapshot currentSnapshot;
    private float snapshotAge;


    private final GameInputProcessor inputProcessor;
    private boolean paused = false;
    private final SettingsMenu settingsMenu;
    private final PauseMenu pauseMenu;
    private final MatchEndScreen matchEndScreen;

    public GameScreen(Main game, AssetService assetService, SettingsMenu settingsMenu,
                      ThreadClient network) {
        this.game = game;
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.assetService = assetService;
        this.inputMultiplexer = new InputMultiplexer();
        this.settingsMenu = settingsMenu;
        this.pauseMenu = new PauseMenu(assetService, game, this, settingsMenu);
        this.network = network;

        this.nameFont = assetService.getSkin().getFont("buttonFont");
        for (int slot = 0; slot < MAX_PLAYERS; slot++) {
            slotTints[slot] = new Color(Color.WHITE);
            slotLabels[slot] = new GlyphLayout();
        }

        map = new GameMap(assetService);
        mapRenderer = new OrthogonalTiledMapRenderer(map.getTiledMap(), 1f / Constants.PIXELS_PER_METER);
        terrainRenderer = new TerrainRenderer(map.getSurfaceObjects());

        spawn = map.getPlayerSpawn();

        inputProcessor = new GameInputProcessor(this);
        matchEndScreen = new MatchEndScreen(assetService, game, network, inputProcessor);
        inputMultiplexer.addProcessor(pauseMenu.getStage());
        inputMultiplexer.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(inputMultiplexer);

        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta) {
            if (network.isConnectionLost()) {
                // The server stopped answering: the match cannot continue.
                network.disconnect();
                game.setScreen(MainMenu.class);
                return;
            }

            Gdx.gl.glClearColor(0.04f, 0.05f, 0.08f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            update(delta);
            draw();
    }

    public InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }

    private void draw() {
        // The menus draw with their own stage viewports and leave the GL
        // viewport set to theirs; the world must always use the game one.
        viewport.apply();

        terrainRenderer.render(camera.combined);

        mapRenderer.setView(camera);
        mapRenderer.render();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int slot = 0; slot < players.length; slot++) {
            Character character = players[slot];
            if (character == null)
                continue;
            character.setTint(slotTints[slot]);
            character.draw(batch);
        }

        batch.end();

        drawNames();

        if (matchEndScreen.isVisible()) {
            matchEndScreen.act(Gdx.graphics.getDeltaTime());
            matchEndScreen.draw();
            return;
        }

        if (paused) {
            pauseMenu.getStage().act(Gdx.graphics.getDeltaTime());
            pauseMenu.getStage().draw();

            settingsMenu.drawSettingsMenu(Gdx.graphics.getDeltaTime());
        }
    }

    /**
     * Draws the name of every player above its head, in the colour of its slot.
     * Runs in screen space, so the text keeps a readable size whatever the zoom.
     */
    private void drawNames() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        // Screen space with the origin at the bottom left, the same orientation
        // BitmapFont expects; a y-down projection would draw the text mirrored.
        labelProjection.setToOrtho(0, width, 0, height, 0, 1);
        batch.setProjectionMatrix(labelProjection);
        batch.begin();

        for (int slot = 0; slot < players.length; slot++) {
            Character character = players[slot];
            GlyphLayout label = slotLabels[slot];
            if (character == null || label.width <= 0f)
                continue;

            // The head is the anchor of the character; the name goes above it.
            labelPoint.set(character.getPosition().x, character.getPosition().y + NAME_OFFSET_Y, 0f);
            labelPoint.prj(camera.combined);

            float x = (labelPoint.x + 1f) / 2f * width;
            float y = (labelPoint.y + 1f) / 2f * height;

            nameFont.setColor(slotTints[slot]);
            // BitmapFont draws downwards from the y it is given, so start the
            // text one line above the anchor to leave the head clear.
            nameFont.draw(batch, label, x - label.width / 2f, y + label.height);
        }

        nameFont.setColor(Color.WHITE);
        batch.end();
        batch.setProjectionMatrix(camera.combined);
    }

    /** Rebuilds the colour and the name label of every slot when a roster arrives. */
    private void refreshRoster() {
        Roster current = network.getRoster();
        if (current == roster)
            return;
        roster = current;

        for (int slot = 0; slot < MAX_PLAYERS; slot++) {
            slotTints[slot].set(Color.WHITE);
            slotLabels[slot].setText(nameFont, "");
        }

        for (Roster.Entry entry : current.entries()) {
            int slot = entry.slot();
            if (slot < 0 || slot >= MAX_PLAYERS)
                continue;
            slotTints[slot].set(entry.color());
            slotLabels[slot].setText(nameFont, entry.name());
        }

        // A slot that is no longer in the roster left the match: its body must
        // disappear instead of staying frozen where the last state left it.
        for (int slot = 0; slot < MAX_PLAYERS; slot++) {
            if (players[slot] == null || current.entry(slot) != null)
                continue;
            players[slot].dispose();
            players[slot] = null;
        }
    }

    private void update(float delta) {
        refreshRoster();
        updateSnapshot(delta);
        centerCameraOnPlayers();
        sendInput(delta);
    }

    /**
     * Keeps the two newest snapshots and renders a blend of them. Without this,
     * the sprites would jump to a new pose every received packet; with it, the
     * render thread shows the pose between the last two snapshots the server
     * produced. A slot that appears for the first time is shown as it is.
     */
    private void updateSnapshot(float delta) {
        StatePacket.Snapshot incoming = network.getSnapshot();
        if (incoming != null && incoming != currentSnapshot) {
            previousSnapshot = currentSnapshot;
            currentSnapshot = incoming;
            snapshotAge = 0f;
        }

        if (currentSnapshot == null)
            return;

        if (currentSnapshot.matchState() == StatePacket.MATCH_FINISHED)
            matchEndScreen.show(currentSnapshot.result() == StatePacket.RESULT_WIN);

        snapshotAge += delta;
        float alpha = Math.min(1f, snapshotAge / SNAPSHOT_INTERVAL);

        applySnapshot(previousSnapshot == null
                ? currentSnapshot
                : StateInterpolator.interpolate(previousSnapshot, currentSnapshot, alpha));
    }

    /**
     * Copies a snapshot onto the sprites. The client never simulates: it only
     * places each body where the server says it is. A slot is created lazily on
     * the first snapshot that mentions it.
     */
    private void applySnapshot(StatePacket.Snapshot snapshot) {
        for (PlayerState state : snapshot.players()) {
            int slot = state.slot();
            if (slot < 0 || slot >= players.length || state.parts().length == 0)
                continue;

            Character character = players[slot];
            if (character == null) {
                PartState head = state.parts()[0];
                character = new Character(new Vector2(head.x(), head.y()), new Vector2(CHARACTER_SIZE));
                players[slot] = character;
            }

            for (int i = 0; i < state.parts().length; i++) {
                PartState part = state.parts()[i];
                character.applyPartState(i, new Vector2(part.x(), part.y()), part.angleDegrees());
            }

            character.setGripping(state.leftGripped(), state.rightGripped());
        }
    }

    /** Keeps the camera on every received character, or on the spawn while waiting. */
    private void centerCameraOnPlayers() {
        boolean found = false;
        float minX = 0, maxX = 0, minY = 0, maxY = 0;

        for (Character character : players) {
            if (character == null)
                continue;

            Vector2 position = character.getPosition();
            if (!found) {
                minX = maxX = position.x;
                minY = maxY = position.y;
                found = true;
            } else {
                minX = Math.min(minX, position.x);
                maxX = Math.max(maxX, position.x);
                minY = Math.min(minY, position.y);
                maxY = Math.max(maxY, position.y);
            }
        }

        if (!found) {
            camera.position.set(spawn.x, spawn.y, 0);
        } else {
            camera.position.set((minX + maxX) / 2f, (minY + maxY) / 2f, 0);
        }

        camera.zoom = 1.5f;
        camera.update();
    }

    /**
     * Sends the input at a fixed rate (60 Hz) and not once per frame, so the
     * server receives a steady stream even when rendering runs at another rate.
     */
    private void sendInput(float delta) {
        if (matchEndScreen.isVisible())
            return;

        if (!paused)
            updatePointer();

        inputAccumulator += delta;

        if (!paused && inputProcessor.consumeImpulse())
            queueEvent(InputPacket.EVENT_IMPULSE);

        int sent = 0;
        while (inputAccumulator >= INPUT_INTERVAL && sent < MAX_INPUTS_PER_FRAME) {
            inputAccumulator -= INPUT_INTERVAL;
            sent++;

            int events = 0;
            if (eventTicksRemaining > 0) {
                events = pendingEvents;
                eventTicksRemaining--;
                if (eventTicksRemaining == 0)
                    pendingEvents = 0;
            }

            // While paused it sends no buttons or keys, but the tick keeps
            // going out to deliver the pause and resume events.
            boolean active = !paused;
            network.sendInput(
                    worldPointer.x, worldPointer.y,
                    active && inputProcessor.isLeftMousePressed(),
                    active && inputProcessor.isRightMousePressed(),
                    active && inputProcessor.isAPressed(),
                    active && inputProcessor.isDPressed(),
                    active && inputProcessor.isWPressed(),
                    events);
        }

        // if the frame took too long, the backlog is dropped instead of accumulated
        if (inputAccumulator > INPUT_INTERVAL * MAX_INPUTS_PER_FRAME)
            inputAccumulator = 0f;
    }

    /**
     * Moves the virtual world cursor with the raw mouse delta. The cursor is
     * captured, so GLFW gives no real position: the deltas are accumulated.
     *
     * <p>The delta is scaled by a fixed factor instead of being unprojected
     * through the camera. That keeps the pointer independent of the window
     * size, the resolution, the zoom and the camera following the character
     * (a swinging character must not push itself through its own camera).
     *
     * <p>It is not limited to the screen edges: the cursor is never drawn and
     * the server only uses its movement.
     */
    private void updatePointer() {
        Vector2 delta = inputProcessor.getMouseDelta();
        // screen Y grows downwards, world Y grows upwards
        worldPointer.add(delta.x * POINTER_METERS_PER_COUNT, -delta.y * POINTER_METERS_PER_COUNT);
    }


    public boolean isPaused(){
        return paused;
    }

    public void pauseGame(){
        if(paused)
            return;

        queueEvent(InputPacket.EVENT_PAUSE);
        inputProcessor.resetMouseButtons();
        paused = true;
        pauseMenu.open();
    }

    public void resumeGame(){
        if(!paused)
            return;

        queueEvent(InputPacket.EVENT_RESUME);
        paused = false;
        pauseMenu.close();
    }

    /** Leaves the match for good: the server frees this slot and gets a roster. */
    public void leaveMatch(){
        network.leave();
    }

    /** Marks an event to be repeated for a few ticks instead of once. */
    private void queueEvent(int event) {
        pendingEvents |= event;
        eventTicksRemaining = EVENT_REPEAT_TICKS;
    }

    /**
     * Called when the window is minimized, for example by alt-tab out of the
     * fullscreen game. The match keeps running on the server, so the player
     * comes back to the pause menu instead of to a hand still being dragged.
     */
    @Override
    public void pause() {
        if (!matchEndScreen.isVisible())
            pauseGame();
    }

    @Override
    public void resize(int width, int height){
        // A minimized window reports a size of zero: keep the last real one.
        if (width <= 0 || height <= 0)
            return;

        viewport.update(width, height);

        pauseMenu.resize(width, height);
        matchEndScreen.resize(width, height);
        settingsMenu.resize(width, height);
    }

    @Override
    public void dispose() {
        terrainRenderer.dispose();
        mapRenderer.dispose();
        for (Character character : players) {
            if (character != null)
                character.dispose();
        }
        pauseMenu.dispose();
        matchEndScreen.dispose();
    }
}
