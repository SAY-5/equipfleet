package com.say5.equipfleet.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.say5.equipfleet.AbstractPostgresIT;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EquipmentApiIT extends AbstractPostgresIT {

  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;

  private String base() {
    return "http://localhost:" + port + "/api";
  }

  @Test
  void registersListsAndRecordsEventsOverHttp() {
    ResponseEntity<Map> created =
        rest.postForEntity(
            base() + "/equipment", Map.of("type", "loader", "site", "yard-1"), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Number id = (Number) created.getBody().get("id");

    ResponseEntity<Map> event =
        rest.postForEntity(
            base() + "/equipment/" + id + "/events",
            Map.of("status", "IN_USE", "occurredAt", "2026-06-01T08:00:00Z"),
            Map.class);
    assertThat(event.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<Object[]> history =
        rest.getForEntity(base() + "/equipment/" + id + "/events", Object[].class);
    assertThat(history.getBody()).hasSize(1);

    ResponseEntity<Object[]> list = rest.getForEntity(base() + "/equipment", Object[].class);
    assertThat(list.getBody()).isNotEmpty();

    ResponseEntity<Map> one = rest.getForEntity(base() + "/equipment/" + id, Map.class);
    assertThat(one.getBody().get("type")).isEqualTo("loader");
  }

  @Test
  void missingEquipmentReturnsNotFound() {
    ResponseEntity<Map> response = rest.getForEntity(base() + "/equipment/987654", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).containsKey("error");
  }

  @Test
  void invalidRegistrationReturnsBadRequest() {
    ResponseEntity<Map> response =
        rest.postForEntity(base() + "/equipment", Map.of("type", "", "site", ""), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsKey("error");
  }
}
