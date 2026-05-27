package com.say5.equipfleet.web;

import com.say5.equipfleet.batch.DailyReportJob;
import com.say5.equipfleet.service.ReportingService;
import com.say5.equipfleet.web.Dtos.ReportResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
class ReportController {

  private final ReportingService reportingService;
  private final DailyReportJob reportJob;

  ReportController(ReportingService reportingService, DailyReportJob reportJob) {
    this.reportingService = reportingService;
    this.reportJob = reportJob;
  }

  @GetMapping("/{date}")
  List<ReportResponse> forDay(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return reportingService.reportsForDay(date).stream().map(ReportResponse::from).toList();
  }

  @PostMapping("/{date}/backfill")
  ResponseEntity<List<ReportResponse>> backfill(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    reportJob.backfill(date);
    List<ReportResponse> rows =
        reportingService.reportsForDay(date).stream().map(ReportResponse::from).toList();
    return ResponseEntity.ok(rows);
  }
}
