package com.say5.equipfleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.IntervalMetricsCalculator.Segment;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Metric correctness on hand-computed scenarios with exactly known expected values: out-of-order
 * and overlapping events, a status spanning midnight, a status still open at day end, and missing
 * or duplicate events. The day window is the 24 hours of 2026-07-01 UTC.
 */
class MetricsCorrectnessTest {

  private static final Instant DAY_START = Instant.parse("2026-07-01T00:00:00Z");
  private static final Instant DAY_END = Instant.parse("2026-07-02T00:00:00Z");
  private static final double EPS = 1e-9;

  private static Instant at(String time) {
    return Instant.parse("2026-07-01T" + time + ":00Z");
  }

  @Test
  void outOfOrderEventsAreSortedBeforeMeasuring() {
    // Given out of order; effective timeline from IDLE carry-in:
    // 00-06 IDLE, 06-12 IN_USE, 12-18 IDLE, 18-24 DOWN.
    List<Segment> events =
        List.of(
            new Segment(EquipmentStatus.DOWN, at("18:00")),
            new Segment(EquipmentStatus.IN_USE, at("06:00")),
            new Segment(EquipmentStatus.IDLE, at("12:00")));

    MetricResult result =
        IntervalMetricsCalculator.compute(EquipmentStatus.IDLE, events, DAY_START, DAY_END);

    // IN_USE for 6h of 24h, DOWN for 6h of 24h.
    assertThat(result.utilization()).isCloseTo(0.25, within(EPS));
    assertThat(result.uptime()).isCloseTo(0.75, within(EPS));
  }

  @Test
  void overlappingEventsAtSameInstantTakeTheLaterDeclaration() {
    // Two events at the same instant. Sorting is stable on the input order, so the second listed
    // event wins as the status held from that instant. Here both lead to IN_USE from 06:00.
    List<Segment> events =
        List.of(
            new Segment(EquipmentStatus.DOWN, at("06:00")),
            new Segment(EquipmentStatus.IN_USE, at("06:00")));

    MetricResult result =
        IntervalMetricsCalculator.compute(EquipmentStatus.IDLE, events, DAY_START, DAY_END);

    // IDLE 00-06, then IN_USE 06-24 = 18h of 24h in use, nothing down.
    assertThat(result.utilization()).isCloseTo(0.75, within(EPS));
    assertThat(result.uptime()).isCloseTo(1.0, within(EPS));
  }

  @Test
  void statusSpanningMidnightCountsOnlyTheInWindowPortion() {
    // A DOWN entered before the window (prior evening) carries in and holds until 03:00,
    // then IN_USE runs past the window end into the next day.
    MetricResult result =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.DOWN,
            List.of(new Segment(EquipmentStatus.IN_USE, at("03:00"))),
            DAY_START,
            DAY_END);

    // DOWN 00-03 (3h), IN_USE 03-24 (21h). Only this day's portion counts.
    assertThat(result.utilization()).isCloseTo(21.0 / 24.0, within(EPS));
    assertThat(result.uptime()).isCloseTo(21.0 / 24.0, within(EPS));
  }

  @Test
  void statusStillOpenAtDayEndHoldsUntilTheBoundary() {
    // IN_USE entered at noon with no later event holds to the day boundary.
    MetricResult result =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.IDLE,
            List.of(new Segment(EquipmentStatus.IN_USE, at("12:00"))),
            DAY_START,
            DAY_END);

    assertThat(result.utilization()).isCloseTo(0.5, within(EPS));
    assertThat(result.uptime()).isCloseTo(1.0, within(EPS));
  }

  @Test
  void duplicateEventsDoNotChangeTheResult() {
    MetricResult single =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.IDLE,
            List.of(new Segment(EquipmentStatus.IN_USE, at("06:00"))),
            DAY_START,
            DAY_END);

    MetricResult duplicated =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.IDLE,
            List.of(
                new Segment(EquipmentStatus.IN_USE, at("06:00")),
                new Segment(EquipmentStatus.IN_USE, at("06:00"))),
            DAY_START,
            DAY_END);

    assertThat(duplicated).isEqualTo(single);
    assertThat(single.utilization()).isCloseTo(0.75, within(EPS));
  }

  @Test
  void missingEventsFallBackToCarryInForTheWholeDay() {
    MetricResult inUse =
        IntervalMetricsCalculator.compute(EquipmentStatus.IN_USE, List.of(), DAY_START, DAY_END);
    assertThat(inUse.utilization()).isCloseTo(1.0, within(EPS));

    MetricResult unknown = IntervalMetricsCalculator.compute(null, List.of(), DAY_START, DAY_END);
    // Unknown prior state is treated as IDLE: not in use, not down.
    assertThat(unknown.utilization()).isCloseTo(0.0, within(EPS));
    assertThat(unknown.uptime()).isCloseTo(1.0, within(EPS));
  }
}
