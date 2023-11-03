import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrixcsv.CsvImporter

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.*

class CsvImportTest {

  @Test
  void importCsv() {
    URL url = getClass().getResource("/basic.csv")
    CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
    Matrix basic = CsvImporter.importCsv(url, format)
    assertEquals(4, basic.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], basic.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], basic.row(3), "last row")
  }

  @Test
  void csvWithSemiColonsQuoteAndEmptyLine() {
    URL url = getClass().getResource("/colonQuotesEmptyLine.csv")
    CSVFormat format = CSVFormat.Builder.create()
        .setTrim(true)
        .setDelimiter(';')
        .setIgnoreEmptyLines(true)
        .setQuote('"' as Character)
        .setHeader('id', 'name', 'date', 'amount')
        .build()
    Matrix matrix = CsvImporter.importCsv(url, format)
    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], matrix.row(3), "last row")

    Matrix table = matrix.convert(
        ["id": Integer,
         "name": String,
         "date": LocalDate,
         "amount": BigDecimal
        ],
        DateTimeFormatter.ofPattern("yyyy-MMM-dd"),
        NumberFormat.getInstance(Locale.GERMANY)
    )
    assertEquals(4, table.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], table.columnNames(), "Column names")
    assertEquals([4, 'Arne', LocalDate.parse('2023-07-01'), 222.99], table.row(3), "last row")
  }
}
