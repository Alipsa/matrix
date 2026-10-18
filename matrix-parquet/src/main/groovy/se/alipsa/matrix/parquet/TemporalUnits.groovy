package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit

import java.time.Instant

/** Overflow-safe epoch conversion shared by writers and exact predicate conversion. */
@PackageScope
final class TemporalUnits {
  private TemporalUnits() {}

  static long nanosPerUnit(TimeUnit unit) {
    unit == TimeUnit.MILLIS ? 1_000_000L : unit == TimeUnit.MICROS ? 1000L : 1L
  }

  static long epochUnits(Instant instant, TimeUnit unit) {
    long divisor = nanosPerUnit(unit)
    long units = 1_000_000_000L.intdiv(divisor)
    // Sub-microsecond digits are intentionally truncated under MICROS; see req/v0.8.0-fixes.md decision D.
    long fraction = instant.nano.intdiv(divisor)
    instant.epochSecond < 0 ?
        Math.addExact(Math.multiplyExact(instant.epochSecond + 1L, units), fraction - units) :
        Math.addExact(Math.multiplyExact(instant.epochSecond, units), fraction)
  }
}
