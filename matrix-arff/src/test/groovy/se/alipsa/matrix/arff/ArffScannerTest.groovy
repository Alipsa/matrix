package se.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

class ArffScannerTest {

  @Test
  void returnsAllIndexesOutsideQuotesInOneScan() {
    assertEquals([1, 7, 9, 15], ArffScanner.indexesOutsideQuotes("a,'b,c',d,\"e,f\",g", ',' as char))
  }

  @Test
  void singleLookupsTrackQuotesEscapesAndOffsets() {
    def text = "a,'b,c',d,\"e,f\",g"
    assertEquals(7, ArffScanner.indexOfOutsideQuotes(text, ',' as char, 4))
    assertEquals(9, ArffScanner.indexOfOutsideQuotes(text, ',' as char, 8))
    assertEquals(15, ArffScanner.lastIndexOfOutsideQuotes(text, ',' as char))
    assertEquals(-1, ArffScanner.indexOfOutsideQuotes(text, ',' as char, 16))
    assertEquals(false, ArffScanner.hasUnterminatedQuote(text))
    assertEquals(true, ArffScanner.hasUnterminatedQuote("a,'b,c"))
    assertEquals([1, 8], ArffScanner.indexesOutsideQuotes("a,'b\\'c',d", ',' as char))
  }

  @Test
  void rootPathHasFallbackName() {
    def method = MatrixArffReader.getDeclaredMethod('defaultName', java.nio.file.Path)
    method.accessible = true
    assertEquals('ArffMatrix', method.invoke(null, java.nio.file.Path.of('/')))
  }
}
