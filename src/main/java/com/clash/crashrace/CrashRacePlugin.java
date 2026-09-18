package com.clash.crashrace;

import com.clash.crashrace.board.ScoreboardJoinListener;
import com.clash.crashrace.commands.CrashRaceCommand;
import com.clash.crashrace.config.PluginConfig;
import com.clash.crashrace.gui.AdminGui;
import com.clash.crashrace.gui.AdminGuiListener;
import com.clash.crashrace.rank.RankingManager;
import com.clash.crashrace.state.EventManager;
import com.clash.crashrace.track.PlacementListener;
import com.clash.crashrace.track.PlacementTracker;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrashRacePlugin extends JavaPlugin {

    private EventManager eventManager;

    @Override
    public void onEnable() {
        PluginConfig config = new PluginConfig(this);
        PlacementTracker tracker = new PlacementTracker();
        RankingManager rankingManager = new RankingManager();
        eventManager = new EventManager(this, config, tracker, rankingManager);
        AdminGui adminGui = new AdminGui(this, eventManager, config, rankingManager);

        getServer().getPluginManager().registerEvents(new PlacementListener(eventManager, tracker), this);
        getServer().getPluginManager().registerEvents(new AdminGuiListener(adminGui), this);
        getServer().getPluginManager().registerEvents(new ScoreboardJoinListener(eventManager.scoreboard()), this);

        CrashRaceCommand command = new CrashRaceCommand(eventManager, config, adminGui);
        var pluginCommand = getCommand("crashrace");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("CrashRace が有効になりました。");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.reset();
        }
    }
}
