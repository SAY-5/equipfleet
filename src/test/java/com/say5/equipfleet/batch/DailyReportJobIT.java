package com.say5.equipfleet.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.say5.equipfleet.AbstractPostgresIT;
import com.say5.equipfleet.domain.DailyReport;
import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.EquipmentService;
import com.say5.equipfleet.service.ReportingService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DailyReportJobIT extends AbstractPostgresIT {

  @Autowired DailyReportJob job;
  @Autowired EquipmentService equipmentService;
  @Autowired ReportingService reportingService;

  @Test
  void scheduledRunProducesReportsForYesterday() {
    job.run();
    LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
    assertThat(reportingService.reportsForDay(yesterday)).isNotNull();
  }

  @Test
  void backfillProducesReportForHistoricalDay() {
    Equipment asset = equipmentService.register("press", "plant-a");
    LocalDate day = LocalDate.parse("2026-01-15");
    Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
    equipmentService.recordEvent(asset.getId(), EquipmentStatus.IN_USE, start);

    job.backfill(day);

    List<DailyReport> rows = reportingService.reportsForDay(day);
    assertThat(rows).anyMatch(r -> "FLEET".equals(r.getScopeKey()));
  }
}
