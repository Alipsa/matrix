package se.alipsa.matrix.parquet

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.metadata.ParquetMetadata
import org.apache.parquet.io.ColumnIOFactory
import org.apache.parquet.io.InputFile
import org.apache.parquet.io.RecordReader
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Type

import se.alipsa.matrix.core.Matrix

/** Closeable row-group iterator. A row group may be split with maxBatchRows; close on early exit. */
final class ParquetBatches implements MatrixBatchIterator {
  private static final String COMMA = ','
  private static final int KEEP_EMPTY = -1
  private final ParquetFileReader reader
  private final ParquetReadOptions options
  private final MessageType requested
  private final List<String> outputNames
  private final List<Class> types
  private final List<Integer> selected
  private final Map<Integer, Integer> groupIndexes
  private final ParquetPredicate residual
  private final FilterCompat.Filter filter
  private final String name
  private final String sourcePath
  private final String indexMetadata
  private final Map<String, Class> nestedTypes = [:]
  private final boolean binaryAsString
  private ColumnMaterializer materializer
  private RecordReader<Boolean> records
  private org.apache.parquet.column.page.PageReadStore pages
  private long rowsRemaining
  private long remaining
  private int groupPosition
  private boolean closed
  private Matrix pending

  /** Opens an input and parses its footer exactly once. */
  @SuppressWarnings('AbcMetric') // Validates and snapshots all footer-dependent state before data I/O.
  ParquetBatches(InputFile input, ParquetReadOptions options, String fallbackName, ParquetMetadata snapshot = null, String sourcePath = null) {
    options.validate()
    this.options = options
    this.sourcePath = sourcePath
    if (options.sourceFileColumn != null && sourcePath == null) { throw new IllegalArgumentException('sourceFileColumn requires a local file') }
    def conf = new PlainParquetConfiguration()
    def baseOptions = org.apache.parquet.ParquetReadOptions.builder(conf).build()
    ParquetMetadata footer = snapshot
    if (footer == null) {
      input.newStream().withCloseable { stream -> footer = ParquetFileReader.readFooter(input, baseOptions, stream) }
    }
    MessageType full = footer.fileMetaData.schema
    if (full.fields.empty || full.fields.any { it.repetition == Type.Repetition.REPEATED }) {
      throw new IllegalArgumentException('Parquet requires nonempty top-level fields; wrap repeated fields in LIST')
    }
    def metadata = footer.fileMetaData.keyValueMetaData
    name = options.matrixName ?: fallbackName ?: full.name
    indexMetadata = metadata[MatrixParquetReader.METADATA_INDEX_COLUMNS]
    String typeString = metadata[MatrixParquetReader.METADATA_COLUMN_TYPES]
    boolean legacy = typeString != null && !metadata.containsKey('matrix.parquet.version')

    binaryAsString = options.binaryAsString != null ? options.binaryAsString : legacy
    outputNames = options.columns == null ? full.fields*.name : new ArrayList<>(options.columns)
    outputNames.each { String column ->
      if (!full.containsField(column)) { throw new IllegalArgumentException("Unknown column '$column'") }
    }
    residual = options.filter?.bind(full, options.zoneId)
    Set<String> needed = new LinkedHashSet<>(outputNames)
    if (options.filter != null) { needed.addAll(options.filter.columns()) }
    validateLegacyBinary(full, typeString, needed, legacy)
    if (options.sourceFileColumn != null && full.containsField(options.sourceFileColumn)) { throw new IllegalArgumentException('sourceFileColumn collides with file schema') }
    requested = new MessageType(full.name, full.fields.findAll { needed.contains(it.name) })
    List<Class> inferred = MatrixParquetReader.extractFieldTypes(full)
    List<String> declared = typeString == null ? [] : typeString.split(COMMA, KEEP_EMPTY).toList()
    types = requested.fields.collect { Type field ->
      int index = full.getFieldIndex(field.name)
      Class explicit = declared.size() == full.fieldCount ? ColumnTypePolicy.METADATA_TYPES[declared[index]] : null
      explicit = compatibleMetadata(explicit, field) ? explicit : null
      explicit ?: (binaryAsString && field.primitive && field.asPrimitiveType().logicalTypeAnnotation == null &&
          field.asPrimitiveType().primitiveTypeName == org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY ? String : inferred[index])
    }
    Map<String, String> nested = MatrixParquetReader.parseNestedTypes(metadata[MatrixParquetReader.METADATA_NESTED_TYPES])
    requested.fields.each { Type field ->
      NestedPaths.leaves(field, NestedPaths.root(field.name)).each { String path, Type leaf ->
        Class expected = ColumnTypePolicy.METADATA_TYPES[nested[path]]
        if (path != NestedPaths.root(field.name) && compatibleMetadata(expected, leaf)) { nestedTypes[path] = expected }
      }
    }
    selected = options.rowGroups == null ? (0..<footer.blocks.size()).toList() : new ArrayList<>(options.rowGroups)
    selected.each { Integer id ->
      if (id == null || id < 0 || id >= footer.blocks.size()) { throw new IllegalArgumentException("Invalid row-group ID $id") }
    }
    def pushed = residual?.pushdown(full)
    filter = pushed == null ? FilterCompat.NOOP : FilterCompat.get(pushed)
    def readOptions = org.apache.parquet.ParquetReadOptions.builder(conf).withRecordFilter(filter)
        .useColumnIndexFilter(options.useColumnIndexFilter)
        .useStatsFilter(options.useRowGroupFilters).useDictionaryFilter(options.useRowGroupFilters)
        .useBloomFilter(options.useRowGroupFilters).build()
    def stream = input.newStream()
    try {
      reader = ParquetFileReader.open(input, footer, readOptions, stream)
      reader.setRequestedSchema(requested)
    } catch (Exception failure) {
      stream.close()
      throw failure
    }
    groupIndexes = [:]
    reader.rowGroups.eachWithIndex { block, int index -> groupIndexes[footer.blocks.indexOf(block)] = index }
    remaining = options.limit == null ? Long.MAX_VALUE : options.limit
  }

  private static void validateLegacyBinary(MessageType full, String typeString, Set<String> needed, boolean legacy) {
    List<String> legacyTypes = typeString == null ? [] : typeString.split(COMMA, KEEP_EMPTY).toList()
    if (legacy && legacyTypes.size() == full.fieldCount && full.fields.any { field -> needed.contains(field.name) && legacyTypes[full.getFieldIndex(field.name)] == '[B' }) {
      throw new IllegalArgumentException('Unrecoverable legacy Matrix byte[]: original binary payload was not preserved')
    }
  }

  @groovy.transform.PackageScope
  static boolean compatibleMetadata(Class expected, Type field) {
    if (expected == null) { return false }
    if (!field.primitive) {
      boolean list = field.logicalTypeAnnotation instanceof org.apache.parquet.schema.LogicalTypeAnnotation.ListLogicalTypeAnnotation
      return list ? expected == List : expected == Map
    }
    def primitive = field.asPrimitiveType()
    def logical = primitive.logicalTypeAnnotation
    if (logical instanceof org.apache.parquet.schema.LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) { return expected == BigDecimal || (expected == BigInteger && logical.scale == 0) }
    if (logical instanceof org.apache.parquet.schema.LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) { return expected in [Date, java.sql.Timestamp, java.time.LocalDateTime, java.time.Instant] }
    if (logical instanceof org.apache.parquet.schema.LogicalTypeAnnotation.TimeLogicalTypeAnnotation) { return expected in [java.sql.Time, java.time.LocalTime, Date] }
    if (logical instanceof org.apache.parquet.schema.LogicalTypeAnnotation.DateLogicalTypeAnnotation) { return expected in [java.sql.Date, java.time.LocalDate, Date] }
    if (logical instanceof org.apache.parquet.schema.LogicalTypeAnnotation.UUIDLogicalTypeAnnotation) { return expected == UUID }
    if (MatrixParquetReader.isTextLogicalType(logical)) { return expected == String }
    if (logical instanceof org.apache.parquet.schema.LogicalTypeAnnotation.IntLogicalTypeAnnotation) {
      if (!logical.isSigned()) { return expected == MatrixParquetReader.unsignedType(logical) }
      return logical.bitWidth == 8 ? expected in [Byte, Short, Integer] : logical.bitWidth == 16 ? expected in [Short, Integer] :
          expected == (logical.bitWidth == Long.SIZE ? Long : Integer)
    }
    compatiblePhysicalMetadata(expected, primitive.primitiveTypeName)
  }

  private static boolean compatiblePhysicalMetadata(Class expected,
      org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName physical) {
    switch (physical) {
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT96 -> expected in [Date, java.sql.Timestamp, java.time.LocalDateTime, java.time.Instant]
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32 -> expected in [Byte, Short, Integer]
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64 -> expected == Long
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT -> expected == Float
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE -> expected == Double
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BOOLEAN -> expected == Boolean
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY -> expected in [String, byte[], BigDecimal]
      case org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY -> expected == byte[]
      default -> false
    }
  }

  /** Returns an empty Matrix with the selected names and types. */
  Matrix emptyMatrix() {
    List<List> empty = requested.fields.collect { [] }
    build(empty)
  }

  private Matrix build(List<List> data) {
    List<Integer> indexes = outputNames.collect { requested.getFieldIndex(it) }
    List<List> copied = indexes.collect { data[it] }
    Matrix result = Matrix.builder(name).columnNames(outputNames).types(indexes.collect { types[it] })
        .columns(copied).build()
    MatrixParquetReader.restoreIndex(result, indexMetadata)
    if (options.sourceFileColumn != null) { result.addColumn(options.sourceFileColumn, String, [sourcePath] * result.rowCount()) }
    result
  }

  @Override
  boolean hasNext() {
    MatrixParquetReader.withReadZone(options.zoneId) { advance() }
  }

  private boolean advance() {
    if (pending != null) { return true }
    if (closed) { return false }
    try {
      while (remaining > 0) {
        if (rowsRemaining == 0) {
          pages?.close()
          pages = null
          if (groupPosition >= selected.size()) { close(); return false }
          Integer index = groupIndexes[selected[groupPosition++]]
          if (index == null) { continue }
          pages = reader.readFilteredRowGroup(index)
          if (pages == null) { continue }
          rowsRemaining = pages.rowCount
          materializer = new ColumnMaterializer(requested, types, binaryAsString, residual, nestedTypes)
          def columnIO = new ColumnIOFactory(reader.fileMetaData.createdBy).getColumnIO(requested, reader.fileMetaData.schema, true)
          records = columnIO.getRecordReader(pages, materializer, filter)
        }
        int cap = (int) Math.min(remaining, options.maxBatchRows ?: Integer.MAX_VALUE)
        while (rowsRemaining > 0 && materializer.columns[0].size() < cap) {
          if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException('Parquet decoding cancelled') }
          try { records.read() } catch (Exception failure) { materializer.skipCurrentRecord(); throw failure }
          rowsRemaining--
        }
        if (!materializer.columns[0].empty) {
          int count = materializer.columns[0].size()
          pending = build(materializer.columns)
          materializer.columns.each { it.clear() }
          remaining -= count
          return true
        }
      }
      close()
      false
    } catch (Exception failure) {
      close()
      throw failure
    }
  }

  @Override
  Matrix next() {
    if (!hasNext()) { throw new NoSuchElementException() }
    Matrix result = pending
    pending = null
    result
  }

  @Override
  void close() {
    if (!closed) {
      closed = true
      pending = null
      try { pages?.close() } finally { reader.close() }
    }
  }
}
