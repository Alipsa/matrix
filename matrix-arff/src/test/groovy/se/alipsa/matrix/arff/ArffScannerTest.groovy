package se.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

class ArffScannerTest {

  @Test
  void returnsAllIndexesOutsideQuotesInOneScan() {
    assertEquals([1, 7, 9, 15], ArffScanner.indexesOutsideQuotes("a,'b,c',d,\"e,f\",g", ',' as char))
  }
}
