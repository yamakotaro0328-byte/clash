package com.clash.crashrace.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RankingManager {

    public static final int MAX_RANKS = 3;

    private final List<RankEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * Adds a new entry as the next open rank (1..3). Returns empty if ranking is already full.
     * Safe to call from any thread.
     */
    public synchronized RankEntry addIfRoom(UUID playerId, String playerName, com.clash.crashrace.track.ChunkKey chunk,
                                             double score, long detectedAtMs) {
        if (entries.size() >= MAX_RANKS) {
            return null;
        }
        int nextRank = entries.size() + 1;
        RankEntry entry = new RankEntry(nextRank, playerId, playerName, chunk, score, detectedAtMs);
        entries.add(entry);
        return entry;
    }

    public boolean hasPlayer(UUID playerId) {
        for (RankEntry entry : entries) {
            if (entry.playerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        return entries.size() >= MAX_RANKS;
    }

    public List<RankEntry> list() {
        List<RankEntry> copy = new ArrayList<>(entries);
        copy.sort((a, b) -> Integer.compare(a.rank(), b.rank()));
        return Collections.unmodifiableList(copy);
    }

    public synchronized boolean removeRank(int rank) {
        RankEntry toRemove = null;
        for (RankEntry entry : entries) {
            if (entry.rank() == rank) {
                toRemove = entry;
                break;
            }
        }
        if (toRemove == null) {
            return false;
        }
        entries.remove(toRemove);
        // Re-number remaining entries so ranks stay contiguous starting at 1.
        List<RankEntry> resorted = new ArrayList<>(entries);
        resorted.sort((a, b) -> Integer.compare(a.rank(), b.rank()));
        entries.clear();
        int rankCounter = 1;
        for (RankEntry entry : resorted) {
            entries.add(new RankEntry(rankCounter++, entry.playerId(), entry.playerName(), entry.chunk(),
                    entry.score(), entry.detectedAtMs()));
        }
        return true;
    }

    public void clear() {
        entries.clear();
    }
}
