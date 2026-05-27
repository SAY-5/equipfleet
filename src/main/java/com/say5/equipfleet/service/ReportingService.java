package com.say5.equipfleet.service;

import com.say5.equipfleet.domain.DailyReport;
import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.UsageEvent;
import com.say5.equipfleet.repository.DailyReportRepository;
import com.say5.equipfleet.repository.EquipmentRepository;
import com.say5.equipfleet.repository.UsageEventRepository;
import com.say5.equipfleet.service.IntervalMetricsCalculator.Segment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rolls up usage events into per-asset and fleet-level daily reports. The rollup is idempotent: it
 * upserts each report row keyed on (date, scope), so re-running for a day never double-counts. It
 * is shared by the REST layer and the scheduled batch job.
 */
@Service
public class ReportingService {

  private static final Logger log = LoggerFactory.getLogger(ReportingService.class);
  private static final ZoneOffset DAY_ZONE = ZoneOffset.UTC;

  private final EquipmentRepository equipmentRepository;
  private final UsageEventRepository eventRepository;
  private final DailyReportRepository reportRepository;
  private final Clock clock;

  public ReportingService(
      EquipmentRepository equipmentRepository,
      UsageEventRepository eventRepository,
      DailyReportRepository reportRepository,
      Clock clock) {
    this.equipmentRepository = equipmentRepository;
    this.eventRepository = eventRepository;
    this.reportRepository = reportRepository;
    this.clock = clock;
  }

  /**
   * Generates (or regenerates) all report rows for {@code day}. Per-asset rows are computed from
   * each asset's events, then averaged into a single fleet row. Existing rows for the day are
   * updated in place rather than duplicated.
   *
   * @return the rows persisted for the day, fleet row first.
   */
  @Transactional
  public List<DailyReport> generateForDay(LocalDate day) {
    Instant windowStart = day.atStartOfDay(DAY_ZONE).toInstant();
    Instant windowEnd = day.plusDays(1).atStartOfDay(DAY_ZONE).toInstant();
    Instant now = Instant.now(clock);

    List<Equipment> fleet = equipmentRepository.findAll();
    List<DailyReport> persisted = new ArrayList<>();

    double utilizationSum = 0;
    double uptimeSum = 0;
    int counted = 0;

    for (Equipment equipment : fleet) {
      MetricResult metric = computeForAsset(equipment.getId(), windowStart, windowEnd);
      utilizationSum += metric.utilization();
      uptimeSum += metric.uptime();
      counted++;
      persisted.add(upsert(day, equipment.getId(), metric, now));
    }

    double fleetUtilization = counted == 0 ? 0.0 : utilizationSum / counted;
    double fleetUptime = counted == 0 ? 1.0 : uptimeSum / counted;
    DailyReport fleetRow = upsert(day, null, new MetricResult(fleetUtilization, fleetUptime), now);

    List<DailyReport> result = new ArrayList<>();
    result.add(fleetRow);
    result.addAll(persisted);
    log.info("rollup_complete day={} assets={}", day, counted);
    return result;
  }

  private MetricResult computeForAsset(Long equipmentId, Instant windowStart, Instant windowEnd) {
    UsageEvent carryIn = eventRepository.findLatestBefore(equipmentId, windowStart);
    List<UsageEvent> events = eventRepository.findInWindow(equipmentId, windowStart, windowEnd);
    List<Segment> segments = new ArrayList<>();
    for (UsageEvent e : events) {
      segments.add(new Segment(e.getStatus(), e.getOccurredAt()));
    }
    return IntervalMetricsCalculator.compute(
        carryIn == null ? null : carryIn.getStatus(), segments, windowStart, windowEnd);
  }

  private DailyReport upsert(LocalDate day, Long equipmentId, MetricResult metric, Instant now) {
    String scopeKey = equipmentId == null ? "FLEET" : equipmentId.toString();
    return reportRepository
        .findByReportDateAndScopeKey(day, scopeKey)
        .map(
            existing -> {
              existing.update(metric.utilization(), metric.uptime(), now);
              return reportRepository.save(existing);
            })
        .orElseGet(
            () ->
                reportRepository.save(
                    new DailyReport(day, equipmentId, metric.utilization(), metric.uptime(), now)));
  }

  @Transactional(readOnly = true)
  public List<DailyReport> reportsForDay(LocalDate day) {
    return reportRepository.findByReportDateOrderByScopeKeyAsc(day);
  }
}
