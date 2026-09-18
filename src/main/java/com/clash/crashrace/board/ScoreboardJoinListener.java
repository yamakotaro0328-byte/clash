package com.clash.crashrace.board;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Without this, a player who leaves and rejoins mid-event is stuck on the server's default
 * scoreboard - the sidebar is only pushed to whoever is online when the event starts.
 */
public final class ScoreboardJoinListener implements Listener {

    private final RaceScoreboard scoreboard;

    public ScoreboardJoinListener(RaceScoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (scoreboard.isActive()) {
            scoreboard.applyTo(event.getPlayer());
        }
    }
}
