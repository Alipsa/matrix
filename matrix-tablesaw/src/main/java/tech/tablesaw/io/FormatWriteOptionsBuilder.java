package tech.tablesaw.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;

/**
 * Shared base for format-specific write-option builders.
 *
 * <p>File destinations are opened lazily when writing begins. Constructing an options builder or
 * building options therefore does not create or truncate the target file.
 */
public class FormatWriteOptionsBuilder extends WriteOptions.Builder {

  /**
   * Creates a builder for an existing destination.
   *
   * @param destination destination to use
   */
  protected FormatWriteOptionsBuilder(Destination destination) {
    super(destination);
  }

  /**
   * Creates a builder for an output stream.
   *
   * @param stream stream to use
   */
  protected FormatWriteOptionsBuilder(OutputStream stream) {
    super(stream);
  }

  /**
   * Creates a builder for a writer.
   *
   * @param writer writer to use
   */
  protected FormatWriteOptionsBuilder(Writer writer) {
    super(writer);
  }

  /**
   * Creates a builder whose file is opened lazily when output starts.
   *
   * @param file file to write
   */
  protected FormatWriteOptionsBuilder(File file) {
    super(new LazyFileDestination(file));
    autoClose = true;
  }
}

final class LazyFileDestination extends Destination {

  private final File file;
  private OutputStream lazyStream;

  LazyFileDestination(File file) {
    super((OutputStream) null);
    this.file = Objects.requireNonNull(file, "file");
  }

  @Override
  public synchronized OutputStream stream() {
    if (lazyStream == null) {
      try {
        lazyStream = new FileOutputStream(file);
      } catch (FileNotFoundException e) {
        throw new RuntimeIOException(e);
      }
    }
    return lazyStream;
  }

  @Override
  public Writer writer() {
    return null;
  }

  @Override
  public Writer createWriter() {
    return new OutputStreamWriter(stream());
  }
}
