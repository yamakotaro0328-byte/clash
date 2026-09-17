package com.clash.crashrace.state;

import com.clash.crashrace.board.RaceScoreboard;
import com.clash.crashrace.config.PluginConfig;
import com.clash.crashrace.detect.CulpritResolver;
import com.clash.crashrace.detect.ResolvedCulprit;
import com.clash.crashrace.mitigate.ChunkMitigator;
import com.clash.crashrace.rank.RankEntry;
import com.clash.crashrace.rank.RankingManager;
import com.clash.crashrace.track.PlacementTracker;
import com.clash.crashrace.watchdog.HangWatcherThread;
import com.clash.crashrace.watchdog.HeartbeatKeeper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EventManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final PlacementTracker tracker;
    private final RankingManager rankingManager;
    private final RaceScoreboard scoreboard;
    private final HeartbeatKeeper heartbeat = new HeartbeatKeeper();
    private final ChunkMitigator mitigator;

    private volatile EventState state = EventState.IDLE;
    private volatile long endTimeMs = 0L;
    private HangWatcherThread watcherThread;
    private BukkitTask tickTask;

    public EventManager(JavaPlugin plugin, PluginConfig config, PlacementTracker tracker, RankingManager rankingManager) {
        this.plugin = plugin;
        this.config = config;
        this.tracker = tracker;
        this.rankingManager = rankingManager;
        this.scoreboard = new RaceScoreboard(config, rankingManager);
        this.mitigator = new ChunkMitigator(plugin, config);
    }

    public EventState state() {
        return state;
    }

    public long remainingMs() {
        return Math.max(0L, endTimeMs - System.currentTimeMillis());
    }

    public RaceScoreboard scoreboard() {
        return scoreboard;
    }

    public void start(int durationMinutes) {
        stopInternalTasks();
        tracker.clear();
        rankingManager.clear();
        endTimeMs = System.currentTimeMillis() + durationMinutes * 60_000L;
        state = EventState.RUNNING;

        heartbeat.start(plugin);
        watcherThread = new HangWatcherThread(heartbeat, config, this::onHangDetectedAsync);
        watcherThread.start();

        scoreboard.showToAll(remainingMs());
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 20L);

        Bukkit.broadcastMessage(ChatColor.RED + "[CrashRace] " + ChatColor.WHITE
                + "大会がスタートしました！ 制限時間: " + durationMinutes + "分");
    }

    public void extend(int minutes) {
        if (state != EventState.RUNNING) {
            return;
        }
        endTimeMs += minutes * 60_000L;
        scoreboard.update(remainingMs());
        Bukkit.broadcastMessage(ChatColor.RED + "[CrashRace] " + ChatColor.WHITE
                + "イベントが" + minutes + "分延長されました！");
    }

    public void stop() {
        if (state != EventState.RUNNING) {
            return;
        }
        state = EventState.ENDED;
        stopInternalTasks();
        announceResults();
    }

    public void reset() {
        stopInternalTasks();
        tracker.clear();
        rankingManager.clear();
        state = EventState.IDLE;
        scoreboard.hideFromAll();
    }

    private void stopInternalTasks() {
        heartbeat.stop(plugin);
        if (watcherThread != null) {
            watcherThread.shutdown();
            watcherThread = null;
        }
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void onTick() {
        if (state != EventState.RUNNING) {
            return;
        }
        tracker.pruneOlderThan(config.suspectWindowSeconds() * 2_000L);
        long remaining = remainingMs();
        scoreboard.update(remaining);
        if (remaining <= 0) {
            state = EventState.ENDED;
            stopInternalTasks();
            announceResults();
        }
    }

    /** Called from the watcher thread. Must not touch Bukkit API directly here. */
    private void onHangDetectedAsync() {
        CulpritResolver resolver = new CulpritResolver(tracker, config.blockWeights(), config.suspectWindowSeconds());
        ResolvedCulprit culprit = resolver.resolve(id -> config.distinctPlayersOnly() && rankingManager.hasPlayer(id));
        if (culprit == null) {
            plugin.getLogger().warning("[CrashRace] Hang detected but no culprit could be identified"
                    + " (no recent block placements logged).");
            return;
        }
        RankEntry entry = rankingManager.addIfRoom(culprit.playerId(), culprit.playerName(), culprit.chunk(),
                culprit.score(), System.currentTimeMillis());
        plugin.getLogger().warning("[CrashRace] Hang detected. Suspected cause: " + culprit.playerName()
                + " in chunk " + culprit.chunk().chunkX() + "," + culprit.chunk().chunkZ()
                + " (world " + culprit.chunk().worldName() + ", score " + culprit.score() + ")"
                + (entry != null ? " -> rank " + entry.rank() : " (ranking already full)"));

        // Hop back onto the main thread for anything touching live world/player state; this task
        // simply waits in queue if the main thread is still catching up from the hang.
        Bukkit.getScheduler().runTask(plugin, () -> {
            mitigator.mitigate(culprit.chunk());
            if (entry != null) {
                scoreboard.update(remainingMs());
                Bukkit.broadcastMessage(ChatColor.RED + "[CrashRace] " + ChatColor.YELLOW
                        + entry.rank() + "位" + ChatColor.WHITE + ": " + culprit.playerName()
                        + " の装置が検知されました！");
                if (rankingManager.isFull() && config.autoEndWhenFull() && state == EventState.RUNNING) {
                    stop();
                }
            }
        });
    }

    private void announceResults() {
        scoreboard.update(0);
        Bukkit.broadcastMessage(ChatColor.RED + "[CrashRace] " + ChatColor.WHITE + "大会終了！結果発表:");
        var entries = rankingManager.list();
        if (entries.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "誰も検知されませんでした。");
            return;
        }
        for (RankEntry entry : entries) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "" + entry.rank() + "位: " + ChatColor.WHITE + entry.playerName());
        }
    }
}
