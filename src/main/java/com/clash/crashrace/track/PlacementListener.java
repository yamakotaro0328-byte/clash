package com.clash.crashrace.track;

import com.clash.crashrace.state.EventManager;
import com.clash.crashrace.state.EventState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;

/**
 * Logs every block/entity a player places while the event is running, so a hang can later be
 * traced back to a chunk and player using only this pre-collected data.
 */
public final class PlacementListener implements Listener {

    private final EventManager eventManager;
    private final PlacementTracker tracker;

    public PlacementListener(EventManager eventManager, PlacementTracker tracker) {
        this.eventManager = eventManager;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (eventManager.state() != EventState.RUNNING) {
            return;
        }
        ChunkKey chunk = ChunkKey.of(event.getBlock().getChunk());
        PlacementRecord record = new PlacementRecord(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                event.getBlockPlaced().getType().name(),
                PlacementKind.BLOCK,
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(),
                System.currentTimeMillis());
        tracker.record(chunk, record);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (eventManager.state() != EventState.RUNNING) {
            return;
        }
        if (event.getPlayer() == null) {
            return;
        }
        var loc = event.getEntity().getLocation();
        ChunkKey chunk = ChunkKey.of(loc.getChunk());
        PlacementRecord record = new PlacementRecord(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                event.getEntity().getType().name(),
                PlacementKind.ENTITY,
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                System.currentTimeMillis());
        tracker.record(chunk, record);
    }
}
