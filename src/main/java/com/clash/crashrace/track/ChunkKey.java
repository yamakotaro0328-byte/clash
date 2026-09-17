package com.clash.crashrace.track;

import org.bukkit.Chunk;

public record ChunkKey(String worldName, int chunkX, int chunkZ) {

    public static ChunkKey of(Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public boolean isNear(ChunkKey other, int radius) {
        if (!worldName.equals(other.worldName)) {
            return false;
        }
        return Math.abs(chunkX - other.chunkX) <= radius && Math.abs(chunkZ - other.chunkZ) <= radius;
    }
}
