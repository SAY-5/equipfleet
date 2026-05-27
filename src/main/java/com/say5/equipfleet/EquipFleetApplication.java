package com.say5.equipfleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EquipFleetApplication {

  public static void main(String[] args) {
    SpringApplication.run(EquipFleetApplication.class, args);
  }
}
