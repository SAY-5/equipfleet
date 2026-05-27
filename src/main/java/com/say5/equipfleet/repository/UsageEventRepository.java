package com.say5.equipfleet.repository;

import com.say5.equipfleet.domain.UsageEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

  List<UsageEvent> findByEquipmentIdOrderByOccurredAtAsc(Long equipmentId);

  /**
   * Latest event strictly before {@code instant} for an asset, used to determine the status the
   * asset is already in at the start of a reporting window.
   */
  @Query(
      "select e from UsageEvent e where e.equipmentId = :equipmentId and e.occurredAt < :instant"
          + " order by e.occurredAt desc limit 1")
  UsageEvent findLatestBefore(
      @Param("equipmentId") Long equipmentId, @Param("instant") Instant instant);

  /** Events for an asset that occur within the half-open window [from, to). */
  @Query(
      "select e from UsageEvent e where e.equipmentId = :equipmentId and e.occurredAt >= :from"
          + " and e.occurredAt < :to order by e.occurredAt asc")
  List<UsageEvent> findInWindow(
      @Param("equipmentId") Long equipmentId, @Param("from") Instant from, @Param("to") Instant to);
}
