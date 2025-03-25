package spreadsheet


import org.junit.jupiter.api.Test
import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsEventDataReader
import static se.alipsa.matrix.core.ValueConverter.*

class FastOdsTest {

  @Test
  void testPosition() {
    Sheet spreadsheet
    URL url = this.getClass().getResource("/positions.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is,
          'Sheet1', 1, 11, 4, 7)
    }
    assert spreadsheet.get(0) == ['d1',	'e1',	'f1', null]
    assert spreadsheet.get(9) == ['d10',	'e10',	'f10', null]
    assert spreadsheet.get(10) == [null,	null,	null, null]
    assert 11 == spreadsheet.size() : "Number of rows is wrong"
  }

  @Test
  void testSheet1() {
    Sheet spreadsheet
    URL url = this.getClass().getResource("/simple.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is,
          'Sheet1', 4, 5, 2, 3)
    }
    assert spreadsheet.get(0) == ['hej hopp', null]
    assert spreadsheet.get(1) == [null, 12345]
    assert 2 == spreadsheet.size() : "Number of rows is wrong"
  }

  @Test
  void testSheet2() {
    Sheet spreadsheet
    URL url = this.getClass().getResource("/simple.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is, 'Sheet2', 4, 7, 2, 8)
    }
    assert spreadsheet.get(2) == [3, asLocalDate('2025-03-21'), 'doo', 12.7, 0.1245, 'baz', false]
    assert spreadsheet.get(3) == [2, null, null, 38, 15.2, null, null]
    assert 4 == spreadsheet.size() : "Number of rows is wrong"
  }
}
