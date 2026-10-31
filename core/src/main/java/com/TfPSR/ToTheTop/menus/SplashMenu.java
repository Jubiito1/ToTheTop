package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.AssetService;
import com.TfPSR.ToTheTop.asset.TextureAsset;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class SplashMenu extends ScreenAdapter{
    private final Stage stage;
    private final Texture glowTexture;
    private final Image glowImage, logoImage;
    private boolean finished = false;
    private final Main game;
    private final AssetService assetService;

    public SplashMenu(Main game, AssetService assetService){
        this.game = game;
        this.assetService = assetService;
        stage = new Stage(new ScreenViewport());
        glowTexture = createGlowTexture();
        glowImage = new Image(glowTexture);
        logoImage = new Image(assetService.load(TextureAsset. LOGO));

        stage.addActor(glowImage);
        stage.addActor(logoImage);
    }

    private Texture createGlowTexture(){
        int size = 256;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float dx = (x - size / 2f) / (size / 2f);
                float dy = (y - size / 2f) / (size / 2f);
                float dist = (float) Math.sqrt(dx * dx + dy *dy);
                float a = Math.max(0, 1f - dist);

                pixmap.setColor(1f, 1f, 1f, a *a);
                pixmap.drawPixel(x, y);
            }
        }
        Texture glowTexture = new Texture(pixmap);
        pixmap.dispose();

        return glowTexture;
    }


    @Override
    public void show(){
        finished = false;

        glowImage.getColor().a = 0f;
        logoImage.getColor().a = 0f;

        logoImage.addAction(Actions.sequence(
            Actions.fadeIn(1f),
            Actions.delay(1.5f),
            Actions.fadeOut(1f),
            Actions.run(new Runnable() {
                @Override
                public void run() {
                    finished = true;
                }
            })
        ));

        glowImage.setColor(1f, 1f, 1f, 0f);
        glowImage.addAction(Actions.sequence(
            Actions.fadeIn(1.5f),
            Actions.delay(1f),
            Actions.fadeOut(1f)
        ));
    }

    @Override
    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);

        float centerX = stage.getWidth() / 2f;
        float centerY = stage.getHeight() / 2f;

        logoImage.setPosition(
            centerX - logoImage.getWidth() / 2f,
            centerY - logoImage.getHeight() / 2f
        );

        glowImage.setPosition(
            centerX - glowImage.getWidth() / 2f,
            centerY - glowImage.getHeight() / 2f
        );
    }

    @Override
    public void render(float delta){
        Gdx.gl.glClearColor(0,0,0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        if(finished){
            game.addScreen(new LoadMenu(game, assetService));
            game.setScreen(LoadMenu.class);
        }
    }

    @Override
    public void dispose(){
        stage.dispose();
        glowTexture.dispose();
    }
}
