package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.utils.Disposable;

public class AssetService implements Disposable {
    private final AssetManager assetManager;

    public AssetService(FileHandleResolver fileHandleResolver) {
        this.assetManager = new AssetManager(fileHandleResolver);
    }

    public <T> T load(Asset<T> asset){
        this.assetManager.load(asset.getDescriptor());
        this.assetManager.finishLoading() ;
        return this.assetManager.get(asset.getDescriptor());
    }

    public <T> void queue(Asset<T> asset){
        this.assetManager.load(asset.getDescriptor());
    }

    public <T> T get(Asset<T> asset){
        return this.assetManager.get(asset.getDescriptor());
    }

    public boolean update(){
        return this.assetManager.update();
    }

    @Override
    public void dispose() {
        this.assetManager.dispose();
    }


}
