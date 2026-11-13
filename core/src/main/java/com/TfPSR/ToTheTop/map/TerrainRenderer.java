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

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Array<Triangle> triangles = new Array<>();
    private final Array<float[]> outlines = new Array<>();

    public TerrainRenderer(MapObjects surfaces) {
        EarClippingTriangulator triangulator = new EarClippingTriangulator();

        int rockIndex = 0;
        for (MapObject object : surfaces) {
            if (!(object instanceof PolygonMapObject polygon))
                continue;

            float[] pixels = polygon.getPolygon().getTransformedVertices();
            float[] meters = new float[pixels.length];
            for (int i = 0; i < pixels.length; i++)
                meters[i] = pixels[i] / Constants.PIXELS_PER_METER;

            Color color = ROCK_COLORS[rockIndex++ % ROCK_COLORS.length];
            ShortArray indices = triangulator.computeTriangles(meters);
            for (int i = 0; i + 2 < indices.size; i += 3) {
                int a = indices.get(i) * 2;
                int b = indices.get(i + 1) * 2;
                int c = indices.get(i + 2) * 2;
                triangles.add(new Triangle(
                        meters[a], meters[a + 1],
                        meters[b], meters[b + 1],
                        meters[c], meters[c + 1],
                        color));
            }

            outlines.add(meters);
        }
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
        shapes.setColor(OUTLINE_COLOR);
        for (float[] outline : outlines)
            shapes.polygon(outline);
        shapes.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }

    private record Triangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
    }
}
