package com.TfPSR.CucoProject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game { //We use Game, because it has better methods of pause, play, and resume
    private Batch batch;
    private Texture image;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MainMenu(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    public Batch getBatch(){
        return batch;
    }
}
