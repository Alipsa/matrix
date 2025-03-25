package spreadsheet

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.spreadsheet.poi.ExcelImporter
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastods.FOdsImporter
import se.alipsa.matrix.spreadsheet.sods.SOdsImporter

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.assertEquals
import static se.alipsa.matrix.core.ValueConverter.*

class LargeFileImportTest {

  int nrows = 360131 // including header row

  String datePattern = 'MM/dd/yyyy'
  DateTimeFormatter dtf = DateTimeFormatter.ofPattern(datePattern)

  List<String> colNames = ['DR_NO', 'Date Rptd',	'DATE OCC',	'TIME OCC',	'AREA',	'AREA NAME',
                           'Rpt Dist No',	'Part 1-2',	'Crm Cd',	'Crm Cd Desc', 'Mocodes',	'Vict Age',
                           'Vict Sex',	'Vict Descent',	'Premis Cd',	'Premis Desc',	'Weapon Used Cd',
                           'Weapon Desc',	'Status',	'Status Desc',	'Crm Cd 1',	'Crm Cd 2',	'Crm Cd 3',
                           'Crm Cd 4', 'LOCATION', 'Cross Street', 'LAT', 'LON']

  List lastRow = [250504051,	asLocalDate('01/14/2025', dtf),
                  asLocalDate('01/14/2025', dtf),	1250,	5,	'Harbor',	509,	1,
                  210,	'ROBBERY',	asBigDecimal(1822_0344_1259),	15,	'F', 'H',	721,	'HIGH SCHOOL', 400,
                  'STRONG-ARM (HANDS, FIST, FEET OR BODILY FORCE)',	'IC',	'Invest Cont', 210,
                  null, null, null, '24300    WESTERN                      AV', null,	33.8046, -118.3074]

  /**
   * This test causes OutOfMemoryError which is
   * precisely why the fastods implementation was created
   */
  @Disabled
  @Test
  void testImportWithODSImporter() {
    assert colNames.size() == lastRow.size()
    URL url = this.getClass().getResource('/Crime_Data_from_2023.ods')
    println "importing $url"
    Instant start = Instant.now()
    def matrix = SOdsImporter.importOds(url, 1, 1, nrows, 'A', 'AB', true)
    Instant finish = Instant.now()
    println "Parsing time: ${formatDuration(Duration.between(start, finish))}"
    checkAssertions(matrix)
  }


  @Test
  void testImportWithFastOdsImporter() {
    assert colNames.size() == lastRow.size()
    URL url = this.getClass().getResource('/Crime_Data_from_2023.ods')
    println "importing $url"
    Instant start = Instant.now()
    def matrix = FOdsImporter.importOds(url, 1, 1, nrows, 'A', 'AB', true)
    Instant finish = Instant.now()
    println "Parsing time: ${formatDuration(Duration.between(start, finish))}"
    checkAssertions(matrix)
  }

  @Test
  void testImportFromFastExcel() {
    assert colNames.size() == lastRow.size()
    URL url = this.getClass().getResource('/Crime_Data_from_2023.xlsx')
    println "importing $url"
    Instant start = Instant.now()
    def matrix = FExcelImporter.importExcel(url, 1, 1, nrows, 'A', 'AB', true)
    Instant finish = Instant.now()
    println "Parsing time: ${formatDuration(Duration.between(start, finish))}"
    checkAssertions(matrix)
  }

  /**
   * This test causes OutOfMemoryError which is
   * precisely why the fastexcel implementation was created
   */
  @Disabled
  @Test
  void testImportFromPoi() {
    assert colNames.size() == lastRow.size()
    URL url = this.getClass().getResource('/Crime_Data_from_2023.xlsx')
    println "importing $url"
    Instant start = Instant.now()
    def matrix = ExcelImporter.importExcel(url, 1, 1, nrows, 'A', 'AB', true)
    Instant finish = Instant.now()
    println "Parsing time: ${formatDuration(Duration.between(start, finish))}"
    checkAssertions(matrix)
  }

  private static String formatDuration(Duration duration) {
    List<String> parts = new ArrayList<>();
    long days = duration.toDaysPart();
    if (days > 0) {
      parts.add(plural(days, "day"));
    }
    int hours = duration.toHoursPart();
    if (hours > 0 || !parts.isEmpty()) {
      parts.add(plural(hours, "hour"));
    }
    int minutes = duration.toMinutesPart();
    if (minutes > 0 || !parts.isEmpty()) {
      parts.add(plural(minutes, "minute"));
    }
    int seconds = duration.toSecondsPart();
    parts.add(plural(seconds, "second"));
    return String.join(", ", parts);
  }

  private static String plural(long num, String unit) {
    return num + " " + unit + (num == 1 ? "" : "s");
  }

  void checkAssertions(Matrix matrix) {
    println "Converting datatypes"
    matrix.convert(
        'DR_NO': Integer,
        'Date Rptd': LocalDate,
        'DATE OCC': LocalDate,
        'TIME OCC': Integer,
        'AREA': Integer,
        'Rpt Dist No': Integer,
        'Part 1-2': Integer,
        'Crm Cd': Integer,
        'Vict Age': Integer,
        'Premis Cd': Integer,
        'Weapon Used Cd': Integer,
        'Crm Cd 1': Integer,
        'Crm Cd 2': Integer,
        'Crm Cd 3': Integer,
        datePattern
    )

    assert matrix.rowCount() == nrows -1
    assert matrix.columnNames() == colNames

    Row mRow = matrix.row(matrix.lastRowIndex())
    //println mRow
    mRow.eachWithIndex {it, idx ->
      def expected = lastRow[idx]
      println "expected $expected ${expected?.class} got $it ${it?.class}"
      if (expected instanceof BigDecimal || it instanceof BigDecimal) {
        assertEquals(expected as Double, it as Double, 0.00001, "diff on column $idx")
      } else {
        assertEquals(lastRow[idx], it, "diff on column $idx")
      }
    }
  }
}
