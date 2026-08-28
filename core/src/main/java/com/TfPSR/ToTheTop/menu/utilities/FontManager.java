package com.TfPSR.ToTheTop.menu.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FontManager {
    private static BitmapFont mainFont;

    public static void loadFonts(){
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.TTF"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        mainFont = generator.generateFont(parameter);
        generator.dispose();

    }

    public static BitmapFont getMainFont(){
        return mainFont;
    }

    public static void dispose(){
        if(mainFont!=null)mainFont.dispose();
    }

}
