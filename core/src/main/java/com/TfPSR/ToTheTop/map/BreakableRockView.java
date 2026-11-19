package com.TfPSR.ToTheTop.map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ShortArray;

import java.util.Random;

/**
 * Drawing of one breakable rock. The server decides its wear and whether it is
 * broken; this only shows it: an ochre rock with cracks, that turns red, cracks
 * further and shakes as it wears, and is only a faint outline while broken.
 */
final class BreakableRockView {

    /** Tiled type of the rocks that crumble when held too long. */
    private static final String BREAKABLE_TYPE = "Breakable";

    /** Wire values sent by the server: wear 0..200, or 255 while broken. */
    private static final int WIRE_BROKEN = 255;
    private static final float WIRE_MAX_WEAR = 200f;

    private static final Color INTACT_COLOR = new Color(0.66f, 0.47f, 0.24f, 1f);
    private static final Color WORN_COLOR = new Color(0.86f, 0.16f, 0.10f, 1f);
    private static final Color OUTLINE_COLOR = new Color(0.90f, 0.70f, 0.40f, 1f);
    private static final Color CRACK_COLOR = new Color(0.16f, 0.08f, 0.04f, 1f);
    private static final Color BROKEN_OUTLINE_COLOR = new Color(0.42f, 0.33f, 0.22f, 1f);

    private static final int CRACKS = 4;
    private static final int CRACK_SEGMENTS = 4;
    private static final float CRACK_WIDTH = 0.045f;
    /** Wear from which the rock starts shaking, and how far it shakes at the end (m). */
    private static final float SHAKE_FROM = 0.6f;
    private static final float MAX_SHAKE = 0.07f;

    private final float[] vertices;
    private final ShortArray triangles;
    /** Crack polylines, from the centre outward, as x/y pairs. */
    private final float[][] cracks = new float[CRACKS][];
    private final float shakePhase;
    private final float[] shifted;
    private final Color color = new Color();

    BreakableRockView(float[] vertices, EarClippingTriangulator triangulator) {
        this.vertices = vertices;
        this.triangles = new ShortArray(triangulator.computeTriangles(vertices));
        this.shifted = new float[vertices.length];

        float cx = 0, cy = 0;
        int n = vertices.length / 2;
        for (int i = 0; i < vertices.length; i += 2) {
            cx += vertices[i];
            cy += vertices[i + 1];
        }
        cx /= n;
        cy /= n;
        float radius = 0;
        for (int i = 0; i < vertices.length; i += 2)
            radius += (float) Math.hypot(vertices[i] - cx, vertices[i + 1] - cy);
        radius = radius / n * 0.75f;

        // Same cracks on every client: the random seed comes from the shape.
        Random random = new Random(Math.round(cx * 1000) * 31L + Math.round(cy * 1000));
        float start = random.nextFloat() * MathUtils.PI2;
        for (int k = 0; k < CRACKS; k++) {
            float angle = start + k * MathUtils.PI2 / CRACKS + (random.nextFloat() - 0.5f) * 0.8f;
            float dx = MathUtils.cos(angle), dy = MathUtils.sin(angle);
            float[] line = new float[(CRACK_SEGMENTS + 1) * 2];
            for (int s = 0; s <= CRACK_SEGMENTS; s++) {
                float along = radius * s / CRACK_SEGMENTS;
                float side = s == 0 ? 0 : (random.nextFloat() - 0.5f) * 0.18f;
                line[s * 2] = cx + dx * along - dy * side;
                line[s * 2 + 1] = cy + dy * along + dx * side;
            }
            cracks[k] = line;
        }
        shakePhase = random.nextFloat() * 100f;
    }

    /** Tiled stores the object type as "type" or, in newer versions, as "class". */
    static boolean isBreakable(MapObject object) {
        Object type = object.getProperties().get("type");
        if (type == null)
            type = object.getProperties().get("class");
        return BREAKABLE_TYPE.equals(type);
    }

    /** Body and cracks, inside a Filled batch. Nothing while broken. */
    void drawFill(ShapeRenderer shapes, int wireState, float time) {
        if (wireState == WIRE_BROKEN)
            return;

        float wear = Math.min(1f, wireState / WIRE_MAX_WEAR);
        float[] v = shake(wear, time);
        float ox = v == vertices ? 0 : v[0] - vertices[0];
        float oy = v == vertices ? 0 : v[1] - vertices[1];

        shapes.setColor(color.set(INTACT_COLOR).lerp(WORN_COLOR, wear));
        for (int i = 0; i + 2 < triangles.size; i += 3) {
            int a = triangles.get(i) * 2, b = triangles.get(i + 1) * 2, c = triangles.get(i + 2) * 2;
            shapes.triangle(v[a], v[a + 1], v[b], v[b + 1], v[c], v[c + 1]);
        }

        // A short crack is always visible, so the rock is recognisable; the
        // cracks grow toward the edge as it wears.
        int visible = 1 + Math.round(wear * (CRACK_SEGMENTS - 1));
        shapes.setColor(CRACK_COLOR);
        for (float[] crack : cracks) {
            for (int s = 0; s < visible; s++) {
                shapes.rectLine(crack[s * 2] + ox, crack[s * 2 + 1] + oy,
                        crack[s * 2 + 2] + ox, crack[s * 2 + 3] + oy, CRACK_WIDTH);
            }
        }
    }

    /** Outline inside a Line batch: normal while intact, faint while broken. */
    void drawOutline(ShapeRenderer shapes, int wireState, float time) {
        if (wireState == WIRE_BROKEN) {
            shapes.setColor(BROKEN_OUTLINE_COLOR);
            shapes.polygon(vertices);
            return;
        }
        float wear = Math.min(1f, wireState / WIRE_MAX_WEAR);
        shapes.setColor(OUTLINE_COLOR);
        shapes.polygon(shake(wear, time));
    }

    /** The vertices displaced by the shake, or the originals when it does not shake yet. */
    private float[] shake(float wear, float time) {
        if (wear <= SHAKE_FROM)
            return vertices;
        float amount = (wear - SHAKE_FROM) / (1f - SHAKE_FROM) * MAX_SHAKE;
        float dx = MathUtils.sin(time * 55f + shakePhase) * amount;
        float dy = MathUtils.cos(time * 47f + shakePhase) * amount;
        for (int i = 0; i < vertices.length; i += 2) {
            shifted[i] = vertices[i] + dx;
            shifted[i + 1] = vertices[i + 1] + dy;
        }
        return shifted;
    }
}
