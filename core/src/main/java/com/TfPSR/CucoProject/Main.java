package com.TfPSR.CucoProject;

import com.TfPSR.CucoProject.network.client.Client;
import com.TfPSR.CucoProject.network.server.GameServer;
import com.TfPSR.CucoProject.network.threads.BroadCastSender;
import com.TfPSR.CucoProject.screens.GameScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import javax.swing.plaf.TableHeaderUI;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game { //We use Game, because it has better methods of pause, play, and resume
    private static final float MAX_WIDTH = 16f;
    private static final float MAX_HEIGHT = 9f;

    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();
    private Batch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private boolean isServer;

    Client client;
    GameServer gameServer;
    BroadCastSender broadCastSender;



    public Main(boolean isServer){
        this.isServer = isServer;
    };

    @Override
    public void create() {
        if(!isServer){
            try {
                client = new Client(); //It needs the try catch, because of the constructor, it handles socket exceptions
                new Thread((Runnable) client).start(); //We send client to other thread to get it out of the main game thread, beacuse it stands by all the process
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }else{
            try {
                broadCastSender = new BroadCastSender();
                gameServer = new GameServer();

                new Thread(broadCastSender).start();
                new Thread((Runnable) gameServer).start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(MAX_WIDTH, MAX_HEIGHT, camera);
        addScreen(new GameScreen(this));
        setScreen(GameScreen.class);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, true);

    }

    public void addScreen(Screen screen) {
        screenCache.put(screen.getClass(), screen);
    }

    public void setScreen(Class<? extends Screen> screenClass) {
        Screen screen = screenCache.get(screenClass);
        if (screen == null) {
            throw new GdxRuntimeException("No screen with class " + screenClass + " found in screenCache");
        }
        super.setScreen(screen);
    }

    @Override
    public void dispose() {
        screenCache.values().forEach(Screen::dispose);
        screenCache.clear();
        this.batch.dispose();
    }

    public Viewport getViewport() {
        return this.viewport;
    }

    public OrthographicCamera getCamera() {
        return this.camera;
    }

    public Batch getBatch() {
        return this.batch;
    }
}
