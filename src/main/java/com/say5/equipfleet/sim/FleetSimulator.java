package com.say5.equipfleet.sim;

import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.EquipmentService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Generates a simulated fleet and a realistic stream of status changes so the platform has data to
 * report on. The data is synthetic and seeded for repeatability; it is not from real equipment.
 */
@Component
public class FleetSimulator {

  private static final Logger log = LoggerFactory.getLogger(FleetSimulator.class);
  private static final List<String> TYPES =
      List.of("forklift", "crane", "loader", "conveyor", "press");
  private static final List<String> SITES = List.of("plant-a", "plant-b", "yard-1");

  private final EquipmentService equipmentService;

  public FleetSimulator(EquipmentService equipmentService) {
    this.equipmentService = equipmentService;
  }

  /**
   * Registers {@code assetCount} assets and emits status changes across the window starting at
   * {@code windowStart} for {@code days} days, roughly {@code changesPerDay} changes per asset per
   * day. A fixed {@code seed} makes the stream reproducible.
   */
  public List<Equipment> generate(
      int assetCount, Instant windowStart, int days, int changesPerDay, long seed) {
    Random random = new Random(seed);
    Instant windowEnd = windowStart.plus(Duration.ofDays(days));

    List<Equipment> assets = new java.util.ArrayList<>();
    for (int i = 0; i < assetCount; i++) {
      String type = TYPES.get(random.nextInt(TYPES.size()));
      String site = SITES.get(random.nextInt(SITES.size()));
      assets.add(equipmentService.register(type, site));
    }

    long totalMillis = Duration.between(windowStart, windowEnd).toMillis();
    int changesPerAsset = Math.max(1, days * changesPerDay);
    int emitted = 0;
    for (Equipment asset : assets) {
      for (int c = 0; c < changesPerAsset; c++) {
        long offset = (long) (random.nextDouble() * totalMillis);
        Instant when = windowStart.plusMillis(offset);
        EquipmentStatus status = drawStatus(random);
        equipmentService.recordEvent(asset.getId(), status, when);
        emitted++;
      }
    }
    log.info("fleet_generated assets={} events={}", assets.size(), emitted);
    return assets;
  }

  private EquipmentStatus drawStatus(Random random) {
    double r = random.nextDouble();
    if (r < 0.6) {
      return EquipmentStatus.IN_USE;
    }
    if (r < 0.9) {
      return EquipmentStatus.IDLE;
    }
    return EquipmentStatus.DOWN;
  }
}
