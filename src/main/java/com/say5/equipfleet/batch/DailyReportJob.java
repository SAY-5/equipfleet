package com.say5.equipfleet.batch;

import com.say5.equipfleet.service.ReportingService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that rolls up the previous day's events into daily reports. Runs once per day; the
 * underlying rollup is idempotent, so a missed or repeated run is safe to re-trigger.
 */
@Component
public class DailyReportJob {

  private static final Logger log = LoggerFactory.getLogger(DailyReportJob.class);

  private final ReportingService reportingService;
  private final Clock clock;

  public DailyReportJob(ReportingService reportingService, Clock clock) {
    this.reportingService = reportingService;
    this.clock = clock;
  }

  @Scheduled(cron = "${equipfleet.report-job.cron:0 30 0 * * *}", zone = "UTC")
  public void run() {
    LocalDate yesterday = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1);
    log.info("daily_report_job_start day={}", yesterday);
    reportingService.generateForDay(yesterday);
  }

  /** Backfill a historical day on demand. Regenerates only that day's rows. */
  public void backfill(LocalDate day) {
    log.info("daily_report_job_backfill day={}", day);
    reportingService.generateForDay(day);
  }
}
