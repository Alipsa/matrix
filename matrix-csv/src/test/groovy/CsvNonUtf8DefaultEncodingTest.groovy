import static org.junit.jupiter.api.Assertions.*

import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvExporter
import se.alipsa.matrix.csv.CsvWriter

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class CsvNonUtf8DefaultEncodingTest {

  @TempDir
  Path tempDir

  @Test
  void deprecatedFileWriteUsesUtf8IndependentlyOfDefaultCharset() {
    assertNotEquals(StandardCharsets.UTF_8, Charset.defaultCharset())
    Matrix matrix = Matrix.builder()
        .columnNames(['name'])
        .rows([['Åsa']])
        .build()
    File file = tempDir.resolve('file.csv').toFile()
    Path path = tempDir.resolve('path.csv')
    String stringPath = tempDir.resolve('string.csv')
    File exporterFile = tempDir.resolve('exporter.csv').toFile()

    CsvWriter.write(matrix, file)
    CsvWriter.write(matrix, path)
    CsvWriter.write(matrix, stringPath)
    CsvExporter.exportToCsv(matrix, CSVFormat.DEFAULT, exporterFile)

    byte[] expected = 'name\r\nÅsa\r\n'.getBytes(StandardCharsets.UTF_8)
    [file, path.toFile(), new File(stringPath), exporterFile].each { File output ->
      assertArrayEquals(expected, Files.readAllBytes(output.toPath()))
    }
  }
}
