package com.TfPSR.ToTheTop;

public class StartFlag {
    public static volatile boolean startFlag = false;   // ThreadClient la escribe, render thread la lee
}
