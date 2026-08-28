package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import org.w3c.dom.Text;

public enum TextureAsset implements Asset<Texture>{
    BACKGROUND_IMAGE("backgroundImage.png");

    private final AssetDescriptor<Texture> descriptor;

    TextureAsset(String textureFile){
        this.descriptor = new AssetDescriptor<>("images/" + textureFile, Texture.class);
    }
    @Override
    public AssetDescriptor<Texture> getDescriptor() {
        return descriptor;
    }
}
