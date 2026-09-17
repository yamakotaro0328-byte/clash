package com.clash.crashrace.detect;

import com.clash.crashrace.track.ChunkKey;

import java.util.List;
import java.util.UUID;

public record ResolvedCulprit(ChunkKey chunk, UUID playerId, String playerName, double score,
                               List<BlockTarget> weightedBlocks) {
}
