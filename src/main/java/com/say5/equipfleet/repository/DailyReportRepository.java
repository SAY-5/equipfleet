package com.say5.equipfleet.repository;

import com.say5.equipfleet.domain.DailyReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

  List<DailyReport> findByReportDateOrderByScopeKeyAsc(LocalDate reportDate);

  Optional<DailyReport> findByReportDateAndScopeKey(LocalDate reportDate, String scopeKey);
}
