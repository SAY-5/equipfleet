package com.say5.equipfleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.say5.equipfleet.AbstractPostgresIT;
import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.sim.FleetSimulator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EquipmentServiceIT extends AbstractPostgresIT {

  @Autowired EquipmentService equipmentService;
  @Autowired FleetSimulator simulator;

  @Test
  void registersAndRecordsHistory() {
    Equipment asset = equipmentService.register("crane", "plant-b");
    assertThat(asset.getId()).isNotNull();
    assertThat(asset.getStatus()).isEqualTo(EquipmentStatus.IDLE);

    Instant t = Instant.parse("2026-02-01T08:00:00Z");
    equipmentService.recordEvent(asset.getId(), EquipmentStatus.IN_USE, t);
    equipmentService.recordEvent(asset.getId(), EquipmentStatus.DOWN, t.plusSeconds(3600));

    assertThat(equipmentService.history(asset.getId())).hasSize(2);
    assertThat(equipmentService.get(asset.getId()).getStatus()).isEqualTo(EquipmentStatus.DOWN);
  }

  @Test
  void getMissingThrows() {
    assertThatThrownBy(() -> equipmentService.get(999_999L)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void simulatorGeneratesFleetAndEvents() {
    Instant start = Instant.parse("2026-04-01T00:00:00Z");
    List<Equipment> assets = simulator.generate(3, start, 1, 5, 7L);
    assertThat(assets).hasSize(3);
    for (Equipment a : assets) {
      assertThat(equipmentService.history(a.getId())).isNotEmpty();
    }
  }
}
