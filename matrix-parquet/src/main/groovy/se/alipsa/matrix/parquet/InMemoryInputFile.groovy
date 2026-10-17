package se.alipsa.matrix.parquet

import org.apache.parquet.io.DelegatingSeekableInputStream
import org.apache.parquet.io.InputFile
import org.apache.parquet.io.SeekableInputStream

/** Seekable Parquet input backed by a byte array, without temporary files. */
final class InMemoryInputFile implements InputFile {
  private final byte[] content

  /** Retains the supplied array; callers must not modify it during a read. */
  InMemoryInputFile(byte[] content) {
    if (content == null) { throw new IllegalArgumentException('content cannot be null') }
    this.content = content
  }

  @Override
  long getLength() { content.length }

  @Override
  SeekableInputStream newStream() {
    def input = new ByteArrayInputStream(content)
    new DelegatingSeekableInputStream(input) {
      @Override
      long getPos() { content.length - input.available() }
      @Override
      void seek(long position) {
        if (position < 0 || position > content.length) { throw new EOFException("Invalid position $position") }
        input.reset()
        input.skipNBytes(position)
      }
    }
  }
}
