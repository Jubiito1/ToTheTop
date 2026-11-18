package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.TiledMapAsset;
import com.TfPSR.ToTheTop.core.Constants;
import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class GameMap {

    private final TiledMap map;
    private final MapLayer playerLayer;
    private final MapLayer surfaceLayer;

    /** Name of the rectangle, in the Player layer, that bounds the level sideways. */
    private static final String PLAY_AREA_OBJECT = "PlayArea";

    public GameMap(AssetService assetService) {
        map = (assetService.get(TiledMapAsset.FIRST_TILED_MAP));
        final MapGroupLayer gameplayLayer = (MapGroupLayer) map.getLayers().get("Gameplay");
        playerLayer = gameplayLayer.getLayers().get("Player");
        surfaceLayer = gameplayLayer.getLayers().get("Surface");
    }

    public MapObjects getSurfaceObjects() {
        return surfaceLayer.getObjects();
    }

    /**
     * Side limits of the level, in meters: the {@value #PLAY_AREA_OBJECT}
     * rectangle drawn in Tiled, or {@code null} when the map has none. Only its
     * left and right edges are used.
     */
    public Rectangle getPlayArea() {
        MapObject area = playerLayer.getObjects().get(PLAY_AREA_OBJECT);
        if (!(area instanceof RectangleMapObject rectangle))
            return null;
        Rectangle pixels = rectangle.getRectangle();
        return new Rectangle(pixels.x / Constants.PIXELS_PER_METER, pixels.y / Constants.PIXELS_PER_METER,
                pixels.width / Constants.PIXELS_PER_METER, pixels.height / Constants.PIXELS_PER_METER);
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
