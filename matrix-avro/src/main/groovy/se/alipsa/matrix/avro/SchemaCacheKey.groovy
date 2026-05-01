package se.alipsa.matrix.avro

import groovy.transform.PackageScope

/**
 * Cache key for inferred or configured Avro schemas.
 */
@PackageScope
final class SchemaCacheKey {

  private static final int HASH_FACTOR = 31
  private final String schemaName
  private final String namespace
  private final boolean inferPrecisionAndScale
  private final int rowCount
  private final List<String> columnNames
  private final List<Class<?>> columnTypes
  private final Map<String, Map<String, ?>> columnSchemas
  SchemaCacheKey(String schemaName, String namespace, boolean inferPrecisionAndScale,
                 int rowCount, List<String> columnNames, List<Class<?>> columnTypes,
                 Map<String, Map<String, ?>> columnSchemas) {
    this.schemaName = schemaName
    this.namespace = namespace
    this.inferPrecisionAndScale = inferPrecisionAndScale
    this.rowCount = rowCount
    this.columnNames = Collections.unmodifiableList(new ArrayList<>(columnNames))
    this.columnTypes = Collections.unmodifiableList(new ArrayList<>(columnTypes))
    this.columnSchemas = columnSchemas
  }
  @Override
  boolean equals(Object other) {
    if (this.is(other)) {
      return true
    }
    if (!SchemaCacheKey.isInstance(other)) {
      return false
    }
    SchemaCacheKey that = (SchemaCacheKey) other
    inferPrecisionAndScale == that.inferPrecisionAndScale &&
        rowCount == that.rowCount &&
        schemaName == that.schemaName &&
        namespace == that.namespace &&
        columnNames == that.columnNames &&
        columnTypes == that.columnTypes &&
        columnSchemas == that.columnSchemas
  }
  @Override
  int hashCode() {
    int result = schemaName.hashCode()
    result = HASH_FACTOR * result + namespace.hashCode()
    result = HASH_FACTOR * result + (inferPrecisionAndScale ? 1 : 0)
    result = HASH_FACTOR * result + rowCount
    result = HASH_FACTOR * result + columnNames.hashCode()
    result = HASH_FACTOR * result + columnTypes.hashCode()
    result = HASH_FACTOR * result + columnSchemas.hashCode()
    result
  }

}
