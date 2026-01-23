package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import org.apache.parquet.io.PositionOutputStream

/**
 * A {@link PositionOutputStream} implementation backed by a {@link ByteArrayOutputStream}.
 *
 * <p>This class allows writing Parquet data directly to memory without creating temporary files.
 * It tracks the current write position which is required by the Parquet format for writing
 * metadata at the end of the file.</p>
 *
 * <p><strong>Limitations:</strong></p>
 * <ul>
 *   <li>Maximum size limited to ~2GB (Integer.MAX_VALUE) due to ByteArrayOutputStream constraints</li>
 *   <li>Entire file must fit in memory</li>
 *   <li>Not suitable for very large datasets</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * InMemoryPositionOutputStream stream = new InMemoryPositionOutputStream()
 * // Write data to stream...
 * byte[] parquetData = stream.toByteArray()
 * }</pre>
 *
 * @see InMemoryOutputFile
 * @see org.apache.parquet.io.PositionOutputStream
 */
@CompileStatic
class InMemoryPositionOutputStream extends PositionOutputStream {

  private final ByteArrayOutputStream contents
  private long position = 0

  /**
   * Creates a new in-memory position output stream.
   */
  InMemoryPositionOutputStream() {
    this.contents = new ByteArrayOutputStream()
  }

  /**
   * Returns the current write position in the stream.
   *
   * @return the number of bytes written so far
   * @throws IOException if an I/O error occurs
   */
  @Override
  long getPos() throws IOException {
    return position
  }

  /**
   * Writes a single byte to the stream.
   *
   * @param b the byte to write
   * @throws IOException if an I/O error occurs
   */
  @Override
  void write(int b) throws IOException {
    contents.write(b)
    position++
  }

  /**
   * Writes a portion of a byte array to the stream.
   *
   * @param b the byte array
   * @param off the offset to start reading from
   * @param len the number of bytes to write
   * @throws IOException if an I/O error occurs
   */
  @Override
  void write(byte[] b, int off, int len) throws IOException {
    contents.write(b, off, len)
    position += len
  }

  /**
   * Closes the stream.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  void close() throws IOException {
    contents.close()
  }

  /**
   * Returns the contents of this stream as a byte array.
   *
   * <p>This method should only be called after all data has been written
   * and the stream has been closed.</p>
   *
   * @return a byte array containing all the data written to this stream
   */
  byte[] toByteArray() {
    return contents.toByteArray()
  }
}
