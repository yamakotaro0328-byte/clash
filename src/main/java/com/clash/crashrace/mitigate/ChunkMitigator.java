package com.clash.crashrace.mitigate;

import com.clash.crashrace.config.PluginConfig;
import com.clash.crashrace.track.ChunkKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Best-effort mitigation of the chunk suspected of causing a hang: clears non-player
 * entities and cycles the chunk. Must only be called from the main thread (it touches
 * live World/Chunk/Entity state), typically scheduled to run once the main thread frees up.
 * Scoped to the offending area only, so it stays a light complement to any separate
 * server-wide lag-clearing plugin running alongside it during the event.
 */
public final class ChunkMitigator {

    private final JavaPlugin plugin;
    private final PluginConfig config;

    public ChunkMitigator(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void mitigate(ChunkKey center) {
        if (!config.mitigationEnabled()) {
            return;
        }
        World world = Bukkit.getWorld(center.worldName());
        if (world == null) {
            return;
        }
        int radius = config.mitigationChunkRadius();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = center.chunkX() + dx;
                int cz = center.chunkZ() + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                Chunk chunk = world.getChunkAt(cx, cz);
                if (config.mitigationClearEntities()) {
                    clearNonPlayerEntities(chunk);
                }
                if (config.mitigationReloadChunk()) {
                    // Best-effort: succeeds only when no player/plugin ticket is keeping the chunk
                    // loaded. Near players it will usually no-op, which is an acceptable fallback -
                    // entity clearing above is the main lever for relieving tick load.
                    chunk.unload(true);
                }
            }
        }
        plugin.getLogger().info("[CrashRace] Mitigated area around chunk " + center.chunkX() + "," + center.chunkZ()
                + " in world " + center.worldName());
    }

    private void clearNonPlayerEntities(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }
}
