package com.TfPSR.ToTheTop.map;

import com.TfPSR.ToTheTop.core.Constants;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ShortArray;

//Here we draw the surface, although map controlls the collisions, we draw the poligons here
public class TerrainRenderer implements Disposable {

    private static final Color[] ROCK_COLORS = {
            new Color(0.26f, 0.28f, 0.32f, 1f),
            new Color(0.30f, 0.32f, 0.36f, 1f),
            new Color(0.23f, 0.25f, 0.29f, 1f),
            new Color(0.28f, 0.30f, 0.34f, 1f),
            new Color(0.24f, 0.27f, 0.31f, 1f)
    };

    private static final Color OUTLINE_COLOR = new Color(0.52f, 0.58f, 0.66f, 1f);

    /** Colour the rocks fade into toward the top of the climb: snowy, slightly cold. */
    private static final Color SNOW_COLOR = new Color(0.88f, 0.90f, 0.94f, 1f);
    private static final Color SNOW_OUTLINE_COLOR = new Color(0.66f, 0.71f, 0.80f, 1f);
    /** Share of snow on the highest rocks below the summit (1 would be pure snow). */
    private static final float MAX_SNOW = 0.85f;

    /** The summit rock, the highest of the map, is golden. */
    private static final Color SUMMIT_COLOR = new Color(0.86f, 0.68f, 0.20f, 1f);
    private static final Color SUMMIT_OUTLINE_COLOR = new Color(1f, 0.86f, 0.40f, 1f);

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Array<Triangle> triangles = new Array<>();
    private final Array<Outline> outlines = new Array<>();

    public TerrainRenderer(MapObjects surfaces) {
        Array<float[]> rocks = new Array<>();
        for (MapObject object : surfaces) {
            if (!(object instanceof PolygonMapObject polygon))
                continue;

            float[] pixels = polygon.getPolygon().getTransformedVertices();
            float[] meters = new float[pixels.length];
            for (int i = 0; i < pixels.length; i++)
                meters[i] = pixels[i] / Constants.PIXELS_PER_METER;
            rocks.add(meters);
        }

        // The summit is the rock that reaches highest; the others whiten
        // between the lowest top (the floor) and the highest top below the summit.
        int summit = -1;
        for (int i = 0; i < rocks.size; i++) {
            if (summit == -1 || topOf(rocks.get(i)) > topOf(rocks.get(summit)))
                summit = i;
        }
        float lowest = Float.MAX_VALUE;
        float highest = -Float.MAX_VALUE;
        for (int i = 0; i < rocks.size; i++) {
            if (i == summit)
                continue;
            lowest = Math.min(lowest, topOf(rocks.get(i)));
            highest = Math.max(highest, topOf(rocks.get(i)));
        }

        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        for (int i = 0; i < rocks.size; i++) {
            float[] meters = rocks.get(i);
            Color fill;
            Color outline;
            if (i == summit) {
                fill = SUMMIT_COLOR;
                outline = SUMMIT_OUTLINE_COLOR;
            } else {
                float height = highest > lowest ? (topOf(meters) - lowest) / (highest - lowest) : 0f;
                float snow = MAX_SNOW * height;
                fill = new Color(ROCK_COLORS[i % ROCK_COLORS.length]).lerp(SNOW_COLOR, snow);
                outline = new Color(OUTLINE_COLOR).lerp(SNOW_OUTLINE_COLOR, snow);
            }

            ShortArray indices = triangulator.computeTriangles(meters);
            for (int t = 0; t + 2 < indices.size; t += 3) {
                int a = indices.get(t) * 2;
                int b = indices.get(t + 1) * 2;
                int c = indices.get(t + 2) * 2;
                triangles.add(new Triangle(
                        meters[a], meters[a + 1],
                        meters[b], meters[b + 1],
                        meters[c], meters[c + 1],
                        fill));
            }

            outlines.add(new Outline(meters, outline));
        }
    }

    /** Highest y of a polygon given as x/y pairs, in meters. */
    private static float topOf(float[] vertices) {
        float top = -Float.MAX_VALUE;
        for (int i = 1; i < vertices.length; i += 2)
            top = Math.max(top, vertices[i]);
        return top;
    }

    public void render(Matrix4 projection) {
        shapes.setProjectionMatrix(projection);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Triangle triangle : triangles) {
            shapes.setColor(triangle.color);
            shapes.triangle(triangle.x1, triangle.y1,
                    triangle.x2, triangle.y2,
                    triangle.x3, triangle.y3);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (Outline outline : outlines) {
            shapes.setColor(outline.color);
            shapes.polygon(outline.vertices);
        }
        shapes.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }

    private record Triangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
    }

    private record Outline(float[] vertices, Color color) {
    }
}
