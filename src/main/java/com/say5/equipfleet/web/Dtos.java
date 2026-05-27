package com.say5.equipfleet.web;

import com.say5.equipfleet.domain.DailyReport;
import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.domain.UsageEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Request and response payloads for the REST API. */
public final class Dtos {

  private Dtos() {}

  public record RegisterEquipmentRequest(
      @NotBlank @Size(max = 255) String type, @NotBlank @Size(max = 255) String site) {}

  public record EquipmentResponse(
      Long id, String type, String site, EquipmentStatus status, Instant registeredAt) {
    public static EquipmentResponse from(Equipment e) {
      return new EquipmentResponse(
          e.getId(), e.getType(), e.getSite(), e.getStatus(), e.getRegisteredAt());
    }
  }

  public record RecordEventRequest(@NotNull EquipmentStatus status, @NotNull Instant occurredAt) {}

  public record EventResponse(
      Long id, Long equipmentId, EquipmentStatus status, Instant occurredAt) {
    public static EventResponse from(UsageEvent e) {
      return new EventResponse(e.getId(), e.getEquipmentId(), e.getStatus(), e.getOccurredAt());
    }
  }

  public record ReportResponse(
      LocalDate reportDate,
      Long equipmentId,
      String scope,
      double utilization,
      double uptime,
      Instant computedAt) {
    public static ReportResponse from(DailyReport r) {
      return new ReportResponse(
          r.getReportDate(),
          r.getEquipmentId(),
          r.getScopeKey(),
          r.getUtilization(),
          r.getUptime(),
          r.getComputedAt());
    }
  }
}
