package com.say5.equipfleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A status change for an equipment asset. Each event marks the asset entering a status at a given
 * instant; the status holds until the next event for that asset.
 */
@Entity
@Table(
    name = "usage_event",
    indexes = {
      @Index(name = "idx_usage_event_equipment_ts", columnList = "equipment_id, occurred_at")
    })
public class UsageEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "equipment_id", nullable = false)
  private Long equipmentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EquipmentStatus status;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected UsageEvent() {}

  public UsageEvent(Long equipmentId, EquipmentStatus status, Instant occurredAt) {
    this.equipmentId = equipmentId;
    this.status = status;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public Long getEquipmentId() {
    return equipmentId;
  }

  public EquipmentStatus getStatus() {
    return status;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
