# equipfleet

Equipment asset tracking and scheduled utilization reporting for a simulated fleet. equipfleet
registers equipment, records status changes over time, and runs a daily batch job that rolls the
day's events up into per-asset and fleet-level utilization and uptime reports.

Built with Java 21, Spring Boot, Spring Data JPA, Flyway, and PostgreSQL. Integration tests run
against a real Postgres via Testcontainers.

## Domain

- **Equipment**: an asset with a type, a site, and a current status (`IN_USE`, `IDLE`, `DOWN`).
- **Usage event**: a status change at an instant. An asset holds a status from each event until the
  next event, so the event stream is a step function over time.
- **Daily report**: a derived row per asset per day, plus one fleet-level row, holding utilization
  and uptime fractions for that day.

Utilization is the fraction of the day an asset is `IN_USE`. Uptime is the fraction of the day it is
not `DOWN`. Both are always in the range `[0, 1]`.

## REST API

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/equipment` | register an asset |
| `GET` | `/api/equipment` | list assets |
| `GET` | `/api/equipment/{id}` | fetch one asset |
| `POST` | `/api/equipment/{id}/events` | record a status event |
| `GET` | `/api/equipment/{id}/events` | an asset's event history |
| `GET` | `/api/reports/{date}` | reports for a day (ISO date) |
| `POST` | `/api/reports/{date}/backfill` | regenerate a past day's report |

## Service layer

`ReportingService` holds the rollup logic and is reused by both the REST layer and the scheduled
batch job. The metric math lives in `IntervalMetricsCalculator`, a pure function over a timeline of
status segments and a day window, which makes the tricky cases (events out of order, a status that
spans midnight, a status still open at day end, events outside the day) testable in isolation.

## Scheduled batch job

`DailyReportJob` runs once per day (cron, UTC) and rolls up the previous day's events. The rollup
upserts each report row keyed on `(date, scope)`, so re-running a day never double-counts. The same
entry point backs the on-demand backfill endpoint, which regenerates one historical day without
touching other days.

## Simulated fleet

`FleetSimulator` registers assets and emits a seeded stream of status changes so the platform has
data to report on. The data is synthetic and labelled as simulated; it is not from real equipment.
Set `EQUIPFLEET_SEED_ENABLED=true` to seed on startup.

## Running locally

```bash
docker compose up --build
```

This starts Postgres and the application, seeds a simulated fleet, and serves the API on
`http://localhost:8080`.

## Tests

```bash
mvn verify
```

Runs unit tests, integration tests against a Testcontainers Postgres, and a JaCoCo line-coverage
gate. Docker must be available.

## How this differs

equipfleet is an equipment-asset tracking and scheduled utilization/uptime reporting platform. Its
angle is the equipment domain, a reusable service layer shared by the API and the batch job, and an
idempotent scheduled rollup with backfill, which fits a manufacturing or equipment-operations
context.

It is distinct from:

- **fleetwatch**, which measures detection-quality metrics over a robot fleet in Python and C++.
- **shopflow** and **cloudshift**, which cover e-commerce and platform migration.

Those projects share the word "fleet" but solve different problems; equipfleet is about equipment
status over time and the reports derived from it.
