package com.clash.crashrace.rank;

import com.clash.crashrace.track.ChunkKey;

import java.util.UUID;

public record RankEntry(int rank, UUID playerId, String playerName, ChunkKey chunk, double score, long detectedAtMs) {
}
