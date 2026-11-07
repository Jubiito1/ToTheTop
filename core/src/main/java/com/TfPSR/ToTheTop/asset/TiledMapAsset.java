package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.TiledMap;

public enum TiledMapAsset implements Asset <TiledMap>{

    FIRST_TILED_MAP("map.tmx");

    private final AssetDescriptor<TiledMap> descriptor;

    TiledMapAsset(String tiledMapFile){
        this.descriptor = new AssetDescriptor<>("maps/" + tiledMapFile, TiledMap.class);
    }


    @Override
    public AssetDescriptor<TiledMap> getDescriptor(){
        return descriptor;
    }
}
