package com.xusu.sshlinkterminal.shizuku;

interface IShizukuDaemonService {
    int getVersion();
    String applyKeepAliveHardening(String packageName);
    String queryHardeningStatus(String packageName);
    void startWatchdog(String packageName, String serviceClassName);
    void heartbeat();
    void stopWatchdog();
    void destroy();
}
