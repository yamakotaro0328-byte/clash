package com.clash.crashrace.track;

import java.util.UUID;

public record PlacementRecord(UUID playerId, String playerName, String materialName, long timestampMs) {
}
