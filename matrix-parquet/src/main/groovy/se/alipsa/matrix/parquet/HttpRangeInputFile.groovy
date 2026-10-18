package se.alipsa.matrix.parquet

import org.apache.parquet.io.InputFile
import org.apache.parquet.io.SeekableInputStream

import java.nio.ByteBuffer

/** HTTP range input with a bounded per-stream cache and stable object validators. */
final class HttpRangeInputFile implements InputFile {
  private static final String CONTENT_RANGE = 'Content-Range'
  private static final String ETAG = 'ETag'
  private static final String LAST_MODIFIED = 'Last-Modified'

  private static final int MAX_REQUEST_BYTES = 8388608
  private static final int PREFETCH_BYTES = 65536
  private static final int EOF = -1
  private volatile URL resolvedTarget
  private final URL source
  private final ParquetReadOptions options
  private final int cacheBytes
  private final long length
  private final String etag
  private final String modified

  /** Verifies byte-range support immediately; errors allow AUTO callers to choose download. */
  HttpRangeInputFile(URL source, ParquetReadOptions options) {
    if (!(source.protocol in ['http', 'https'])) { throw new IOException('Range reads require HTTP(S)') }
    this.source = source
    this.options = options
    cacheBytes = options.rangeCacheBytes

    def response = request(0L, 0L)
    def connection = response.connection
    boolean complete = false
    try {
      String range = connection.getHeaderField(CONTENT_RANGE)
      def match = range =~ /bytes 0-0\/(\d+)/
      if (!match.matches()) { throw new IOException("Invalid Content-Range: $range") }
      length = Long.parseLong(match.group(1))
      if (length <= 0) { throw new IOException('Empty remote object') }
      etag = connection.getHeaderField(ETAG)
      modified = connection.getHeaderField(LAST_MODIFIED)
      connection.inputStream.withCloseable { stream ->
        if (stream.read() < 0 || stream.read() != EOF) { throw new IOException('Invalid range payload length') }
      }
      resolvedTarget = connection.URL
      complete = true
    } finally { if (!complete) { connection.disconnect() } }
  }

  private Map<String, HttpURLConnection> request(long start, long end) {
    Map<String, String> headers = [Range: "bytes=$start-$end".toString()]
    if (etag != null && !etag.startsWith('W/')) { headers['If-Match'] = etag }
    else if (modified != null) { headers['If-Unmodified-Since'] = modified }
    URL target = resolvedTarget ?: source
    HttpURLConnection connection = HttpTransport.open(target, options, headers)
    if (target.toExternalForm() != source.toExternalForm() && connection.responseCode in [401, 403]) {
      connection.disconnect()
      connection = HttpTransport.open(source, options, headers)
    }
    if (connection.responseCode != 206 || !(connection.getHeaderField('Content-Encoding') in [null, 'identity'])) {
      int status = connection.responseCode
      connection.disconnect()
      throw new IOException("Range request rejected: HTTP $status")
    }
    [connection: connection]
  }

  private byte[] fetch(long start, int count) {
    long end = start + count - 1
    HttpURLConnection connection = request(start, end).connection
    boolean complete = false
    try {
      if (connection.getHeaderField(CONTENT_RANGE) != "bytes $start-$end/$length".toString() ||
          (etag != null && connection.getHeaderField(ETAG) != etag) ||
          (modified != null && connection.getHeaderField(LAST_MODIFIED) != modified)) {
        throw new IOException('Remote object/range changed during read')
      }
      connection.inputStream.withCloseable { stream ->
        byte[] bytes = stream.readNBytes(count)
        if (bytes.length != count || stream.read() != EOF) { throw new EOFException('Truncated/oversized HTTP range') }
        resolvedTarget = connection.URL
        complete = true
        bytes
      }
    } finally { if (!complete) { connection.disconnect() } }
  }

  @Override
  long getLength() { length }

  @Override
  SeekableInputStream newStream() {
    new SeekableInputStream() {
      private long position
      private long cacheStart = -1L
      private byte[] cache = new byte[0]
      private boolean closed

      @Override
      long getPos() { position }
      @Override
      void seek(long value) {
        if (value < 0 || value > length) { throw new EOFException("Invalid seek $value") }
        position = value
      }
      @Override
      int read() {
        byte[] single = new byte[1]
        read(single, 0, 1) < 0 ? EOF : single[0] & 0xff
      }
      @Override
      int read(byte[] target, int offset, int count) {
        Objects.checkFromIndexSize(offset, count, target.length)
        if (closed) { throw new IOException('Stream closed') }
        if (count == 0) { return 0 }
        if (position >= length) { return EOF }
        byte[] available = cache
        long start = cacheStart
        if (position < cacheStart || position >= cacheStart + cache.length) {
          start = position
          int requested = Math.max(Math.min(count, MAX_REQUEST_BYTES), Math.min(cacheBytes, PREFETCH_BYTES))
          available = fetch(position, (int) Math.min(requested, length - position))
          cacheStart = start
          cache = available.length <= cacheBytes ? available : new byte[0]
        }
        int copied = (int) Math.min(count, start + available.length - position)
        System.arraycopy(available, (int) (position - start), target, offset, copied)
        position += copied
        copied
      }
      @Override
      void readFully(byte[] target) { readFully(target, 0, target.length) }
      @Override
      void readFully(byte[] target, int offset, int count) {
        int total = 0
        while (total < count) {
          int result = read(target, offset + total, count - total)
          if (result < 0) { throw new EOFException() }
          total += result
        }
      }
      @Override
      int read(ByteBuffer target) {
        if (target.isReadOnly()) { throw new java.nio.ReadOnlyBufferException() }
        if (target.hasArray()) {
          int result = read(target.array(), target.arrayOffset() + target.position(), target.remaining())
          if (result > 0) { target.position(target.position() + result) }
          return result
        }
        byte[] bytes = new byte[Math.min(target.remaining(), MAX_REQUEST_BYTES)]
        int result = read(bytes, 0, bytes.length)
        if (result > 0) { target.put(bytes, 0, result) }
        result
      }
      @Override
      void readFully(ByteBuffer target) {
        while (target.hasRemaining()) {
          if (read(target) < 0) { throw new EOFException() }
        }
      }
      @Override
      void close() { closed = true; cache = new byte[0] }
    }
  }
}
