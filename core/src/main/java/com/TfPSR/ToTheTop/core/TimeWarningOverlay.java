package com.TfPSR.ToTheTop.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

/**
 * Red vignette that warns the match is running out of time, without a HUD.
 * It appears in the last {@link #WARNING_SHARE} of the time and grows until the
 * end; the edges get clearly red while the center stays almost clear, so the
 * game is always visible.
 *
 * <p>Purely visual: the remaining time comes from the server state.
 */
public final class TimeWarningOverlay {

    /** Share of the time limit, at the end, during which the warning shows. */
    private static final float WARNING_SHARE = 0.3f;

    /** Opacity of the center and of the corners when the time is over. */
    private static final float CENTER_ALPHA = 0.12f;
    private static final float EDGE_ALPHA = 0.6f;

    /** Normalized distance from the center where the red edge starts and peaks. */
    private static final float EDGE_START = 0.45f;
    private static final float EDGE_END = 1.2f;

    private static final int TEXTURE_SIZE = 128;

    private final Texture vignette;
    private final Matrix4 projection = new Matrix4();

    public TimeWarningOverlay() {
        vignette = createVignette();
    }

    /**
     * How strong the warning is: 0 before the last {@link #WARNING_SHARE} of the
     * time, growing linearly to 1 when the time runs out.
     */
    public static float intensity(float timeRemaining, float timeLimit) {
        if (timeLimit <= 0f)
            return 0f;
        float share = timeRemaining / timeLimit;
        return MathUtils.clamp(1f - share / WARNING_SHARE, 0f, 1f);
    }

    /** Draws the vignette over the whole window, in screen space. */
    public void draw(Batch batch, float timeRemaining, float timeLimit) {
        float strength = intensity(timeRemaining, timeLimit);
        if (strength <= 0f)
            return;

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        projection.setToOrtho2D(0, 0, width, height);

        batch.setProjectionMatrix(projection);
        batch.begin();
        batch.setColor(1f, 1f, 1f, strength);
        batch.draw(vignette, 0, 0, width, height);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    public void dispose() {
        vignette.dispose();
    }

    /** Red texture, nearly clear in the center and opaque toward the edges. */
    private static Texture createVignette() {
        Pixmap pixmap = new Pixmap(TEXTURE_SIZE, TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        float half = TEXTURE_SIZE / 2f;

        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                float dx = (x + 0.5f - half) / half;
                float dy = (y + 0.5f - half) / half;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                float edge = MathUtils.clamp((distance - EDGE_START) / (EDGE_END - EDGE_START), 0f, 1f);
                edge = edge * edge * (3f - 2f * edge);   // smoothstep: soft transition
                float alpha = MathUtils.lerp(CENTER_ALPHA, EDGE_ALPHA, edge);

                pixmap.setColor(0.85f, 0.05f, 0.05f, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}
