package com.say5.equipfleet.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.say5.equipfleet.AbstractPostgresIT;
import com.say5.equipfleet.domain.DailyReport;
import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.repository.DailyReportRepository;
import com.say5.equipfleet.service.EquipmentService;
import com.say5.equipfleet.service.ReportingService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the rollup is idempotent (re-running a day does not double-count and yields identical
 * rows) and that a backfill regenerates one historical day's numbers without touching other days.
 */
class IdempotencyBackfillIT extends AbstractPostgresIT {

  @Autowired DailyReportJob job;
  @Autowired EquipmentService equipmentService;
  @Autowired ReportingService reportingService;
  @Autowired DailyReportRepository reportRepository;

  @Test
  void runningTheJobTwiceYieldsIdenticalReports() {
    Equipment asset = equipmentService.register("forklift", "plant-a");
    LocalDate day = LocalDate.parse("2026-09-01");
    Instant start = day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    equipmentService.recordEvent(asset.getId(), EquipmentStatus.IN_USE, start);
    equipmentService.recordEvent(
        asset.getId(), EquipmentStatus.DOWN, start.plus(java.time.Duration.ofHours(6)));

    reportingService.generateForDay(day);
    Map<String, List<Double>> first = snapshot(reportingService.reportsForDay(day));
    long countAfterFirst = countForDay(day);

    reportingService.generateForDay(day);
    Map<String, List<Double>> second = snapshot(reportingService.reportsForDay(day));
    long countAfterSecond = countForDay(day);

    assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    assertThat(second).isEqualTo(first);
  }

  @Test
  void backfillRegeneratesOneDayWithoutTouchingOthers() {
    Equipment asset = equipmentService.register("conveyor", "plant-b");
    LocalDate target = LocalDate.parse("2026-09-10");
    LocalDate neighbour = LocalDate.parse("2026-09-11");

    Instant targetStart = target.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    equipmentService.recordEvent(asset.getId(), EquipmentStatus.IN_USE, targetStart);
    Instant neighbourStart = neighbour.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    equipmentService.recordEvent(asset.getId(), EquipmentStatus.DOWN, neighbourStart);

    // Build the neighbour report first and remember it.
    reportingService.generateForDay(neighbour);
    Map<String, List<Double>> neighbourBefore = snapshot(reportingService.reportsForDay(neighbour));

    // Backfill the target day after the fact.
    job.backfill(target);

    double targetUtil = assetUtilization(reportingService.reportsForDay(target), asset.getId());
    // IN_USE all of the target day with no later event before midnight.
    assertThat(targetUtil).isCloseTo(1.0, within(1e-9));

    // The neighbour day is unchanged: it carried in IN_USE and went DOWN at its own start.
    Map<String, List<Double>> neighbourAfter = snapshot(reportingService.reportsForDay(neighbour));
    assertThat(neighbourAfter).isEqualTo(neighbourBefore);
    double neighbourUtil =
        assetUtilization(reportingService.reportsForDay(neighbour), asset.getId());
    assertThat(neighbourUtil).isCloseTo(0.0, within(1e-9));
  }

  private long countForDay(LocalDate day) {
    return reportRepository.findByReportDateOrderByScopeKeyAsc(day).size();
  }

  private Map<String, List<Double>> snapshot(List<DailyReport> rows) {
    return rows.stream()
        .collect(
            Collectors.toMap(
                DailyReport::getScopeKey, r -> List.of(r.getUtilization(), r.getUptime())));
  }

  private double assetUtilization(List<DailyReport> rows, Long equipmentId) {
    return rows.stream()
        .filter(r -> equipmentId.equals(r.getEquipmentId()))
        .findFirst()
        .orElseThrow()
        .getUtilization();
  }
}
