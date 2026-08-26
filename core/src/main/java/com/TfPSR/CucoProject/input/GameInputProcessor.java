package com.TfPSR.CucoProject.input;

import com.TfPSR.CucoProject.entity.Character;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameInputProcessor extends InputAdapter {

    private final Character player;
    private final Viewport viewport;

    private final Vector2 mousePosition = new Vector2();

    public GameInputProcessor(Character player, Viewport viewport) {
        this.player = player;
        this.viewport = viewport;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        mousePosition.set(screenX, screenY);
        viewport.unproject(mousePosition);

        if(button == Input.Buttons.LEFT) {
            player.onLeftClickPressed(mousePosition);
            return true;
        }

        if(button == Input.Buttons.RIGHT) {
            player.onRightClickPressed(mousePosition);
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
}
