package com.say5.equipfleet.service;

/** Utilization and uptime fractions for a single reporting window, each in the range [0, 1]. */
public record MetricResult(double utilization, double uptime) {

  public MetricResult {
    if (utilization < 0.0 || utilization > 1.0) {
      throw new IllegalArgumentException("utilization out of range: " + utilization);
    }
    if (uptime < 0.0 || uptime > 1.0) {
      throw new IllegalArgumentException("uptime out of range: " + uptime);
    }
  }
}
