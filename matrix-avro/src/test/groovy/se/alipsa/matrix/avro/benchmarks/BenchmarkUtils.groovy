package se.alipsa.matrix.avro.benchmarks

import groovy.transform.CompileStatic

@CompileStatic
final class BenchmarkUtils {

  private BenchmarkUtils() {
  }

  static long timeNs(Closure action) {
    long start = System.nanoTime()
    action.call()
    return System.nanoTime() - start
  }

  static String avgMs(List<Long> values) {
    if (values.isEmpty()) return "0"
    long sum = values.sum() as long
    return String.format(Locale.US, "%.2f", sum / 1_000_000.0d / values.size())
  }

  static String minMs(List<Long> values) {
    if (values.isEmpty()) return "0"
    return String.format(Locale.US, "%.2f", (values.min() as long) / 1_000_000.0d)
  }

  static String maxMs(List<Long> values) {
    if (values.isEmpty()) return "0"
    return String.format(Locale.US, "%.2f", (values.max() as long) / 1_000_000.0d)
  }
}
