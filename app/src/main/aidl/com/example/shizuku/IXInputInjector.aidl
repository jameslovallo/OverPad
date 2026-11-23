package com.example.shizuku;

interface IXInputInjector {
    int getUid();
    boolean isUinputWritable();
    String getBackendMode();
    int getVirtualDeviceId();
    String getDiagnostics();
    boolean ensureVirtualGamepad();
    void disconnectVirtualGamepad();
    void sendGamepadState(
        int buttonsMask,
        float leftTrigger,
        float rightTrigger,
        float thumbLX,
        float thumbLY,
        float thumbRX,
        float thumbRY,
        float hatX,
        float hatY
    );
    void resetAll();
    void destroy();
}
