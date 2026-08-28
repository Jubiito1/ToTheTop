package com.TfPSR.CucoProject;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class GameInputProcessor extends InputAdapter {

    private final Character player;

    public GameInputProcessor(Character player) {
        this.player = player;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if(button == Input.Buttons.LEFT) {
            player.onLeftClickPressed();
            return true;
        }

        if(button == Input.Buttons.RIGHT) {
            player.onRightClickPressed();
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if(button == Input.Buttons.LEFT) {
            player.onLeftClickReleased();
            return true;
        }

        if(button == Input.Buttons.RIGHT) {
            player.onRightClickReleased();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyDown(int keycode){
        if(keycode == Input.Keys.SPACE){
            player.onSpacePressed();
            return true;
        }

        return false;
    }
}
