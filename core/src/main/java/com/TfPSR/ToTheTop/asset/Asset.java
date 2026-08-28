package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.assets.AssetDescriptor;

public interface Asset<T>{
    AssetDescriptor<T> getDescriptor();
}
