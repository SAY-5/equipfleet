package com.say5.equipfleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.IntervalMetricsCalculator.Segment;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntervalMetricsCalculatorTest {

  private static final Instant DAY_START = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant DAY_END = Instant.parse("2026-01-02T00:00:00Z");

  @Test
  void inUseAllDayReportsFullUtilizationAndUptime() {
    MetricResult result =
        IntervalMetricsCalculator.compute(EquipmentStatus.IN_USE, List.of(), DAY_START, DAY_END);

    assertThat(result.utilization()).isEqualTo(1.0);
    assertThat(result.uptime()).isEqualTo(1.0);
  }

  @Test
  void halfUseHalfIdleGivesHalfUtilizationFullUptime() {
    Instant noon = Instant.parse("2026-01-01T12:00:00Z");
    MetricResult result =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.IN_USE,
            List.of(new Segment(EquipmentStatus.IDLE, noon)),
            DAY_START,
            DAY_END);

    assertThat(result.utilization()).isCloseTo(0.5, within(1e-9));
    assertThat(result.uptime()).isEqualTo(1.0);
  }

  @Test
  void downQuarterDayDropsUptime() {
    Instant sixAm = Instant.parse("2026-01-01T06:00:00Z");
    MetricResult result =
        IntervalMetricsCalculator.compute(
            EquipmentStatus.DOWN,
            List.of(new Segment(EquipmentStatus.IN_USE, sixAm)),
            DAY_START,
            DAY_END);

    assertThat(result.uptime()).isCloseTo(0.75, within(1e-9));
    assertThat(result.utilization()).isCloseTo(0.75, within(1e-9));
  }
}
