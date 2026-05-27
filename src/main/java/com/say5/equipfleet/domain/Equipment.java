package com.say5.equipfleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** An equipment asset tracked in the fleet. */
@Entity
@Table(name = "equipment")
public class Equipment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String site;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EquipmentStatus status;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  protected Equipment() {}

  public Equipment(String type, String site, EquipmentStatus status, Instant registeredAt) {
    this.type = type;
    this.site = site;
    this.status = status;
    this.registeredAt = registeredAt;
  }

  public Long getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getSite() {
    return site;
  }

  public EquipmentStatus getStatus() {
    return status;
  }

  public void setStatus(EquipmentStatus status) {
    this.status = status;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }
}
