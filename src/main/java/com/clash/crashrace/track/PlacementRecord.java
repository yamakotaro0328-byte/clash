package com.clash.crashrace.track;

import java.util.UUID;

public record PlacementRecord(UUID playerId, String playerName, String materialName, PlacementKind kind,
                               int x, int y, int z, long timestampMs) {
}
