package com.clash.crashrace.commands;

import com.clash.crashrace.config.PluginConfig;
import com.clash.crashrace.gui.AdminGui;
import com.clash.crashrace.state.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CrashRaceCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("start", "stop", "extend", "reset", "admin");

    private final EventManager eventManager;
    private final PluginConfig config;
    private final AdminGui adminGui;

    public CrashRaceCommand(EventManager eventManager, PluginConfig config, AdminGui adminGui) {
        this.eventManager = eventManager;
        this.config = config;
        this.adminGui = adminGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(usage());
            return true;
        }
        if (!sender.hasPermission("crashrace.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }

        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "start" -> {
                int minutes = config.defaultDurationMinutes();
                if (args.length >= 2) {
                    minutes = parseIntOrDefault(args[1], minutes);
                }
                eventManager.start(minutes);
                return true;
            }
            case "stop" -> {
                eventManager.stop();
                return true;
            }
            case "extend" -> {
                int minutes = config.extendStepMinutes();
                if (args.length >= 2) {
                    minutes = parseIntOrDefault(args[1], minutes);
                }
                eventManager.extend(minutes);
                return true;
            }
            case "reset" -> {
                eventManager.reset();
                sender.sendMessage(ChatColor.GRAY + "[CrashRace] リセットしました。");
                return true;
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "このコマンドはゲーム内でのみ使用できます。");
                    return true;
                }
                adminGui.open(player);
                return true;
            }
            default -> {
                sender.sendMessage(usage());
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        return List.of();
    }

    private int parseIntOrDefault(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String usage() {
        return ChatColor.RED + "使い方: " + ChatColor.WHITE + "/crashrace <start|stop|extend|reset|admin>";
    }
}
