package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

/** Tests for MIME bundle invariants. */
class MimeBundleTest {
  @Test
  void preservesRichMimeBeforePlainText() {
    MimeBundle bundle = MimeBundle.svg('<svg/>', 'chart')

    assertEquals(['image/svg+xml', 'text/plain'], bundle.keySet().toList())
    assertEquals('chart', bundle['text/plain'])
  }

  @Test
  void createsHtmlAndPlainTextBundles() {
    MimeBundle html = MimeBundle.html('<table/>', 'table')
    MimeBundle plain = MimeBundle.plain('fallback')

    assertEquals(['text/html', 'text/plain'], html.keySet().toList())
    assertEquals('<table/>', html['text/html'])
    assertEquals(['text/plain'], plain.keySet().toList())
    assertEquals('fallback', plain['text/plain'])
  }
}
