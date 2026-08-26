package com.TfPSR.CucoProject;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainMenu extends ScreenAdapter {
    private final Main game;
    private final Batch batch;
    private final Viewport viewport = new ScreenViewport(); //The viewport is for the scale, with this parameter we center the components and is the real size of the game
    private BitmapFont font;

    public MainMenu(Main game){
        this.game = game;
        this.batch = game.getBatch();
        this.font = new BitmapFont();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true); //We change the viewport parameters when we get the screen size
    }

    @Override
    public void render(float deltaTime){

        ScreenUtils.clear(Color.WHITE);


        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);//this, implements our scale at the game, if we dont have LibGDX uses it own matrix with another pixels
        batch.begin();//batch sends everything we want to draw, into an only package, deleting a possible image lost
        font.setColor(Color.BLACK);
        font.draw(batch, "Main Menu", 100, 200);


        batch.end();
    }


    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }
}
