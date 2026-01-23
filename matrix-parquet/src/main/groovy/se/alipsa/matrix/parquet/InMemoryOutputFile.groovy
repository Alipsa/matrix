package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.PositionOutputStream

/**
 * An {@link OutputFile} implementation that writes Parquet data to memory instead of disk.
 *
 * <p>This class enables writing Parquet format data directly to a byte array without
 * creating temporary files. It's particularly useful for:</p>
 * <ul>
 *   <li>Network transmission (e.g., HTTP responses, message queues)</li>
 *   <li>In-memory data processing pipelines</li>
 *   <li>Testing without file system I/O</li>
 *   <li>Cloud storage uploads (write to memory, then upload)</li>
 * </ul>
 *
 * <p><strong>Performance Note:</strong> For datasets larger than ~100MB, consider writing
 * directly to a file using {@link MatrixParquetWriter#write(Matrix, File)} to avoid
 * memory pressure. The in-memory approach requires approximately 2-3x the final file size
 * in heap memory.</p>
 *
 * <p><strong>Size Limitations:</strong> Maximum file size is limited to ~2GB due to
 * Java's {@link ByteArrayOutputStream} constraints (Integer.MAX_VALUE).</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * InMemoryOutputFile outputFile = new InMemoryOutputFile()
 *
 * // Use with ExampleParquetWriter
 * ParquetWriter<Group> writer = ExampleParquetWriter.builder(outputFile)
 *     .withType(schema)
 *     .build()
 *
 * // Write data...
 * writer.write(group)
 * writer.close()
 *
 * // Retrieve bytes
 * byte[] parquetData = outputFile.getBytes()
 * }</pre>
 *
 * @see InMemoryPositionOutputStream
 * @see MatrixParquetWriter
 * @see org.apache.parquet.io.OutputFile
 */
@CompileStatic
class InMemoryOutputFile implements OutputFile {

  private InMemoryPositionOutputStream stream

  /**
   * Creates a new output stream for writing.
   *
   * @param blockSizeHint suggested block size (ignored for in-memory implementation)
   * @return a new PositionOutputStream for writing
   * @throws IOException if an I/O error occurs
   */
  @Override
  PositionOutputStream create(long blockSizeHint) throws IOException {
    stream = new InMemoryPositionOutputStream()
    return stream
  }

  /**
   * Creates a new output stream, overwriting any existing data.
   *
   * @param blockSizeHint suggested block size (ignored for in-memory implementation)
   * @return a new PositionOutputStream for writing
   * @throws IOException if an I/O error occurs
   */
  @Override
  PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
    return create(blockSizeHint)
  }

  /**
   * Indicates whether this implementation supports custom block sizes.
   *
   * @return false, as in-memory implementation doesn't use block sizes
   */
  @Override
  boolean supportsBlockSize() {
    return false
  }

  /**
   * Returns the default block size.
   *
   * @return 0, as in-memory implementation doesn't use block sizes
   */
  @Override
  long defaultBlockSize() {
    return 0
  }

  /**
   * Returns the Parquet data as a byte array.
   *
   * <p>This method should only be called after the Parquet writer has been closed.</p>
   *
   * @return a byte array containing the complete Parquet file
   * @throws IllegalStateException if called before any data has been written
   */
  byte[] getBytes() {
    if (stream == null) {
      throw new IllegalStateException("No data has been written yet. Call this method after closing the writer.")
    }
    return stream.toByteArray()
  }
}
