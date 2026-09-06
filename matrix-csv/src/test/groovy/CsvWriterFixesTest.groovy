import static org.junit.jupiter.api.Assertions.*

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvExporter
import se.alipsa.matrix.csv.CsvWriteOptions
import se.alipsa.matrix.csv.CsvWriter

import java.nio.file.Files
import java.nio.file.Path

class CsvWriterFixesTest {

  @TempDir
  Path tempDir

  private final Matrix matrix = Matrix.builder()
      .columnNames(['a', 'b'])
      .rows([['1', '2']])
      .build()

  @Test
  void configuredHeadersAreEmittedExactlyOnceWhenRequested() {
    CSVFormat configured = CSVFormat.Builder.create()
        .setHeader('x', 'y')
        .build()

    assertEquals('x,y\r\n1,2\r\n', CsvWriter.writeString(matrix, configured, true))
    assertEquals('1,2\r\n', CsvWriter.writeString(matrix, configured, false))
    assertFalse(configured.skipHeaderRecord, 'Caller format must not be mutated')

    CSVFormat mismatched = CSVFormat.Builder.create()
        .setHeader('only-one')
        .build()
    assertEquals('a,b\r\n1,2\r\n', CsvWriter.writeString(matrix, mismatched, true))

    File file = tempDir.resolve('configured-file.csv').toFile()
    Path path = tempDir.resolve('configured-path.csv')
    String stringPath = tempDir.resolve('configured-string.csv')
    File exporterFile = tempDir.resolve('configured-exporter.csv').toFile()
    CsvWriter.write(matrix, file, configured, true)
    CsvWriter.write(matrix, path, configured, true)
    CsvWriter.write(matrix, stringPath, configured, true)
    CsvExporter.exportToCsv(matrix, configured, exporterFile, true)
    [file, path.toFile(), new File(stringPath), exporterFile].each { File output ->
      assertEquals('x,y\r\n1,2\r\n', Files.readString(output.toPath()))
    }

    File noHeader = tempDir.resolve('configured-no-header.csv').toFile()
    CsvWriter.write(matrix, noHeader, configured, false)
    assertEquals('1,2\r\n', Files.readString(noHeader.toPath()))
  }

  @Test
  void callerWritersAreFlushedAndRemainOpen() {
    TrackingWriter writer = new TrackingWriter()
    CsvWriter.write(matrix, writer, CSVFormat.DEFAULT, true)
    CsvWriter.write(matrix, writer, CSVFormat.DEFAULT, false)

    assertFalse(writer.closed)
    assertEquals('a,b\r\n1,2\r\n1,2\r\n', writer.toString())

    TrackingPrintWriter printWriter = new TrackingPrintWriter(new StringWriter())
    CsvWriter.write(matrix, CSVFormat.DEFAULT, printWriter, true)
    assertFalse(printWriter.closed)

    TrackingWriter exporterWriter = new TrackingWriter()
    CsvExporter.exportToCsv(matrix, CSVFormat.DEFAULT, exporterWriter, true)
    assertFalse(exporterWriter.closed)
    assertEquals('a,b\r\n1,2\r\n', exporterWriter.toString())

    File file = tempDir.resolve('buffered.csv').toFile()
    PrintWriter buffered = new PrintWriter(new FileWriter(file))
    try {
      CsvWriter.write(matrix, CSVFormat.DEFAULT, buffered, true)
      assertEquals('a,b\r\n1,2\r\n', Files.readString(file.toPath()))
    } finally {
      closeWriter(buffered)
    }
  }

  @Test
  void fluentWriterFlushesButDoesNotCloseCallerWriter() {
    TrackingWriter writer = new TrackingWriter()
    CsvWriter.write(matrix).to(writer)

    assertFalse(writer.closed)
    assertEquals('a,b\n1,2\n', writer.toString())
  }

  @Test
  void suppliedCsvPrinterRemainsCallerOwned() {
    TrackingWriter writer = new TrackingWriter()
    CSVFormat configured = CSVFormat.Builder.create()
        .setHeader('x', 'y')
        .build()
    CSVPrinter printer = new CSVPrinter(writer, configured)

    CsvWriter.write(matrix, printer, false)
    printer.flush()

    assertFalse(writer.closed)
    assertEquals('x,y\r\n1,2\r\n', writer.toString())
    printer.close()
  }

  @Test
  void typedWriteOptionsMustBeNonNull() {
    assertThrows(IllegalArgumentException) {
      CsvWriter.writeString(matrix, (CsvWriteOptions) null)
    }
  }

  private static void closeWriter(PrintWriter writer) {
    writer.close()
  }

  private static class TrackingWriter extends StringWriter {
    boolean closed

    @Override
    void close() throws IOException {
      closed = true
      super.close()
    }
  }

  private static class TrackingPrintWriter extends PrintWriter {
    boolean closed

    TrackingPrintWriter(Writer writer) {
      super(writer)
    }

    @Override
    void close() {
      closed = true
      super.close()
    }
  }
}
