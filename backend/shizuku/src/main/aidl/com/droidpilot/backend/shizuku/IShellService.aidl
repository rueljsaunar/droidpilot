package com.droidpilot.backend.shizuku;

interface IShellService {
    void destroy() = 16777114;
    String run(String command) = 1;
    byte[] screencap() = 2;
    String logcat() = 3;
}
