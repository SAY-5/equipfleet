package com.say5.equipfleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.say5.equipfleet.AbstractPostgresIT;
import com.say5.equipfleet.domain.DailyReport;
import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves through the persistence-backed rollup that a status spanning midnight is split across the
 * two days it touches, and that each day's report is computed from only that day's slice.
 */
class CrossDayRollupIT extends AbstractPostgresIT {

  @Autowired EquipmentService equipmentService;
  @Autowired ReportingService reportingService;

  @Test
  void statusSpanningMidnightSplitsAcrossBothDays() {
    Equipment asset = equipmentService.register("crane", "plant-a");
    LocalDate dayOne = LocalDate.parse("2026-08-10");
    LocalDate dayTwo = LocalDate.parse("2026-08-11");

    // IN_USE from 18:00 on day one, then IDLE at 06:00 on day two.
    equipmentService.recordEvent(
        asset.getId(), EquipmentStatus.IN_USE, Instant.parse("2026-08-10T18:00:00Z"));
    equipmentService.recordEvent(
        asset.getId(), EquipmentStatus.IDLE, Instant.parse("2026-08-11T06:00:00Z"));

    reportingService.generateForDay(dayOne);
    reportingService.generateForDay(dayTwo);

    double utilOne = assetUtilization(reportingService.reportsForDay(dayOne), asset.getId());
    double utilTwo = assetUtilization(reportingService.reportsForDay(dayTwo), asset.getId());

    // Day one: IN_USE 18:00-24:00 = 6h of 24h. Day two: IN_USE 00:00-06:00 = 6h of 24h.
    assertThat(utilOne).isCloseTo(6.0 / 24.0, within(1e-9));
    assertThat(utilTwo).isCloseTo(6.0 / 24.0, within(1e-9));
  }

  private double assetUtilization(List<DailyReport> rows, Long equipmentId) {
    return rows.stream()
        .filter(r -> equipmentId.equals(r.getEquipmentId()))
        .findFirst()
        .orElseThrow()
        .getUtilization();
  }
}
