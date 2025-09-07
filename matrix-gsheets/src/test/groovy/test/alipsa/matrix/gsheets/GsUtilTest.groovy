package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static se.alipsa.matrix.gsheets.GsUtil.*
import static org.junit.jupiter.api.Assertions.*

class GsUtilTest {

  @Test
  void testColumnCountForRange() {
    def range = "Arkiv!B2:H100"
    def columns = columnCountForRange(range)
    //println "The number of columns in the range is: ${columns}"
    assertEquals(7, columns)

    // Example with a single-letter range
    def simpleRange = "C:F"
    def simpleColumns = columnCountForRange(simpleRange)
    //println "The number of columns in the simple range is: ${simpleColumns}"
    assertEquals(4, simpleColumns)

    // Example with a multi-letter range
    def multiRange = "A1:AB10"
    def multiColumns = columnCountForRange(multiRange)
    //println "The number of columns in the multi-letter range is: ${multiColumns}"
    assertEquals(28, multiColumns)
  }

}
