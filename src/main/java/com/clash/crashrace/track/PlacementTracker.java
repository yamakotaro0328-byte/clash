package com.clash.crashrace.track;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Records block placements per chunk during an event, so a culprit chunk/player
 * can be guessed from pre-collected data alone once the main thread hangs.
 * All methods are safe to call from any thread (plain data structure access only).
 */
public final class PlacementTracker {

    private final Map<ChunkKey, ConcurrentLinkedDeque<PlacementRecord>> byChunk = new ConcurrentHashMap<>();

    public void record(ChunkKey chunk, PlacementRecord record) {
        byChunk.computeIfAbsent(chunk, k -> new ConcurrentLinkedDeque<>()).addLast(record);
    }

    /** Drops records older than maxAgeMs, across all chunks. Call periodically from the main thread. */
    public void pruneOlderThan(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        for (Map.Entry<ChunkKey, ConcurrentLinkedDeque<PlacementRecord>> entry : byChunk.entrySet()) {
            ConcurrentLinkedDeque<PlacementRecord> deque = entry.getValue();
            PlacementRecord head;
            while ((head = deque.peekFirst()) != null && head.timestampMs() < cutoff) {
                deque.pollFirst();
            }
            if (deque.isEmpty()) {
                byChunk.remove(entry.getKey(), deque);
            }
        }
    }

    public Map<ChunkKey, ConcurrentLinkedDeque<PlacementRecord>> snapshot() {
        return byChunk;
    }

    public void clear() {
        byChunk.clear();
    }
}
