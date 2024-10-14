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

    Matrix b = CsvImporter.importCsv((CsvImporter.Format.Trim): true, url)
    assertEquals(4, b.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], b.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], b.row(3), "last row")

    Matrix b2 = CsvImporter.importCsv(Trim: true, url)
    assertEquals(4, b2.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], b2.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], b2.row(3), "last row")
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

    Matrix m = CsvImporter.importCsv(
        (CsvImporter.Format.Trim): true,
        (CsvImporter.Format.Delimiter): ';',
        (CsvImporter.Format.IgnoreEmptyLines): true,
        (CsvImporter.Format.Quote): '"',
        (CsvImporter.Format.Header): ['id', 'name', 'date', 'amount'],
        url)
    assertEquals(4, m.rowCount(), "Number of rows \\n ${m.content()}")
    assertEquals(['id', 'name', 'date', 'amount'], m.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], m.row(3), "last row")

    Matrix m2 = CsvImporter.importCsv(
        Trim: true,
        Delimiter: ';',
        IgnoreEmptyLines: true,
        Quote: '"',
        Header: ['id', 'name', 'date', 'amount'],
        url)
    assertEquals(4, m2.rowCount(), "Number of rows: \n ${m2.content()}")
    assertEquals(['id', 'name', 'date', 'amount'], m2.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], m2.row(3), "last row")

    Matrix table = matrix.clone().convert(
        ["id": Integer,
         "name": String,
         "date": LocalDate,
         "amount": BigDecimal
        ],
        "yyyy-MMM-dd",
        NumberFormat.getInstance(Locale.GERMANY)
    )
    assertEquals(4, table.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], table.columnNames(), "Column names")
    assertEquals([4, 'Arne', LocalDate.parse('2023-07-01'), 222.99], table.row(3), "last row")
  }
}
