package com.say5.equipfleet.service;

import com.say5.equipfleet.domain.EquipmentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes utilization and uptime over a half-open window {@code [windowStart, windowEnd)} from a
 * timeline of status segments.
 *
 * <p>The status model is step-wise: an asset holds a status from the instant it is entered until
 * the next status change. Utilization is the fraction of the window spent {@code IN_USE}; uptime is
 * the fraction of the window spent not {@code DOWN}. The calculator clamps every segment to the
 * window, so events outside the window never contribute, and a segment that begins before the
 * window or runs past its end (a status still open at window end, or one spanning midnight) is
 * counted only for its in-window portion.
 */
public final class IntervalMetricsCalculator {

  private IntervalMetricsCalculator() {}

  /** A status that begins at {@code start}. The end is derived from the following segment. */
  public record Segment(EquipmentStatus status, Instant start) {}

  /**
   * @param carryInStatus the status the asset is already in at {@code windowStart}, or null if its
   *     state before the window is unknown (treated as IDLE: not in-use, not down).
   * @param segments status changes; may be unsorted and may include events outside the window.
   */
  public static MetricResult compute(
      EquipmentStatus carryInStatus,
      List<Segment> segments,
      Instant windowStart,
      Instant windowEnd) {
    long windowMillis = windowEnd.toEpochMilli() - windowStart.toEpochMilli();
    if (windowMillis <= 0) {
      throw new IllegalArgumentException("window end must be after window start");
    }

    List<Segment> ordered = new ArrayList<>(segments);
    ordered.sort((a, b) -> a.start().compareTo(b.start()));

    // Build the effective status at windowStart and the in-window change points.
    EquipmentStatus current = carryInStatus == null ? EquipmentStatus.IDLE : carryInStatus;
    List<Segment> inWindow = new ArrayList<>();
    for (Segment s : ordered) {
      if (s.start().isBefore(windowStart)) {
        current = s.status();
      } else if (s.start().isBefore(windowEnd)) {
        inWindow.add(s);
      }
      // segments at or after windowEnd are ignored
    }

    long inUseMillis = 0;
    long downMillis = 0;

    Instant cursor = windowStart;
    EquipmentStatus cursorStatus = current;
    for (Segment s : inWindow) {
      Instant segEnd = s.start();
      long span = segEnd.toEpochMilli() - cursor.toEpochMilli();
      if (span > 0) {
        if (cursorStatus == EquipmentStatus.IN_USE) {
          inUseMillis += span;
        } else if (cursorStatus == EquipmentStatus.DOWN) {
          downMillis += span;
        }
      }
      cursor = segEnd;
      cursorStatus = s.status();
    }
    // Tail from the last change point (or windowStart) to windowEnd: status still open at day end.
    long tail = windowEnd.toEpochMilli() - cursor.toEpochMilli();
    if (tail > 0) {
      if (cursorStatus == EquipmentStatus.IN_USE) {
        inUseMillis += tail;
      } else if (cursorStatus == EquipmentStatus.DOWN) {
        downMillis += tail;
      }
    }

    double utilization = (double) inUseMillis / windowMillis;
    double uptime = (double) (windowMillis - downMillis) / windowMillis;
    return new MetricResult(utilization, uptime);
  }
}
