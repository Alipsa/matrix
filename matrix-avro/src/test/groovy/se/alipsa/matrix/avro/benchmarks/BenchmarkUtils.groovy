package se.alipsa.matrix.avro.benchmarks

/**
 * Shared helpers for Matrix Avro benchmark entry points.
 */
final class BenchmarkUtils {

  private static final String ZERO_MS = '0'
  private static final String TWO_DECIMALS = '%.2f'
  private static final double NANOS_PER_MILLI = 1_000_000.0d
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
    return String.format(Locale.US, TWO_DECIMALS, sum / NANOS_PER_MILLI / values.size())
  }
  static String minMs(List<Long> values) {
    if (values.isEmpty()) {
      return ZERO_MS
    }
    return String.format(Locale.US, TWO_DECIMALS, (values.min() as long) / NANOS_PER_MILLI)
  }
  static String maxMs(List<Long> values) {
    if (values.isEmpty()) {
      return ZERO_MS
    }
    return String.format(Locale.US, TWO_DECIMALS, (values.max() as long) / NANOS_PER_MILLI)
  }

}
