package com.say5.equipfleet.sim;

import com.say5.equipfleet.batch.DailyReportJob;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * On startup, when {@code equipfleet.seed.enabled} is true, generates a simulated fleet and rolls
 * up its reports so a fresh instance has data to serve. Disabled by default and in tests.
 */
@Component
@ConditionalOnProperty(prefix = "equipfleet.seed", name = "enabled", havingValue = "true")
public class SeedRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

  private final FleetSimulator simulator;
  private final DailyReportJob reportJob;
  private final Clock clock;
  private final SeedProperties properties;

  public SeedRunner(
      FleetSimulator simulator, DailyReportJob reportJob, Clock clock, SeedProperties properties) {
    this.simulator = simulator;
    this.reportJob = reportJob;
    this.clock = clock;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
    var windowStart = today.minusDays(properties.days()).atStartOfDay(ZoneOffset.UTC).toInstant();
    simulator.generate(
        properties.assets(), windowStart, properties.days(), properties.changesPerDay(), 42L);
    for (int d = properties.days(); d >= 1; d--) {
      reportJob.backfill(today.minusDays(d));
    }
    log.info("seed_complete assets={} days={}", properties.assets(), properties.days());
  }
}
