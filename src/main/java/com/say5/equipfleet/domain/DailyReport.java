package com.say5.equipfleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A derived daily report row. {@code equipmentId} null marks the fleet-level aggregate for the day.
 */
@Entity
@Table(
    name = "daily_report",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_daily_report_day_scope",
            columnNames = {"report_date", "scope_key"}))
public class DailyReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "report_date", nullable = false)
  private LocalDate reportDate;

  @Column(name = "equipment_id")
  private Long equipmentId;

  /** Stable key for the uniqueness constraint: equipment id as text, or "FLEET". */
  @Column(name = "scope_key", nullable = false)
  private String scopeKey;

  @Column(nullable = false)
  private double utilization;

  @Column(nullable = false)
  private double uptime;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;

  protected DailyReport() {}

  public DailyReport(
      LocalDate reportDate,
      Long equipmentId,
      double utilization,
      double uptime,
      Instant computedAt) {
    this.reportDate = reportDate;
    this.equipmentId = equipmentId;
    this.scopeKey = equipmentId == null ? "FLEET" : equipmentId.toString();
    this.utilization = utilization;
    this.uptime = uptime;
    this.computedAt = computedAt;
  }

  public Long getId() {
    return id;
  }

  public LocalDate getReportDate() {
    return reportDate;
  }

  public Long getEquipmentId() {
    return equipmentId;
  }

  public String getScopeKey() {
    return scopeKey;
  }

  public double getUtilization() {
    return utilization;
  }

  public double getUptime() {
    return uptime;
  }

  public Instant getComputedAt() {
    return computedAt;
  }

  public void update(double newUtilization, double newUptime, Instant when) {
    this.utilization = newUtilization;
    this.uptime = newUptime;
    this.computedAt = when;
  }
}
