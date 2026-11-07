package com.TfPSR.ToTheTop.menus;

import com.TfPSR.ToTheTop.Main;
import com.TfPSR.ToTheTop.asset.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;

public class LoadMenu extends ScreenAdapter {
    private final AssetService assetService;
    private final Main game;
    private ShapeRenderer shapeRenderer;
    private final SettingsMenu settingsMenu;
    private final Texture backgroundImage;
    private boolean settingsMenuTransferred = false;

    public LoadMenu(Main game, AssetService assetService) {
        this.game = game;
        this.assetService = assetService;
        this.settingsMenu = new SettingsMenu(assetService);
        this.backgroundImage = assetService.get(TextureAsset.LOAD);
    }

   @Override
   public void show(){
       this.shapeRenderer = new ShapeRenderer();

       for (SoundAsset sound : SoundAsset.values()) {
           assetService.queue(sound);
       }

       for(MusicAsset music : MusicAsset.values()){
           assetService.queue(music);
       }

       for(TextureAsset texture : TextureAsset.values()){
           if (texture != TextureAsset.LOAD) {
               assetService.queue(texture);
           }
       }

       for(TiledMapAsset tiledMap : TiledMapAsset.values()){
           assetService.queue(tiledMap);
       }
   }

    @Override
    public void render(float delta) {
        boolean finishedLoad = assetService.update();
        float progress = assetService.getProgress();

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Matrix4 screenProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        game.getBatch().setProjectionMatrix(screenProjection);
        game.getBatch().begin();
        game.getBatch().draw(backgroundImage, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getBatch().end();

        shapeRenderer.setProjectionMatrix(screenProjection);

        float progressBarWidth = Gdx.graphics.getWidth() * 0.6f;
        float progressBarHeight = 24f;
        float x = (Gdx.graphics.getWidth() - progressBarWidth) / 2;
        float y = Gdx.graphics.getHeight() / 4;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        drawRoundedBar(x, y, progressBarWidth, progressBarHeight);
        shapeRenderer.setColor(Color.WHITE);
        drawRoundedBar(x, y, progressBarWidth * progress, progressBarHeight);
        shapeRenderer.end();

        BitmapFont font = assetService.getSkin().getFont("buttonFont");
        GlyphLayout titleLayout = new GlyphLayout(font, "Loading...");
        GlyphLayout percentLayout = new GlyphLayout(font, Math.round(progress * 100) + "%");

        game.getBatch().setProjectionMatrix(screenProjection);
        game.getBatch().begin();
        font.draw( game.getBatch(), titleLayout,(Gdx.graphics.getWidth() - titleLayout.width) / 2f, y + progressBarHeight + 32f);
        font.draw(game.getBatch(),percentLayout,(Gdx.graphics.getWidth() - percentLayout.width) / 2f, y - 12f);
        game.getBatch().end();

        if(finishedLoad){
            game.addScreen(new MainMenu(game, assetService, settingsMenu));
            settingsMenuTransferred = true;
            game.setScreen(MainMenu.class);
        }
    }

    @Override
    public void dispose(){
        shapeRenderer.dispose();
        if (!settingsMenuTransferred) {
            settingsMenu.dispose();
        }
    }

    private void drawRoundedBar(float x, float y, float width, float height) {
        float radius = Math.min(height / 2f, width / 2f);
        float centerY = y + height / 2f;
        shapeRenderer.rect(x + radius,centerY - radius,width - 2f * radius,2f * radius);
        shapeRenderer.circle(x + radius, centerY, radius);
        shapeRenderer.circle(x + width - radius, centerY, radius);
    }
}

