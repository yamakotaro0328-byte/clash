package com.clash.crashrace.watchdog;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Updates a timestamp every server tick. As long as the main thread is alive and ticking,
 * {@link #lastHeartbeatMs()} keeps advancing; if the main thread hangs, it stops advancing,
 * which {@link HangWatcherThread} (running on its own thread) uses to detect a near-crash.
 */
public final class HeartbeatKeeper {

    private volatile long lastHeartbeatMs = System.currentTimeMillis();
    private int taskId = -1;

    public void start(JavaPlugin plugin) {
        lastHeartbeatMs = System.currentTimeMillis();
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin,
                () -> lastHeartbeatMs = System.currentTimeMillis(), 0L, 1L);
    }

    public void stop(JavaPlugin plugin) {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public long lastHeartbeatMs() {
        return lastHeartbeatMs;
    }
}
