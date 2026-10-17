package se.alipsa.matrix.parquet

import org.apache.parquet.io.InputFile
import org.apache.parquet.io.SeekableInputStream

import java.nio.ByteBuffer

/** Test-only exact byte counter over an independent seekable input. */
class CountingInput implements InputFile {
  final InputFile delegate
  long totalBytes
  CountingInput(InputFile delegate) { this.delegate = delegate }
  @Override long getLength() { delegate.length }
  @Override SeekableInputStream newStream() {
    SeekableInputStream stream = delegate.newStream()
    new SeekableInputStream() {
      @Override long getPos() { stream.pos }
      @Override void seek(long value) { stream.seek(value) }
      @Override int read() { int value = stream.read(); if (value >= 0) { totalBytes++ } ; value }
      @Override int read(byte[] target, int offset, int length) {
        int count = stream.read(target, offset, length)
        if (count > 0) { totalBytes += count }
        count
      }
      @Override void readFully(byte[] target) { readFully(target, 0, target.length) }
      @Override void readFully(byte[] target, int offset, int length) { stream.readFully(target, offset, length); totalBytes += length }
      @Override int read(ByteBuffer target) { int count = stream.read(target); if (count > 0) { totalBytes += count } ; count }
      @Override void readFully(ByteBuffer target) { int count = target.remaining(); stream.readFully(target); totalBytes += count }
      @Override void close() { stream.close() }
    }
  }
}
