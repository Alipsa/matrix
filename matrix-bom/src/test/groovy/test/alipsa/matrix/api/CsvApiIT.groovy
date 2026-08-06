package test.alipsa.matrix.api

import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvExporter
import se.alipsa.matrix.csv.CsvImporter
import se.alipsa.matrix.csv.CsvReader

import java.nio.charset.StandardCharsets

import static org.junit.jupiter.api.Assertions.assertEquals

/** Covers CSV import/export, headers, charset, names, and the fluent reader. */
@Tag('csv')
class CsvApiIT implements ApiItSupport {

  @Test
  void csvFormatsAndFluentReaderRoundTrip() {
    Matrix data = Matrix.builder([name: ['Åsa', 'Bo'], value: [1, 2]], [String, Integer], 'csv').build()
    StringWriter writer = new StringWriter()
    CsvExporter.exportToCsv(data, CSVFormat.DEFAULT, writer)
    Matrix fromStream = CsvImporter.importCsv(
        new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8)), CSVFormat.DEFAULT, true,
        StandardCharsets.UTF_8, 'stream')
    assertEquals(['name', 'value'], fromStream.columnNames())
    assertEquals('stream', fromStream.matrixName)
    Matrix fromBuilder = CsvReader.read().matrixName('fluent').fromString(writer.toString())
    assertEquals(data.rowCount(), fromBuilder.rowCount())
    assertEquals('fluent', fromBuilder.matrixName)
  }
}
