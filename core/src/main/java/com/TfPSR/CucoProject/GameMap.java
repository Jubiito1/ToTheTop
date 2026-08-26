package com.TfPSR.CucoProject;

import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;

public class GameMap {

    private final TiledMap map;
    private final MapLayer playerLayer;
    private final MapLayer surfaceLayer;

    public GameMap(String mapPath) {
        TmxMapLoader loader = new TmxMapLoader();

        map = loader.load(mapPath);
        final MapGroupLayer gameplayLayer = (MapGroupLayer) map.getLayers().get("Gameplay");
        playerLayer = gameplayLayer.getLayers().get("Player");
        surfaceLayer = gameplayLayer.getLayers().get("Surface");
    }

    public MapObjects getSurfaceObjects() {
        return surfaceLayer.getObjects();
    }

    public Vector2 getPlayerSpawn() {
        float x = playerLayer.getObjects().get("PlayerSpawn").getProperties().get("x", Float.class);
        float y = playerLayer.getObjects().get("PlayerSpawn").getProperties().get("y", Float.class);
        return pixelsToMeters(x, y);
    }

    private Vector2 pixelsToMeters(float x, float y) {
        return new Vector2(x / Constants.PIXELS_PER_METER, y / Constants.PIXELS_PER_METER);
    }

    public TiledMap getTiledMap() {
        return map;
    }
}
