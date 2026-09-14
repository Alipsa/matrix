package spreadsheet

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TemporalRoundTripTest {

  private static final LocalDateTime LDT = LocalDateTime.of(2024, 3, 5, 6, 7, 8)
  private static final Date UTIL_DATE = Date.from(LDT.atZone(ZoneId.systemDefault()).toInstant())

  private static File tmp(String extension) {
    File file = File.createTempFile('matrix-temporal', extension)
    file.delete()
    file.deleteOnExit()
    file
  }

  private static Matrix dates() {
    Matrix.builder().data(
        z: [ZonedDateTime.of(LDT, ZoneId.systemDefault())],
        o: [OffsetDateTime.of(LDT, ZoneOffset.ofHours(5))],
        d: [UTIL_DATE],
        s: [new java.sql.Date(UTIL_DATE.time)],
        t: [new java.sql.Timestamp(UTIL_DATE.time)],
        w: [new java.sql.Time(UTIL_DATE.time)]
    ).types(ZonedDateTime, OffsetDateTime, Date, java.sql.Date, java.sql.Timestamp, java.sql.Time).build()
  }

  @Test
  void writesAndAppendsTemporalTypesConsistently() {
    ['.ods', '.xlsx'].each { String extension ->
      File file = tmp(extension)
      SpreadsheetWriter.write(dates(), file, 'first')
      SpreadsheetWriter.write(dates(), file, 'second')
      ['first', 'second'].each { String sheet ->
        Matrix matrix = SpreadsheetImporter.importSpreadsheet(file.absolutePath, sheet, true)
        ['z', 'o', 'd', 's', 't', 'w'].each { String column ->
          assertEquals(LDT, matrix[0, column], "$extension $sheet $column")
        }
      }
    }
  }

  @Test
  void odsReaderAcceptsOffsetDateValues() {
    String xml = OdsTestUtil.contentXml('''
      <table:table table:name="Sheet1">
        <table:table-row><table:table-cell office:value-type="string"><text:p>when</text:p></table:table-cell></table:table-row>
        <table:table-row><table:table-cell office:value-type="date" office:date-value="2024-03-05T06:07:08+02:00"/></table:table-row>
        <table:table-row><table:table-cell office:value-type="date" office:date-value="2024-03-05T06:07:08Z"/></table:table-row>
      </table:table>''')
    Matrix matrix = SpreadsheetImporter.importSpreadsheet(OdsTestUtil.createOds(xml))
    assertEquals(LDT, matrix[0, 'when'])
    assertEquals(LDT, matrix[1, 'when'])
  }

}
