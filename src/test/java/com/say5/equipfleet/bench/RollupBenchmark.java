package com.say5.equipfleet.bench;

import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.IntervalMetricsCalculator;
import com.say5.equipfleet.service.IntervalMetricsCalculator.Segment;
import com.say5.equipfleet.service.MetricResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Measures the throughput of the rollup core over a simulated fleet month. Builds a fixed event
 * volume in memory, then times repeated per-asset-per-day metric computation. Prints one line of
 * measured numbers; a CI smoke gate parses it and compares against a recorded baseline.
 *
 * <p>This benchmark exercises the CPU-bound calculation only, so it runs without a database and is
 * repeatable. The event data is simulated.
 */
public final class RollupBenchmark {

  private static final int ASSETS = 200;
  private static final int DAYS = 30;
  private static final int CHANGES_PER_ASSET_PER_DAY = 24;
  private static final long SEED = 99L;

  private RollupBenchmark() {}

  public static void main(String[] args) {
    Instant monthStart = Instant.parse("2026-01-01T00:00:00Z");
    Dataset dataset = buildDataset(monthStart);
    int rollups = ASSETS * DAYS;

    // Warmup.
    for (int i = 0; i < 3; i++) {
      runOnce(dataset, monthStart);
    }

    int iterations = 5;
    long bestNanos = Long.MAX_VALUE;
    long total = 0;
    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      double checksum = runOnce(dataset, monthStart);
      long elapsed = System.nanoTime() - start;
      bestNanos = Math.min(bestNanos, elapsed);
      total += elapsed;
      if (Double.isNaN(checksum)) {
        throw new IllegalStateException("benchmark produced NaN");
      }
    }

    double bestMs = bestNanos / 1_000_000.0;
    double rollupsPerSec = rollups / (bestNanos / 1_000_000_000.0);
    long totalEvents = (long) ASSETS * DAYS * CHANGES_PER_ASSET_PER_DAY;
    System.out.printf(
        "BENCH events=%d rollups=%d iterations=%d best_ms=%.3f rollups_per_sec=%.1f%n",
        totalEvents, rollups, iterations, bestMs, rollupsPerSec);
  }

  private static double runOnce(Dataset dataset, Instant monthStart) {
    double checksum = 0;
    for (int asset = 0; asset < ASSETS; asset++) {
      List<Segment> events = dataset.eventsByAsset.get(asset);
      for (int day = 0; day < DAYS; day++) {
        Instant windowStart = monthStart.plus(day, ChronoUnit.DAYS);
        Instant windowEnd = windowStart.plus(1, ChronoUnit.DAYS);
        MetricResult result =
            IntervalMetricsCalculator.compute(EquipmentStatus.IDLE, events, windowStart, windowEnd);
        checksum += result.utilization() + result.uptime();
      }
    }
    return checksum;
  }

  private static Dataset buildDataset(Instant monthStart) {
    Random random = new Random(SEED);
    EquipmentStatus[] statuses = EquipmentStatus.values();
    long monthMillis = (long) DAYS * 24 * 3600 * 1000;
    Dataset dataset = new Dataset();
    int perAsset = DAYS * CHANGES_PER_ASSET_PER_DAY;
    for (int asset = 0; asset < ASSETS; asset++) {
      List<Segment> events = new ArrayList<>(perAsset);
      for (int i = 0; i < perAsset; i++) {
        long offset = (long) (random.nextDouble() * monthMillis);
        events.add(
            new Segment(statuses[random.nextInt(statuses.length)], monthStart.plusMillis(offset)));
      }
      dataset.eventsByAsset.add(events);
    }
    return dataset;
  }

  private static final class Dataset {
    final List<List<Segment>> eventsByAsset = new ArrayList<>();
  }
}
