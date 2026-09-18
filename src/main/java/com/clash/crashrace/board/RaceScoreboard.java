package com.clash.crashrace.board;

import com.clash.crashrace.config.PluginConfig;
import com.clash.crashrace.rank.RankEntry;
import com.clash.crashrace.rank.RankingManager;
import com.clash.crashrace.util.DurationFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class RaceScoreboard {

    private static final String OBJECTIVE_ID = "crashrace";

    private final PluginConfig config;
    private final RankingManager rankingManager;
    private Scoreboard scoreboard;
    private Objective objective;

    public RaceScoreboard(PluginConfig config, RankingManager rankingManager) {
        this.config = config;
        this.rankingManager = rankingManager;
    }

    public void showToAll(long remainingMs) {
        ensureBoard();
        render(remainingMs);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    public void update(long remainingMs) {
        if (scoreboard == null) {
            return;
        }
        render(remainingMs);
    }

    public boolean isActive() {
        return scoreboard != null;
    }

    /** Re-applies the current board to one player, e.g. a player who joins mid-event and would
     *  otherwise be stuck on the server's default scoreboard until the event ends. */
    public void applyTo(Player player) {
        if (scoreboard != null) {
            player.setScoreboard(scoreboard);
        }
    }

    public void hideFromAll() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(main);
        }
        scoreboard = null;
        objective = null;
    }

    private void ensureBoard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(OBJECTIVE_ID, "dummy",
                ChatColor.translateAlternateColorCodes('&', config.scoreboardTitle()));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void render(long remainingMs) {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int line = 15;
        objective.getScore(ChatColor.GRAY + "残り時間: " + ChatColor.WHITE + DurationFormat.format(remainingMs)).setScore(line--);
        objective.getScore(" ").setScore(line--);

        var entries = rankingManager.list();
        String[] labels = {ChatColor.GOLD + "1位: ", ChatColor.GRAY + "2位: ", ChatColor.YELLOW + "3位: "};
        for (int i = 0; i < RankingManager.MAX_RANKS; i++) {
            String suffix = "  ";
            for (int pad = 0; pad < i; pad++) {
                suffix += " "; // keep each entry text unique to avoid scoreboard entry collisions
            }
            RankEntry entry = i < entries.size() ? entries.get(i) : null;
            String name = entry != null ? entry.playerName() : "----";
            objective.getScore(labels[i] + ChatColor.WHITE + name + suffix).setScore(line--);
        }
    }
}
