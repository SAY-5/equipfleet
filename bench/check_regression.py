#!/usr/bin/env python3
"""bench-regress smoke gate.

Parses a BENCH line from the rollup benchmark and compares its measured
rollups_per_sec against a recorded baseline. Fails if throughput drops more
than the allowed tolerance below baseline. This is a smoke gate: it catches
gross regressions, not small run-to-run noise.
"""
import json
import re
import sys
from pathlib import Path

TOLERANCE = 0.30  # allow up to 30 percent slower than baseline

def parse_throughput(text: str) -> float:
    match = re.search(r"rollups_per_sec=([0-9.]+)", text)
    if not match:
        raise SystemExit("no BENCH line with rollups_per_sec found in input")
    return float(match.group(1))

def main() -> int:
    raw = sys.stdin.read()
    measured = parse_throughput(raw)

    baseline_path = Path(__file__).with_name("baseline.json")
    baseline = json.loads(baseline_path.read_text())
    expected = float(baseline["rollups_per_sec"])
    floor = expected * (1.0 - TOLERANCE)

    print(f"measured rollups_per_sec={measured:.1f}")
    print(f"baseline rollups_per_sec={expected:.1f} floor={floor:.1f} tolerance={TOLERANCE:.0%}")

    if measured < floor:
        print("REGRESSION: throughput below floor")
        return 1
    print("OK: throughput within tolerance")
    return 0

if __name__ == "__main__":
    sys.exit(main())
