package com.say5.equipfleet;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for integration tests. Uses a single shared Postgres container for the whole suite (started
 * once, never stopped per test) to keep the suite fast and avoid per-test container churn.
 */
@SpringBootTest
public abstract class AbstractPostgresIT {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("equipfleet")
          .withUsername("equipfleet")
          .withPassword("equipfleet");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}
