package com.clash.crashrace.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class AdminGuiListener implements Listener {

    private final AdminGui adminGui;

    public AdminGuiListener(AdminGui adminGui) {
        this.adminGui = adminGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof AdminGuiHolder)) {
            return;
        }
        boolean shouldRefresh = adminGui.handleClick(player, event.getSlot());
        if (shouldRefresh) {
            adminGui.refresh(event.getInventory());
        }
    }
}
