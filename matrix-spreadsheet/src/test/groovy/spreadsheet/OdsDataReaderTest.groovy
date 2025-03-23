package spreadsheet;

import org.junit.jupiter.api.Test;
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsDataReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

class OdsDataReaderTest {

  @Test
  void testDataReader() throws IOException {
    try (InputStream is = this.getClass().getResourceAsStream("/Book2.ods")) {
      List<List<?>> rows = OdsDataReader.load(is, 'Sheet1')
      if (rows != null) {
        for (List<?> row : rows) {
          println("$row (${row.size()})")
        }
      } else {
        println("No data found")
      }
    }
  }

  @Test
  void testHugeFile() throws IOException {
    try (InputStream is = this.getClass().getResourceAsStream("/Crime_Data_from_2023.ods")) {
      Instant start = Instant.now()
      List<List<?>> rows = OdsDataReader.load(is, "Sheet1")
      Instant finish = Instant.now()
      System.out.println("Parsing time: " + formatDuration(Duration.between(start, finish)));
      assert 360131 == rows.size()
      int ncols = 0
      for (int i = 0; i < 20; i++) {
        ncols = Math.max(ncols, rows.get(i).size())
      }
      println("Read " + ncols + " columns")
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
