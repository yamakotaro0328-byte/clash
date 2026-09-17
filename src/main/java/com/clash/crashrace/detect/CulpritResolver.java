package com.clash.crashrace.detect;

import com.clash.crashrace.track.ChunkKey;
import com.clash.crashrace.track.PlacementKind;
import com.clash.crashrace.track.PlacementRecord;
import com.clash.crashrace.track.PlacementTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

/**
 * Heuristically guesses which chunk (and which player) caused a main-thread hang,
 * using only pre-collected block placement data. This is a best-effort guess, not proof:
 * it assumes the chunk with the most recent, heaviest placement activity is responsible.
 *
 * Safe to call from a non-main thread: touches only thread-safe collections, no Bukkit API.
 */
public final class CulpritResolver {

    private final PlacementTracker tracker;
    private final Map<String, Integer> weights;
    private final int suspectWindowSeconds;

    public CulpritResolver(PlacementTracker tracker, Map<String, Integer> weights, int suspectWindowSeconds) {
        this.tracker = tracker;
        this.weights = weights;
        this.suspectWindowSeconds = suspectWindowSeconds;
    }

    /**
     * @param alreadyRanked predicate returning true if a player already holds a rank and should be skipped
     */
    public ResolvedCulprit resolve(Predicate<UUID> alreadyRanked) {
        long cutoff = System.currentTimeMillis() - suspectWindowSeconds * 1000L;

        record ChunkScore(ChunkKey chunk, double total, Map<UUID, Double> perPlayer, Map<UUID, String> names,
                          List<PlacementRecord> recentRecords) {
        }

        List<ChunkScore> chunkScores = new ArrayList<>();

        for (Map.Entry<ChunkKey, ConcurrentLinkedDeque<PlacementRecord>> entry : tracker.snapshot().entrySet()) {
            Map<UUID, Double> perPlayer = new HashMap<>();
            Map<UUID, String> names = new HashMap<>();
            List<PlacementRecord> recentRecords = new ArrayList<>();
            double total = 0;
            for (PlacementRecord record : entry.getValue()) {
                if (record.timestampMs() < cutoff) {
                    continue;
                }
                int weight = weights.getOrDefault(record.materialName(), weights.getOrDefault("DEFAULT", 1));
                perPlayer.merge(record.playerId(), (double) weight, Double::sum);
                names.putIfAbsent(record.playerId(), record.playerName());
                recentRecords.add(record);
                total += weight;
            }
            if (total > 0) {
                chunkScores.add(new ChunkScore(entry.getKey(), total, perPlayer, names, recentRecords));
            }
        }

        chunkScores.sort((a, b) -> Double.compare(b.total(), a.total()));

        for (ChunkScore chunkScore : chunkScores) {
            UUID bestPlayer = null;
            double bestScore = -1;
            for (Map.Entry<UUID, Double> playerScore : chunkScore.perPlayer().entrySet()) {
                if (alreadyRanked.test(playerScore.getKey())) {
                    continue;
                }
                if (playerScore.getValue() > bestScore) {
                    bestScore = playerScore.getValue();
                    bestPlayer = playerScore.getKey();
                }
            }
            if (bestPlayer != null) {
                List<BlockTarget> weightedBlocks = new ArrayList<>();
                for (PlacementRecord record : chunkScore.recentRecords()) {
                    if (record.kind() == PlacementKind.BLOCK && weights.containsKey(record.materialName())) {
                        weightedBlocks.add(new BlockTarget(record.x(), record.y(), record.z()));
                    }
                }
                return new ResolvedCulprit(chunkScore.chunk(), bestPlayer, chunkScore.names().get(bestPlayer),
                        bestScore, weightedBlocks);
            }
        }
        return null;
    }
}
