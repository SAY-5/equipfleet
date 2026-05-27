package com.say5.equipfleet;

import static org.assertj.core.api.Assertions.assertThat;

import com.say5.equipfleet.domain.DailyReport;
import com.say5.equipfleet.domain.EquipmentStatus;
import com.say5.equipfleet.service.EquipmentService;
import com.say5.equipfleet.service.ReportingService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EquipFleetApiIT extends AbstractPostgresIT {

  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;
  @Autowired EquipmentService equipmentService;
  @Autowired ReportingService reportingService;

  @Test
  void servesDomainEndToEnd() {
    String base = "http://localhost:" + port + "/api";

    record EquipmentResponse(Long id, String type, String site, String status) {}
    var created =
        rest.postForEntity(
            base + "/equipment",
            new java.util.HashMap<>(java.util.Map.of("type", "forklift", "site", "plant-a")),
            EquipmentResponse.class);
    assertThat(created.getStatusCode().is2xxSuccessful()).isTrue();
    Long id = created.getBody().id();
    assertThat(id).isNotNull();

    LocalDate day = LocalDate.parse("2026-03-01");
    Instant start = day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    equipmentService.recordEvent(id, EquipmentStatus.IN_USE, start);
    equipmentService.recordEvent(
        id, EquipmentStatus.IDLE, start.plus(java.time.Duration.ofHours(12)));

    List<DailyReport> rows = reportingService.generateForDay(day);
    assertThat(rows).isNotEmpty();

    ResponseEntity<String> fetched = rest.getForEntity(base + "/reports/" + day, String.class);
    assertThat(fetched.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(fetched.getBody()).contains("utilization");
  }
}
