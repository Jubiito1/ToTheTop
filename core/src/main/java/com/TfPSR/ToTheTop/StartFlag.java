package com.TfPSR.ToTheTop;

public class StartFlag {
    public static volatile boolean startFlag = false;   // ThreadClient writes it, the render thread reads it
}
