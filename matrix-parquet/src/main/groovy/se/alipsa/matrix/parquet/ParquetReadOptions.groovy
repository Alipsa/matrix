package se.alipsa.matrix.parquet

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.time.ZoneId

/**
 * Typed options for Parquet read operations via the SPI.
 */
class ParquetReadOptions {
  private static final String KEY_REMOTE_READ_MODE = 'remoteReadMode'
  private static final String DOWNLOAD = 'DOWNLOAD'
  private static final String KEY_CONNECT_TIMEOUT_MILLIS = 'connectTimeoutMillis'
  private static final String KEY_READ_TIMEOUT_MILLIS = 'readTimeoutMillis'
  private static final String KEY_RANGE_CACHE_BYTES = 'rangeCacheBytes'
  private static final String KEY_SOURCE_FILE_COLUMN = 'sourceFileColumn'
  private static final String KEY_PARALLELISM = 'parallelism'
  private static final String KEY_LIMIT = 'limit'
  private static final String KEY_ROW_GROUPS_BY_FILE = 'rowGroupsByFile'
  private static final String KEY_ROW_GROUPS = 'rowGroups'
  private static final String KEY_MAX_BATCH_ROWS = 'maxBatchRows'

  private static final String KEY_COLUMNS = 'columns'
  private static final String KEY_FILTER = 'filter'
  private static final String KEY_BINARY_AS_STRING = 'binaryAsString'
  private static final String KEY_USE_COLUMN_INDEX_FILTER = 'useColumnIndexFilter'

  String remoteReadMode = DOWNLOAD
  int connectTimeoutMillis = 10000
  int readTimeoutMillis = 60000
  int rangeCacheBytes = 8388608

  @groovy.transform.PackageScope
  String hubToken = null
  String sourceFileColumn = null
  int parallelism = 1

  @groovy.transform.PackageScope
  boolean useRowGroupFilters = true

  List<String> columns = null
  ParquetPredicate filter = null
  Boolean binaryAsString = null
  boolean useColumnIndexFilter = true
  Long limit = null
  List<Integer> rowGroups = null
  Map<String, List<Integer>> rowGroupsByFile = [:]
  Integer maxBatchRows = null

  /** Validates bounds and duplicate selectors before opening data pages. */
  void validate() {
    if (!(remoteReadMode in [DOWNLOAD, 'AUTO', 'RANGE'])) { throw new IllegalArgumentException('remoteReadMode must be DOWNLOAD, AUTO or RANGE') }
    if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0 || rangeCacheBytes <= 0) { throw new IllegalArgumentException('HTTP timeouts and rangeCacheBytes must be positive') }

    if (parallelism <= 0) { throw new IllegalArgumentException('parallelism must be positive') }
    if (limit != null && limit < 0) { throw new IllegalArgumentException('limit must be nonnegative') }
    if (maxBatchRows != null && maxBatchRows <= 0) { throw new IllegalArgumentException('maxBatchRows must be positive') }
    if (columns != null && (columns.empty || columns.toSet().size() != columns.size())) { throw new IllegalArgumentException('columns must be nonempty and unique') }
    if (rowGroups != null && rowGroups.toSet().size() != rowGroups.size()) { throw new IllegalArgumentException('rowGroups must be unique') }
  }

  /** Transport: DOWNLOAD, AUTO or RANGE; default DOWNLOAD. */
  ParquetReadOptions remoteReadMode(String value) { this.remoteReadMode = value; this }

  /** HTTP connection timeout; default 10000. */
  ParquetReadOptions connectTimeoutMillis(int value) { this.connectTimeoutMillis = value; this }

  /** HTTP read timeout; default 60000. */
  ParquetReadOptions readTimeoutMillis(int value) { this.readTimeoutMillis = value; this }

  /** Maximum HTTP range cache bytes; default 8388608. */
  ParquetReadOptions rangeCacheBytes(int value) { this.rangeCacheBytes = value; this }

  /** Optional local source path column; default null. */
  ParquetReadOptions sourceFileColumn(String value) { this.sourceFileColumn = value; this }

  /** Bounded dataset decoder workers; default 1. */
  ParquetReadOptions parallelism(int value) { this.parallelism = value; this }

  /** Ordered output columns; default null. */
  ParquetReadOptions columns(List<String> value) { this.columns = value; this }

  /** Exact logical row filter; default null. */
  ParquetReadOptions filter(ParquetPredicate value) { this.filter = value; this }

  /** Unannotated BINARY override; default null. */
  ParquetReadOptions binaryAsString(Boolean value) { this.binaryAsString = value; this }

  /** Enable page-index pruning; default true. */
  ParquetReadOptions useColumnIndexFilter(boolean value) { this.useColumnIndexFilter = value; this }

  /** Maximum accepted rows; default null. */
  ParquetReadOptions limit(Long value) { this.limit = value; this }

  /** Ordered original footer IDs; default null. */
  ParquetReadOptions rowGroups(List<Integer> value) { this.rowGroups = value; this }

  /** Per-file original footer group IDs, keyed by canonical local path; default empty/all. */
  ParquetReadOptions rowGroupsByFile(Map<String, List<Integer>> value) { rowGroupsByFile = value; this }

  /** Maximum rows per batch; default null. */
  ParquetReadOptions maxBatchRows(Integer value) { this.maxBatchRows = value; this }

  String matrixName = null
  ZoneId zoneId = null

  ParquetReadOptions matrixName(String value) {
    this.matrixName = value
    this
  }

  ParquetReadOptions zoneId(ZoneId value) {
    this.zoneId = value
    this
  }

  ParquetReadOptions zoneId(String value) {
    this.zoneId = ZoneId.of(value)
    this
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [:]
    if (matrixName != null) {
      result.matrixName = matrixName
    }
    if (zoneId != null) {
      result.zoneId = zoneId
    }
    [KEY_COLUMNS, KEY_FILTER, KEY_BINARY_AS_STRING, KEY_USE_COLUMN_INDEX_FILTER, KEY_LIMIT, KEY_ROW_GROUPS, KEY_ROW_GROUPS_BY_FILE, KEY_MAX_BATCH_ROWS, KEY_REMOTE_READ_MODE, KEY_CONNECT_TIMEOUT_MILLIS, KEY_READ_TIMEOUT_MILLIS, KEY_RANGE_CACHE_BYTES, KEY_SOURCE_FILE_COLUMN, KEY_PARALLELISM].each { String key ->
      result[key] = this[key]
    }
    result
  }

  static ParquetReadOptions fromMap(Map<String, ?> options) {
    ParquetReadOptions result = new ParquetReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    Set<String> known = descriptors()*.name*.toLowerCase(Locale.ROOT).toSet()
    Set<String> unknown = new HashSet<>(normalized.keySet())
    unknown.removeAll(known)
    if (!unknown.empty) { throw new IllegalArgumentException("Unknown Parquet options: $unknown") }
    normalized = normalized.findAll { String key, Object value -> value != null }
    if (normalized.containsKey('matrixname')) {
      String matrixName = OptionMaps.stringValueOrNull(normalized.matrixname)
      if (matrixName != null) {
        result.matrixName(matrixName)
      }
    }
    if (normalized.containsKey('zoneid')) {
      def value = normalized.zoneid
      if (value instanceof ZoneId) {
        result.zoneId(value as ZoneId)
      } else if (value != null) {
        result.zoneId(String.valueOf(value))
      }
    }
    applySelection(result, normalized)
    result.validate()
    result
  }

  private static void applySelection(ParquetReadOptions result, Map<String, Object> normalized) {
    if (normalized.containsKey(KEY_COLUMNS)) { result.columns = (List<String>) normalized.columns }
    if (normalized.containsKey(KEY_FILTER)) { result.filter = (ParquetPredicate) normalized.filter }
    if (normalized.containsKey('binaryasstring')) { result.binaryAsString = OptionMaps.booleanValueOrNull(normalized.binaryasstring, KEY_BINARY_AS_STRING) }
    if (normalized.containsKey('usecolumnindexfilter')) { result.useColumnIndexFilter = OptionMaps.booleanValueOrNull(normalized.usecolumnindexfilter, KEY_USE_COLUMN_INDEX_FILTER) }
    if (normalized.limit != null) { result.limit = ((Number) normalized.limit).longValue() }
    if (normalized.rowgroupsbyfile != null) { result.rowGroupsByFile = (Map<String, List<Integer>>) normalized.rowgroupsbyfile }
    if (normalized.rowgroups != null) { result.rowGroups = (List<Integer>) normalized.rowgroups }
    if (normalized.maxbatchrows != null) { result.maxBatchRows = ((Number) normalized.maxbatchrows).intValue() }
    if (normalized.remotereadmode != null) { result.remoteReadMode = String.valueOf(normalized.remotereadmode) }
    if (normalized.connecttimeoutmillis != null) { result.connectTimeoutMillis = ((Number) normalized.connecttimeoutmillis).intValue() }
    if (normalized.readtimeoutmillis != null) { result.readTimeoutMillis = ((Number) normalized.readtimeoutmillis).intValue() }
    if (normalized.rangecachebytes != null) { result.rangeCacheBytes = ((Number) normalized.rangecachebytes).intValue() }
    if (normalized.sourcefilecolumn != null) { result.sourceFileColumn = String.valueOf(normalized.sourcefilecolumn) }
    if (normalized.parallelism != null) { result.parallelism = ((Number) normalized.parallelism).intValue() }
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    ParquetReadOptions defaults = new ParquetReadOptions()
    [
        new OptionDescriptor('matrixName', String, null, 'Name for the resulting Matrix'),
        new OptionDescriptor('zoneId', ZoneId, null, 'Time zone to use when reading timestamp values'),
        new OptionDescriptor(KEY_REMOTE_READ_MODE, String, defaults.remoteReadMode, 'Transport: DOWNLOAD, AUTO or RANGE'),
        new OptionDescriptor(KEY_CONNECT_TIMEOUT_MILLIS, Integer, defaults.connectTimeoutMillis, 'HTTP connection timeout'),
        new OptionDescriptor(KEY_READ_TIMEOUT_MILLIS, Integer, defaults.readTimeoutMillis, 'HTTP read timeout'),
        new OptionDescriptor(KEY_RANGE_CACHE_BYTES, Integer, defaults.rangeCacheBytes, 'Maximum HTTP range cache bytes'),
        new OptionDescriptor(KEY_SOURCE_FILE_COLUMN, String, null, 'Optional local source path column'),
        new OptionDescriptor(KEY_PARALLELISM, Integer, defaults.parallelism, 'Bounded dataset decoder workers'),
        new OptionDescriptor(KEY_COLUMNS, List, null, 'Ordered output columns'),
        new OptionDescriptor(KEY_FILTER, ParquetPredicate, null, 'Exact logical row filter'),
        new OptionDescriptor(KEY_BINARY_AS_STRING, Boolean, null, 'Unannotated BINARY override'),
        new OptionDescriptor(KEY_USE_COLUMN_INDEX_FILTER, Boolean, defaults.useColumnIndexFilter, 'Enable page-index pruning'),
        new OptionDescriptor(KEY_LIMIT, Long, null, 'Maximum accepted rows'),
        new OptionDescriptor(KEY_ROW_GROUPS, List, null, 'Ordered original footer IDs'),
        new OptionDescriptor(KEY_ROW_GROUPS_BY_FILE, Map, null, 'Per-file footer IDs keyed by canonical local path'),
        new OptionDescriptor(KEY_MAX_BATCH_ROWS, Integer, null, 'Maximum rows per batch')
    ]
  }
}
