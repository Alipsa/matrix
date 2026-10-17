package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

/** Shared exact-decimal settings for top-level and nested logical field paths. */
@PackageScope
final class DecimalSchemaPolicy {
  final Map<String, int[]> settings
  final boolean infer
  final Integer precision
  final Integer scale
  final String path
  final Set<String> used

  DecimalSchemaPolicy(Map<String, int[]> settings, boolean infer = true, Integer precision = null,
      Integer scale = null, String path = '', Set<String> used = new HashSet<>()) {
    this.settings = settings
    this.infer = infer
    this.precision = precision
    this.scale = scale
    this.path = path
    this.used = used
  }

  DecimalSchemaPolicy child(String name) {
    new DecimalSchemaPolicy(settings, infer, precision, scale, path.empty ? name : "$path.$name".toString(), used)
  }

  int[] resolve(List<?> values, int[] supplied) {
    int[] explicit = settings[path] ?: supplied
    if (settings.containsKey(path)) { used.add(path) }
    if (explicit != null) { return explicit }
    if (precision != null) { return [precision, scale] as int[] }
    if (!infer) { throw new IllegalArgumentException("Decimal field '$path' requires explicit precision/scale when inference is disabled") }
    ColumnTypePolicy.decimal(values)
  }
}
