package com.clash.crashrace.watchdog;

import com.clash.crashrace.config.PluginConfig;

/**
 * Runs independently of the Bukkit main thread and scheduler, so it keeps working even while
 * the main thread is hung. Polls {@link HeartbeatKeeper} and fires {@code onHang} once the gap
 * since the last heartbeat crosses the configured threshold - intended to fire shortly before
 * the server's own Watchdog would forcibly kill the process, while there is still time to
 * record a culprit from already-collected data.
 */
public final class HangWatcherThread extends Thread {

    private final HeartbeatKeeper heartbeat;
    private final PluginConfig config;
    private final Runnable onHang;
    private volatile boolean running = true;
    private volatile long lastTriggerMs = 0L;

    public HangWatcherThread(HeartbeatKeeper heartbeat, PluginConfig config, Runnable onHang) {
        super("CrashRace-HangWatcher");
        setDaemon(true);
        this.heartbeat = heartbeat;
        this.config = config;
        this.onHang = onHang;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(Math.max(50L, config.checkIntervalMs()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long gap = System.currentTimeMillis() - heartbeat.lastHeartbeatMs();
            if (gap < config.hangThresholdMs()) {
                continue;
            }
            long now = System.currentTimeMillis();
            if (now - lastTriggerMs < config.detectionCooldownMs()) {
                continue;
            }
            lastTriggerMs = now;
            try {
                onHang.run();
            } catch (Throwable ignored) {
                // A callback failure must never take down the watcher thread.
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
