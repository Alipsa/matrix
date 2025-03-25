package spreadsheet

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test
import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsDataReader;
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsEventDataReader

import java.time.Duration;
import java.time.Instant

class OdsDataReaderTest {

  @Test
  void testDataReader() throws IOException {
    try (InputStream is = this.getClass().getResourceAsStream("/Book2.ods")) {
      List<List<?>> rows = OdsEventDataReader.create().readOds(is, 'Sheet1', 3, 11, 2, 6)
      /*
      if (rows != null) {
        for (List<?> row : rows) {
          println("$row (${row.size()})")
        }
      } else {
        println("No data found")
      }*/
      assert rows.size() == 9
      for (List<?> row : rows) {
        assert 5 == row.size()
      }
      assert ['id', 'OB', 'IB', 'deferred_interest_amount', 'percentdiff'] == rows.get(0)
      assert [752810, 18609, ',', 0, '#VALUE!'] == rows[8]
    }
  }
  // 24300    WESTERN                      AV
  @Test
  void testPreserveSpace() throws IOException {
    try (InputStream is = this.getClass().getResourceAsStream("/Book2.ods")) {
      Sheet rows = OdsEventDataReader.create().readOds(is, 'Sheet3', 1, 1, 1, 1)
      Assertions.assertEquals('24300    WESTERN                      AV', rows [ 0][ 0])
    }
  }

  @Test
  void testHugeFile() throws IOException {
    int nRows = 360131
    try (InputStream is = this.getClass().getResourceAsStream("/Crime_Data_from_2023.ods")) {
      Instant start = Instant.now()
      Sheet sheet = OdsDataReader.create().readOds(is, "Sheet1", 1, nRows, 1, 28)
      Instant finish = Instant.now()
      System.out.println("Parsing time: " + formatDuration(Duration.between(start, finish)));
      assert nRows == sheet.size()
      int ncols = 0
      for (int i = 0; i < 20; i++) {
        ncols = Math.max(ncols, sheet.get(i).size())
      }
      //println("Read " + ncols + " columns")
      assert ncols >= 28

    }
  }

  private static String formatDuration(Duration d) {
    long days = d.toDays()
    d = d.minusDays(days)
    long hours = d.toHours()
    d = d.minusHours(hours)
    long minutes = d.toMinutes()
    d = d.minusMinutes(minutes)
    long seconds = d.getSeconds()
    d = d.minusSeconds(seconds)
    long millis = d.toMillis()

    (days == 0 ? "" : days + " days,") +
        (hours == 0 ? "" : hours + " hours, ") +
        (minutes == 0 ? "" : minutes + " minutes, ") +
        (seconds == 0 ? "" : seconds + " seconds, ") +
        (millis == 0 ? "" : millis + " millis")
  }

}
