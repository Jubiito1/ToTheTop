package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.core.Constants;
import com.TfPSR.ToTheTop.physics.ShapeFactory;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;

public class Rocks {
    ArrayList<Body> rocksBodies = new ArrayList<>();


    public Rocks(MapObjects rocksObjects, World world) {
        for(MapObject rock : rocksObjects) {
            PolygonMapObject rockPolygon = (PolygonMapObject) rock;
            float[] rockVertices = rockPolygon.getPolygon().getTransformedVertices();
            Vector2[] vertices = new Vector2[rockVertices.length / 2];
            for(int i = 0; i < rockVertices.length; i += 2) {
                float x = rockVertices[i] / Constants.PIXELS_PER_METER;
                float y = rockVertices[i + 1] / Constants.PIXELS_PER_METER;
                vertices[i / 2] = new Vector2(x, y);
            }
            Body body = ShapeFactory.createClosedChain(vertices, BodyDef.BodyType.StaticBody, world, 0, 1f, 0f, false, (short) -2);
            rocksBodies.add(body);
        }


    }
}
