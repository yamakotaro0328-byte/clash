package com.clash.crashrace.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class PluginConfig {

    private final JavaPlugin plugin;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public void save() {
        plugin.saveConfig();
    }

    // --- event ---
    public int defaultDurationMinutes() {
        return cfg().getInt("event.default-duration-minutes", 15);
    }

    public int extendStepMinutes() {
        return cfg().getInt("event.extend-step-minutes", 5);
    }

    public boolean autoEndWhenFull() {
        return cfg().getBoolean("event.auto-end-when-full", true);
    }

    public void setAutoEndWhenFull(boolean value) {
        cfg().set("event.auto-end-when-full", value);
        save();
    }

    public boolean distinctPlayersOnly() {
        return cfg().getBoolean("event.distinct-players-only", true);
    }

    // --- watchdog ---
    public long hangThresholdMs() {
        return cfg().getLong("watchdog.hang-threshold-ms", 8000L);
    }

    public void setHangThresholdMs(long value) {
        cfg().set("watchdog.hang-threshold-ms", Math.max(1000L, value));
        save();
    }

    public long checkIntervalMs() {
        return cfg().getLong("watchdog.check-interval-ms", 500L);
    }

    public int suspectWindowSeconds() {
        return cfg().getInt("watchdog.suspect-window-seconds", 30);
    }

    public long detectionCooldownMs() {
        return cfg().getLong("watchdog.detection-cooldown-ms", 15000L);
    }

    /** Heartbeat gap must drop below this before new detections resume, on top of the cooldown timer. */
    public long recoveryThresholdMs() {
        return cfg().getLong("watchdog.recovery-threshold-ms", 2000L);
    }

    // --- block weights ---
    public Map<String, Integer> blockWeights() {
        Map<String, Integer> weights = new HashMap<>();
        var section = cfg().getConfigurationSection("block-weights");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                weights.put(key.toUpperCase(java.util.Locale.ROOT), section.getInt(key));
            }
        }
        weights.putIfAbsent("DEFAULT", 1);
        return weights;
    }

    // --- mitigation ---
    public boolean mitigationEnabled() {
        return cfg().getBoolean("mitigation.enabled", true);
    }

    public void setMitigationEnabled(boolean value) {
        cfg().set("mitigation.enabled", value);
        save();
    }

    public int mitigationChunkRadius() {
        return cfg().getInt("mitigation.chunk-radius", 1);
    }

    public boolean mitigationClearEntities() {
        return cfg().getBoolean("mitigation.clear-entities", true);
    }

    public boolean mitigationReloadChunk() {
        return cfg().getBoolean("mitigation.reload-chunk", true);
    }

    public boolean mitigationClearRedstone() {
        return cfg().getBoolean("mitigation.clear-redstone", true);
    }

    // --- discord ---
    public boolean discordEnabled() {
        return cfg().getBoolean("discord.enabled", false);
    }

    public String discordBotToken() {
        return cfg().getString("discord.bot-token", "");
    }

    public String discordChannelId() {
        return cfg().getString("discord.channel-id", "");
    }

    public boolean discordNotifyOnStart() {
        return cfg().getBoolean("discord.notify-on-start", true);
    }

    public boolean discordNotifyOnDetection() {
        return cfg().getBoolean("discord.notify-on-detection", true);
    }

    public boolean discordNotifyOnEnd() {
        return cfg().getBoolean("discord.notify-on-end", true);
    }

    // --- scoreboard ---
    public int scoreboardUpdateIntervalTicks() {
        return cfg().getInt("scoreboard.update-interval-ticks", 20);
    }

    public String scoreboardTitle() {
        return cfg().getString("scoreboard.title", "&c&lCrashRace");
    }
}
