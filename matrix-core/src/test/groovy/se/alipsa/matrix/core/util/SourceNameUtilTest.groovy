package se.alipsa.matrix.core.util

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

class SourceNameUtilTest {

  @Test
  void derivesNamesFromUrls() {
    assertEquals('sales report', SourceNameUtil.matrixName(new URI('https://example.test/a/sales%20report.csv?version=1.2#part').toURL()))
    assertEquals('a+b c', SourceNameUtil.matrixName(new URI('https://example.test/a+b%20c.csv').toURL()))
    assertEquals('archive.data', SourceNameUtil.matrixName(new URI('https://example.test/archive.data.csv').toURL()))
    assertEquals('', SourceNameUtil.matrixName(new URI('https://example.test/').toURL()))
    assertEquals('', SourceNameUtil.matrixName(new URI('https://example.test/path/').toURL()))
    assertEquals('a b', SourceNameUtil.matrixName(new URL('https://example.test/a b.csv?version=1.2#part')))
    assertEquals('a b%20c', SourceNameUtil.matrixName(new URL('https://example.test/a b%20c.csv')))
    assertEquals('re?port', SourceNameUtil.matrixName(new URI('https://example.test/re%3Fport.csv').toURL()))
    assertEquals('re#port', SourceNameUtil.matrixName(new URI('https://example.test/re%23port.csv').toURL()))
    assertEquals('data', SourceNameUtil.matrixName(new URI('jar:file:/tmp/archive.jar!/nested/data.csv').toURL()))
    assertEquals('a?b', SourceNameUtil.matrixName(new URI('https://example.test/a%3Fb.json?q=1').toURL()))
    assertEquals('a#b', SourceNameUtil.matrixName(new URI('https://example.test/a%23b.json').toURL()))
    assertEquals('data', SourceNameUtil.matrixName(new URI('https://example.test/y/data').toURL()))
    assertEquals('', SourceNameUtil.matrixName(new URI('https://example.test').toURL()))
  }

  @Test
  void derivesNamesFromFilesAndPreservesLeadingDots() {
    assertEquals('sales', SourceNameUtil.matrixName(new File('sales.csv')))
    assertEquals('.hidden', SourceNameUtil.stripExtension('.hidden'))
  }
}
