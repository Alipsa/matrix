package se.alipsa.matrix.parquet

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.metadata.ParquetMetadata
import org.apache.parquet.io.LocalInputFile

import se.alipsa.matrix.core.Matrix

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Ordered shard batches with strict schema checks and at most one prefetched batch per worker. */
final class DatasetBatches implements MatrixBatchIterator {
  private final List<File> files
  private final List<ParquetMetadata> footers = []
  private final List<Map<String, Integer>> work = []
  private final List<Long> lengths = []
  private final List<Long> modified = []
  private final ParquetReadOptions options
  private final ExecutorService executor
  private final Map<Integer, ParquetBatches> cursors = [:]
  private final Map<Integer, Future<Matrix>> pending = [:]
  private int current
  private int submitted
  private long remaining
  private volatile boolean closed
  private Matrix template
  private Matrix ready

  /** Checks every footer before reading any rows; supplied shard order is retained. */
  DatasetBatches(List<File> selection, ParquetReadOptions options) {
    options.validate()
    this.options = options
    if (selection == null) { throw new IllegalArgumentException('Dataset files cannot be null') }
    files = selection*.canonicalFile
    if (files.empty || files.toSet().size() != files.size()) {
      throw new IllegalArgumentException('Dataset files must be nonempty and unique')
    }
    readFooters()
    validateSchemas()
    selectWork()
    remaining = options.limit == null ? Long.MAX_VALUE : options.limit
    ParquetReadOptions emptyOptions = ParquetReadOptions.fromMap(options.toMap())
    emptyOptions.rowGroups = []
    emptyOptions.sourceFileColumn = null
    new ParquetBatches(new LocalInputFile(files[0].toPath()), emptyOptions, files[0].name, footers[0]).withCloseable { cursor -> template = cursor.emptyMatrix() }
    if (!footers.every { it.fileMetaData.keyValueMetaData[MatrixParquetReader.METADATA_INDEX_COLUMNS] == footers[0].fileMetaData.keyValueMetaData[MatrixParquetReader.METADATA_INDEX_COLUMNS] }) {
      template = Matrix.builder(template.matrixName).columnNames(template.columnNames()).types(template.types()).build()
    }
    if (options.sourceFileColumn != null) { template.addColumn(options.sourceFileColumn, String, []) }
    executor = Executors.newFixedThreadPool(Math.min(options.parallelism, Math.max(1, work.size())),
        { Runnable action ->
          Thread worker = new Thread(action, 'matrix-parquet-dataset')
          worker.daemon = true
          worker
        } as java.util.concurrent.ThreadFactory)
    try {
      if (remaining > 0) { (0..<Math.min(options.parallelism, work.size())).each { scheduleWork() } }
    } catch (Exception failure) { close(); throw failure }
  }

  private void readFooters() {
    files.each { File file ->
      if (!file.isFile()) { throw new IllegalArgumentException("Not a dataset file: $file") }
      lengths.add(file.length())
      modified.add(file.lastModified())
      def input = new LocalInputFile(file.toPath())
      def readOptions = org.apache.parquet.ParquetReadOptions.builder(new PlainParquetConfiguration()).build()
      input.newStream().withCloseable { stream -> footers.add(ParquetFileReader.readFooter(input, readOptions, stream)) }
    }
  }

  private void validateSchemas() {
    def schema = footers[0].fileMetaData.schema
    footers.eachWithIndex { ParquetMetadata footer, int index ->
      if (footer.fileMetaData.keyValueMetaData[MatrixParquetReader.METADATA_COLUMN_TYPES] != footers[0].fileMetaData.keyValueMetaData[MatrixParquetReader.METADATA_COLUMN_TYPES]) { throw new IllegalArgumentException("Incompatible shard Matrix types: ${files[index]}") }
      if (footer.fileMetaData.schema.fields != schema.fields) { throw new IllegalArgumentException("Incompatible shard schema: ${files[index]}") }
      if (options.sourceFileColumn != null && schema.containsField(options.sourceFileColumn)) { throw new IllegalArgumentException('sourceFileColumn collides with dataset schema') }
    }
    if (options.rowGroups != null) { throw new IllegalArgumentException('Dataset rowGroups require per-file reads') }
  }

  private void selectWork() {
    Set<String> paths = files*.canonicalPath.toSet()
    if (!paths.containsAll(options.rowGroupsByFile.keySet())) { throw new IllegalArgumentException('Unknown shard path in rowGroupsByFile') }
    files.eachWithIndex { File file, int index ->
      List<Integer> ids = options.rowGroupsByFile.containsKey(file.canonicalPath) ? options.rowGroupsByFile[file.canonicalPath] : (0..<footers[index].blocks.size()).toList()
      if (ids == null || ids.toSet().size() != ids.size() || ids.any { it == null || it < 0 || it >= footers[index].blocks.size() }) {
        throw new IllegalArgumentException("Invalid/duplicate row-group IDs for $file")
      }
      ids.each { Integer group -> work.add([file: index, group: group]) }
    }
  }

  @Override
  Matrix emptyMatrix() {
    Matrix result = Matrix.builder(template.matrixName).columnNames(template.columnNames()).types(template.types()).build()
    if (template.hasIndex()) { result.createIndex(template.indexedColumns() as String[]) }
    result
  }

  private void scheduleWork() {
    if (submitted >= work.size()) { return }
    int index = submitted++
    int source = work[index].file
    if (files[source].length() != lengths[source] || files[source].lastModified() != modified[source]) {
      throw new IOException("Shard changed after footer validation: ${files[source]}")
    }
    ParquetReadOptions perFile = ParquetReadOptions.fromMap(options.toMap())
    perFile.limit = null
    perFile.sourceFileColumn = null
    perFile.rowGroups = [work[index].group]
    cursors[index] = new ParquetBatches(new LocalInputFile(files[source].toPath()), perFile, files[source].name, footers[source])
    scheduleBatch(index)
  }

  private void scheduleBatch(int index) {
    ParquetBatches cursor = cursors[index]
    pending[index] = executor.submit({ ->
      synchronized (cursor) {
        closed ? null : (cursor.hasNext() ? cursor.next() : null)
      }
    } as Callable<Matrix>)
  }

  @Override
  boolean hasNext() {
    if (ready != null) { return true }
    if (closed) { return false }
    try {
      while (remaining > 0 && current < work.size()) {
        Matrix batch = pending.remove(current).get()
        if (batch == null) {
          cursors.remove(current).close()
          current++
          scheduleWork()
          continue
        }
        if (batch.rowCount() > remaining) {
          List<List> truncated = []
          batch.columnNames().each { truncated.add(new ArrayList(batch.column(it).subList(0, (int) remaining))) }
          batch = Matrix.builder(batch.matrixName).columnNames(batch.columnNames()).types(batch.types()).columns(truncated).build()
        }
        remaining -= batch.rowCount()
        if (options.sourceFileColumn != null) { batch.addColumn(options.sourceFileColumn, String, [files[work[current].file].absolutePath] * batch.rowCount()) }
        batch = withDatasetIndex(batch)
        ready = batch
        if (remaining > 0) { scheduleBatch(current) }
        return true
      }
      close()
      false
     } catch (Exception failure) {
      String source = current < work.size() ? files[work[current].file].absolutePath : 'dataset'
      try { close() } catch (Exception cleanup) { failure.addSuppressed(cleanup) }
      throw new IOException("Dataset decoding failed for $source", failure)
    }
  }

  private Matrix withDatasetIndex(Matrix batch) {
    if (template.hasIndex()) {
      if (!batch.hasIndex()) { batch.createIndex(template.indexedColumns() as String[]) }
    } else if (batch.hasIndex()) {
      List<List> columns = batch.columnNames().collect { (List) batch.column(it) }
      return Matrix.builder(batch.matrixName).columnNames(batch.columnNames()).types(batch.types())
          .columns(columns).build()
    }
    batch
  }

  @Override
  Matrix next() {
    if (!hasNext()) { throw new NoSuchElementException() }
    Matrix result = ready
    ready = null
    result
  }

  @Override
  void close() {
    if (closed) { return }
    closed = true
    pending.values().each { it.cancel(true) }
    executor.shutdownNow()
    try {
      executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt()
    } finally {
      // A cancelled Future may still be leaving its decoder. Serialize close with its work.
      cursors.values().each { ParquetBatches cursor ->
        synchronized (cursor) { cursor.close() }
      }
      ready = null
    }
  }
}
