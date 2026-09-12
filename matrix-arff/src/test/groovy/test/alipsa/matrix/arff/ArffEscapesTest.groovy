package test.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.arff.ArffEscapes

class ArffEscapesTest {

  @Test
  void escapeMirrorsWekaBackQuoteChars() {
    assertEquals('a\\\\b', ArffEscapes.escape('a\\b'))
    assertEquals("it\\'s", ArffEscapes.escape("it's"))
    assertEquals('say \\"hi\\"', ArffEscapes.escape('say "hi"'))
    assertEquals('tab\\there', ArffEscapes.escape('tab\there'))
    assertEquals('line1\\nline2', ArffEscapes.escape('line1\nline2'))
    assertEquals('cr\\rhere', ArffEscapes.escape('cr\rhere'))
    assertEquals('50\\%', ArffEscapes.escape('50%'))
    assertEquals('plain', ArffEscapes.escape('plain'))
  }

  @Test
  void unescapeDecodesWekaEscapes() {
    assertEquals('\n' as char, ArffEscapes.unescape('n' as char))
    assertEquals('\t' as char, ArffEscapes.unescape('t' as char))
    assertEquals('\r' as char, ArffEscapes.unescape('r' as char))
    assertEquals('%' as char, ArffEscapes.unescape('%' as char))
    assertEquals('\'' as char, ArffEscapes.unescape('\'' as char))
    assertEquals('x' as char, ArffEscapes.unescape('x' as char))
  }

  @Test
  void needsQuotingMirrorsWekaQuote() {
    ['', '?', 'a b', 'a,b', '{a', 'a}', "it's", 'say "x"', 'a\\b', 'a\tb', 'a\nb', 'a\rb', '50%', 'a%b'].each {
      assertTrue(ArffEscapes.needsQuoting(it), "expected quoting for ${it.inspect()}")
    }
    ['plain', 'Iris-setosa', '3.5', 'a_b', 'ÅÄÖ'].each {
      assertFalse(ArffEscapes.needsQuoting(it), "expected no quoting for ${it.inspect()}")
    }
  }

  @Test
  void quoteHelpers() {
    assertEquals("'plain'", ArffEscapes.quote('plain'))
    assertEquals("'it\\'s'", ArffEscapes.quote("it's"))
    assertEquals('plain', ArffEscapes.quoteIfNeeded('plain'))
    assertEquals("'a b'", ArffEscapes.quoteIfNeeded('a b'))
    assertEquals("''", ArffEscapes.quoteIfNeeded(''))
  }
}
