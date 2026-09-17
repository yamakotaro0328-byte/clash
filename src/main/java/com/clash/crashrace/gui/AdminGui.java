package com.clash.crashrace.gui;

import com.clash.crashrace.config.PluginConfig;
import com.clash.crashrace.rank.RankEntry;
import com.clash.crashrace.rank.RankingManager;
import com.clash.crashrace.state.EventManager;
import com.clash.crashrace.state.EventState;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class AdminGui {

    public static final String TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "CrashRace 管理パネル";
    private static final int SIZE = 27;

    public static final int SLOT_START = 10;
    public static final int SLOT_STOP = 11;
    public static final int SLOT_RESET = 12;
    public static final int SLOT_STATUS = 13;
    public static final int SLOT_EXTEND = 14;
    public static final int SLOT_AUTO_END = 15;
    public static final int SLOT_MITIGATION = 16;
    public static final int SLOT_THRESHOLD_MINUS = 19;
    public static final int SLOT_THRESHOLD_DISPLAY = 20;
    public static final int SLOT_THRESHOLD_PLUS = 21;
    public static final int SLOT_RANK1_REMOVE = 23;
    public static final int SLOT_RANK2_REMOVE = 24;
    public static final int SLOT_RANK3_REMOVE = 25;

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private final PluginConfig config;
    private final RankingManager rankingManager;

    public AdminGui(JavaPlugin plugin, EventManager eventManager, PluginConfig config, RankingManager rankingManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.config = config;
        this.rankingManager = rankingManager;
    }

    public void open(Player player) {
        AdminGuiHolder holder = new AdminGuiHolder();
        Inventory inventory = plugin.getServer().createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);
        render(inventory);
        player.openInventory(inventory);
    }

    public void refresh(Inventory inventory) {
        inventory.clear();
        render(inventory);
    }

    private void render(Inventory inventory) {
        inventory.setItem(SLOT_START, item(Material.LIME_WOOL,
                ChatColor.GREEN + "▶ 大会スタート / 再スタート",
                "現在の状態: " + stateLabel(),
                "デフォルト時間: " + config.defaultDurationMinutes() + "分",
                "",
                "クリックでスタートします"));

        inventory.setItem(SLOT_STOP, item(Material.RED_WOOL,
                ChatColor.RED + "■ 大会を今すぐ終了",
                "現在のランキングで確定します"));

        inventory.setItem(SLOT_RESET, item(Material.GRAY_WOOL,
                ChatColor.GRAY + "⟲ リセット",
                "ランキング・ログを消去してIDLEに戻します"));

        List<String> statusLore = new ArrayList<>();
        statusLore.add("状態: " + stateLabel());
        if (eventManager.state() == EventState.RUNNING) {
            statusLore.add("残り時間: " + formatMs(eventManager.remainingMs()));
        }
        statusLore.add("");
        statusLore.add(ChatColor.GOLD + "--- 現在の順位 ---");
        List<RankEntry> entries = rankingManager.list();
        if (entries.isEmpty()) {
            statusLore.add(ChatColor.GRAY + "まだ誰も検知されていません");
        } else {
            for (RankEntry entry : entries) {
                statusLore.add(ChatColor.YELLOW + "" + entry.rank() + "位: " + ChatColor.WHITE + entry.playerName()
                        + ChatColor.GRAY + " (チャンク " + entry.chunk().chunkX() + "," + entry.chunk().chunkZ() + ")");
            }
        }
        inventory.setItem(SLOT_STATUS, item(Material.PAPER, ChatColor.AQUA + "状況表示 (クリックで更新)",
                statusLore.toArray(new String[0])));

        inventory.setItem(SLOT_EXTEND, item(Material.EMERALD,
                ChatColor.GREEN + "+ " + config.extendStepMinutes() + "分延長",
                "実行中のみ有効です"));

        inventory.setItem(SLOT_AUTO_END, item(Material.COMPARATOR,
                ChatColor.YELLOW + "3位まで決定で自動終了",
                "現在: " + onOff(config.autoEndWhenFull()),
                "",
                "クリックで切り替え"));

        inventory.setItem(SLOT_MITIGATION, item(Material.TNT,
                ChatColor.YELLOW + "検知時の自動チャンク掃除",
                "現在: " + onOff(config.mitigationEnabled()),
                "軽量化プラグインと併用可能な",
                "対象チャンク限定の掃除処理です",
                "",
                "クリックで切り替え"));

        inventory.setItem(SLOT_THRESHOLD_MINUS, item(Material.RED_DYE, ChatColor.RED + "- 1秒"));
        inventory.setItem(SLOT_THRESHOLD_DISPLAY, item(Material.CLOCK,
                ChatColor.GOLD + "ハング検知しきい値",
                config.hangThresholdMs() + " ms",
                "",
                ChatColor.GRAY + "サーバーの実際のWatchdogタイムアウトより",
                ChatColor.GRAY + "必ず短く設定してください"));
        inventory.setItem(SLOT_THRESHOLD_PLUS, item(Material.LIME_DYE, ChatColor.GREEN + "+ 1秒"));

        inventory.setItem(SLOT_RANK1_REMOVE, rankRemoveItem(entries, 1));
        inventory.setItem(SLOT_RANK2_REMOVE, rankRemoveItem(entries, 2));
        inventory.setItem(SLOT_RANK3_REMOVE, rankRemoveItem(entries, 3));
    }

    private ItemStack rankRemoveItem(List<RankEntry> entries, int rank) {
        RankEntry match = entries.stream().filter(e -> e.rank() == rank).findFirst().orElse(null);
        if (match == null) {
            return item(Material.GRAY_DYE, ChatColor.GRAY + "" + rank + "位: (未確定)");
        }
        return item(Material.BARRIER, ChatColor.RED + "" + rank + "位 (" + match.playerName() + ") を取り消す");
    }

    /** Handles a click; returns true if the GUI should be re-rendered. */
    public boolean handleClick(Player player, int slot) {
        switch (slot) {
            case SLOT_START -> {
                eventManager.start(config.defaultDurationMinutes());
                feedback(player);
                return true;
            }
            case SLOT_STOP -> {
                eventManager.stop();
                feedback(player);
                return true;
            }
            case SLOT_RESET -> {
                eventManager.reset();
                feedback(player);
                return true;
            }
            case SLOT_STATUS -> {
                return true;
            }
            case SLOT_EXTEND -> {
                eventManager.extend(config.extendStepMinutes());
                feedback(player);
                return true;
            }
            case SLOT_AUTO_END -> {
                config.setAutoEndWhenFull(!config.autoEndWhenFull());
                feedback(player);
                return true;
            }
            case SLOT_MITIGATION -> {
                config.setMitigationEnabled(!config.mitigationEnabled());
                feedback(player);
                return true;
            }
            case SLOT_THRESHOLD_MINUS -> {
                config.setHangThresholdMs(config.hangThresholdMs() - 1000L);
                feedback(player);
                return true;
            }
            case SLOT_THRESHOLD_PLUS -> {
                config.setHangThresholdMs(config.hangThresholdMs() + 1000L);
                feedback(player);
                return true;
            }
            case SLOT_RANK1_REMOVE -> {
                rankingManager.removeRank(1);
                feedback(player);
                return true;
            }
            case SLOT_RANK2_REMOVE -> {
                rankingManager.removeRank(2);
                feedback(player);
                return true;
            }
            case SLOT_RANK3_REMOVE -> {
                rankingManager.removeRank(3);
                feedback(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void feedback(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private String stateLabel() {
        return switch (eventManager.state()) {
            case IDLE -> ChatColor.GRAY + "待機中";
            case RUNNING -> ChatColor.GREEN + "開催中";
            case ENDED -> ChatColor.RED + "終了";
        };
    }

    private String onOff(boolean value) {
        return value ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(List.of(lore));
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
