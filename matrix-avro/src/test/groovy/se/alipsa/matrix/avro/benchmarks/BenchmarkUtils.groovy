package se.alipsa.matrix.avro.benchmarks

/**
 * Shared helpers for Matrix Avro benchmark entry points.
 */
final class BenchmarkUtils {

  private static final String ZERO_MS = '0'
  private static final String TWO_DECIMALS = '%.2f'
  private static final BigDecimal NANOSECONDS_PER_MILLISECOND = 1_000_000.0
  private BenchmarkUtils() {
  }
  static long timeNs(Closure action) {
    long start = System.nanoTime()
    action.call()
    return System.nanoTime() - start
  }
  static String avgMs(List<Long> values) {
    if (values.isEmpty()) {
      return ZERO_MS
    }
    long sum = values.sum() as long
    return String.format(Locale.US, TWO_DECIMALS, sum / NANOSECONDS_PER_MILLISECOND / values.size())
  }
  static String minMs(List<Long> values) {
    if (values.isEmpty()) {
      return ZERO_MS
    }
    return String.format(Locale.US, TWO_DECIMALS, (values.min() as long) / NANOSECONDS_PER_MILLISECOND)
  }
  static String maxMs(List<Long> values) {
    if (values.isEmpty()) {
      return ZERO_MS
    }
    return String.format(Locale.US, TWO_DECIMALS, (values.max() as long) / NANOSECONDS_PER_MILLISECOND)
  }

}
