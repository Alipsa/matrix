package spreadsheet


import org.junit.jupiter.api.Test
import se.alipsa.matrix.spreadsheet.fastods.Spreadsheet
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsEventDataReader

class FastOdsTest {

  @Test
  void testPosition() {
    Spreadsheet spreadsheet
    URL url = this.getClass().getResource("/positions.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is, [
          Sheet1: [1, 11, 4, 7],
      ])
    }
    println "got the following sheets"
    spreadsheet.each {
      println it.key
      it.value.each {println it}
    }
  }

  @Test
  void testSheet1() {
    Spreadsheet spreadsheet
    URL url = this.getClass().getResource("/simple.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is, [
          Sheet1: [4, 5, 2, 3],
      ])
    }
    println "got the following sheets"
    spreadsheet.each {
      println it.key
      it.value.each {println it}
    }
  }

  @Test
  void testSheet2() {
    Spreadsheet spreadsheet
    URL url = this.getClass().getResource("/simple.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is, [
          Sheet2: [4, 7, 2, 8]
      ])
    }
    println "got the following sheet"
    spreadsheet.each {
      println it.key
      it.value.each {println it}
    }
  }

  @Test
  void testOdsMultipleSheets() {
    Spreadsheet spreadsheet
    URL url = this.getClass().getResource("/simple.ods")
    try (InputStream is = url.openStream()) {
      spreadsheet = OdsEventDataReader.create().readOds(is, [
          Sheet1: [4, 5, 2, 3],
          Sheet2: [4, 7, 2, 8]
      ])
    }
    println "got the following sheets"
    spreadsheet.each {
      println it.key
      it.value.each {println it}
    }
  }
}
