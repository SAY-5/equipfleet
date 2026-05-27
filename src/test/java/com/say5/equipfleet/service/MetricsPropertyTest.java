package com.say5.equipfleet.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.IntervalMetricsCalculator.Segment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Property checks over randomized event sets for the interval metrics calculator. */
class MetricsPropertyTest {

  private static final Instant DAY_START = Instant.parse("2026-05-01T00:00:00Z");
  private static final Instant DAY_END = Instant.parse("2026-05-02T00:00:00Z");

  @Test
  void utilizationAndUptimeStayWithinUnitRange() {
    Random random = new Random(1234);
    EquipmentStatus[] statuses = EquipmentStatus.values();
    for (int trial = 0; trial < 500; trial++) {
      List<Segment> segments = new ArrayList<>();
      int count = random.nextInt(20);
      for (int i = 0; i < count; i++) {
        long offset = (long) (random.nextDouble() * 86_400_000L);
        segments.add(
            new Segment(statuses[random.nextInt(statuses.length)], DAY_START.plusMillis(offset)));
      }
      EquipmentStatus carryIn =
          random.nextBoolean() ? statuses[random.nextInt(statuses.length)] : null;

      MetricResult result =
          IntervalMetricsCalculator.compute(carryIn, segments, DAY_START, DAY_END);

      assertThat(result.utilization()).isBetween(0.0, 1.0);
      assertThat(result.uptime()).isBetween(0.0, 1.0);
    }
  }

  @Test
  void downAllDayReportsZeroUptime() {
    MetricResult result =
        IntervalMetricsCalculator.compute(EquipmentStatus.DOWN, List.of(), DAY_START, DAY_END);

    assertThat(result.uptime()).isEqualTo(0.0);
    assertThat(result.utilization()).isEqualTo(0.0);
  }

  @Test
  void eventsOutsideTheDayDoNotAffectIt() {
    Instant priorDay = DAY_START.minusSeconds(7200);
    Instant nextDay = DAY_END.plusSeconds(7200);

    MetricResult withOutside =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.IN_USE,
            List.of(
                new Segment(EquipmentStatus.DOWN, priorDay),
                new Segment(EquipmentStatus.IDLE, nextDay)),
            DAY_START,
            DAY_END);

    // The prior-day DOWN event sets the carry-over status; the next-day event is ignored.
    assertThat(withOutside.utilization()).isEqualTo(0.0);
    assertThat(withOutside.uptime()).isEqualTo(0.0);
  }

  @Test
  void resultIsDeterministicForAFixedEventSet() {
    List<Segment> segments =
        List.of(
            new Segment(EquipmentStatus.IN_USE, Instant.parse("2026-05-01T02:00:00Z")),
            new Segment(EquipmentStatus.DOWN, Instant.parse("2026-05-01T10:00:00Z")),
            new Segment(EquipmentStatus.IDLE, Instant.parse("2026-05-01T18:00:00Z")));

    MetricResult first =
        IntervalMetricsCalculator.compute(EquipmentStatus.IDLE, segments, DAY_START, DAY_END);
    MetricResult second =
        IntervalMetricsCalculator.compute(EquipmentStatus.IDLE, segments, DAY_START, DAY_END);

    assertThat(first).isEqualTo(second);
  }
}
