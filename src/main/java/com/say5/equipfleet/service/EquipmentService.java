package com.say5.equipfleet.service;

import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.domain.UsageEvent;
import com.say5.equipfleet.repository.EquipmentRepository;
import com.say5.equipfleet.repository.UsageEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages equipment assets and their usage event history. */
@Service
public class EquipmentService {

  private final EquipmentRepository equipmentRepository;
  private final UsageEventRepository eventRepository;
  private final Clock clock;

  public EquipmentService(
      EquipmentRepository equipmentRepository, UsageEventRepository eventRepository, Clock clock) {
    this.equipmentRepository = equipmentRepository;
    this.eventRepository = eventRepository;
    this.clock = clock;
  }

  @Transactional
  public Equipment register(String type, String site) {
    Equipment equipment = new Equipment(type, site, EquipmentStatus.IDLE, Instant.now(clock));
    return equipmentRepository.save(equipment);
  }

  @Transactional(readOnly = true)
  public List<Equipment> list() {
    return equipmentRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Equipment get(Long id) {
    return equipmentRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("equipment not found: " + id));
  }

  @Transactional
  public UsageEvent recordEvent(Long equipmentId, EquipmentStatus status, Instant occurredAt) {
    Equipment equipment = get(equipmentId);
    UsageEvent event = eventRepository.save(new UsageEvent(equipmentId, status, occurredAt));
    equipment.setStatus(status);
    return event;
  }

  @Transactional(readOnly = true)
  public List<UsageEvent> history(Long equipmentId) {
    get(equipmentId);
    return eventRepository.findByEquipmentIdOrderByOccurredAtAsc(equipmentId);
  }
}
