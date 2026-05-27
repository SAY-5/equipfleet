package com.say5.equipfleet.sim;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "equipfleet.seed")
public class SeedProperties {

  private boolean enabled = false;
  private int assets = 8;
  private int days = 2;
  private int changesPerDay = 12;

  public boolean enabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int assets() {
    return assets;
  }

  public void setAssets(int assets) {
    this.assets = assets;
  }

  public int days() {
    return days;
  }

  public void setDays(int days) {
    this.days = days;
  }

  public int changesPerDay() {
    return changesPerDay;
  }

  public void setChangesPerDay(int changesPerDay) {
    this.changesPerDay = changesPerDay;
  }
}
