package test.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.ArffWriteOptions
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tests that the ARFF dialect written and read by matrix-arff matches Weka's (weka.core.Utils and
 * weka.core.converters.ArffLoader).
 */
class MatrixArffWekaCompatTest {

  private static List<String> lines(String arff) {
    arff.readLines()
  }

  @Test
  void writerEscapesControlCharactersAndPercentLikeWeka() {
    Matrix m = Matrix.builder('esc')
        .columnNames('s', 'n')
        .columns(['line1\nline2', 'tab\there', 'cr\rx', '50%'], ['a', 'b', 'c', 'x%y'])
        .types([String, String])
        .build()

    String arff = MatrixArffWriter.writeString(m, new ArffWriteOptions().nominalColumns(['n']))
    List<String> out = lines(arff)

    assertTrue(out.contains("@ATTRIBUTE n {a,b,c,'x\\%y'}"), arff)
    assertTrue(out.contains("'line1\\nline2',a"), arff)
    assertTrue(out.contains("'tab\\there',b"), arff)
    assertTrue(out.contains("'cr\\rx',c"), arff)
    assertTrue(out.contains("'50\\%','x\\%y'"), arff)
    assertEquals(m.rowCount() + 6, out.size(), 'every value must stay on one line')
  }

  @Test
  void writerQuotesIdentifiersAndNominalsWithWhitespaceBackslashOrPercent() {
    Matrix m = Matrix.builder('rel\tname')
        .columnNames('col\tumn', 'pct%', 'back\\slash')
        .columns(['a\tb', 'c'], ['x', 'y'], ['p\\q', 'r'])
        .types([String, String, String])
        .build()

    String arff = MatrixArffWriter.writeString(m, new ArffWriteOptions().nominalColumns(['col\tumn', 'pct%', 'back\\slash']))
    List<String> out = lines(arff)

    assertEquals("@RELATION 'rel\\tname'", out[0])
    assertTrue(out.contains("@ATTRIBUTE 'col\\tumn' {'a\\tb',c}"), arff)
    assertTrue(out.contains("@ATTRIBUTE 'pct\\%' {x,y}"), arff)
    assertTrue(out.contains("@ATTRIBUTE 'back\\\\slash' {'p\\\\q',r}"), arff)
    assertTrue(out.contains("'a\\tb',x,'p\\\\q'"), arff)
  }

  @Test
  void readerDecodesWekaEscapesInStringsNominalsAndNames() {
    String arff = '''
@RELATION 'rel\\tname'

@ATTRIBUTE 'col\\tumn' STRING
@ATTRIBUTE 'pct\\%' {'a\\tb',c,'x\\%y','say \\"hi\\"'}

@DATA
'line1\\nline2','a\\tb'
'cr\\rx','say \\"hi\\"'
{0 'tab\\there', 1 'x\\%y'}
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals('rel\tname', m.matrixName)
    assertEquals(['col\tumn', 'pct%'], m.columnNames())
    assertEquals('line1\nline2', m[0, 0])
    assertEquals('a\tb', m[0, 1])
    assertEquals('cr\rx', m[1, 0])
    assertEquals('say "hi"', m[1, 1])
    assertEquals('tab\there', m[2, 0])
    assertEquals('x%y', m[2, 1])
  }

  @Test
  void stringsWithControlCharactersRoundTrip() {
    Matrix m = Matrix.builder('rt')
        .columns(s: ['line1\nline2', 'tab\there', "it's 50%", 'a\\b'])
        .types([String])
        .build()

    Matrix back = MatrixArffReader.readString(MatrixArffWriter.writeString(m))

    assertEquals(m.column('s'), back.column('s'))
  }

  @Test
  void relationLineWithLoneQuoteIsAParseError() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString("@RELATION '\n@ATTRIBUTE a NUMERIC\n@DATA\n1\n")
    }
    assertTrue(e.message.contains('@RELATION'), e.message)
    assertTrue(e.message.contains('line 1'), e.message)
  }

  @Test
  void defaultDateFormatRoundTripsThroughWriterAndReader() {
    Date created = Date.from(Instant.parse('2026-03-18T14:15:16Z'))
    Matrix m = Matrix.builder('dates').columns(created: [created]).types([Date]).build()

    String arff = MatrixArffWriter.writeString(m)
    assertTrue(arff.contains("@ATTRIBUTE created DATE 'yyyy-MM-dd\\'T\\'HH:mm:ss'"), arff)

    Matrix back = MatrixArffReader.readString(arff)
    assertEquals(created, back[0, 'created'])
  }

  @Test
  void wekaStyleEscapedDateFormatIsRead() {
    String arff = '''
@RELATION d
@ATTRIBUTE when date 'yyyy-MM-dd\\'T\\'HH:mm:ss'
@ATTRIBUTE other date "yyyy-MM-dd'T'HH:mm"
@DATA
'2026-03-18T14:15:16','2026-03-18T14:15'
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals(Date.from(Instant.parse('2026-03-18T14:15:16Z')), m[0, 'when'])
    assertEquals(Date.from(Instant.parse('2026-03-18T14:15:00Z')), m[0, 'other'])
  }

  @Test
  void unquotedDateFormatIsRead() {
    String arff = '''
@RELATION d
@ATTRIBUTE when date yyyy-MM-dd
@ATTRIBUTE noFormat DATE
@DATA
2026-03-18,'2026-03-18T14:15:16'
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals(Date.from(Instant.parse('2026-03-18T00:00:00Z')), m[0, 'when'])
    assertEquals(Date.from(Instant.parse('2026-03-18T14:15:16Z')), m[0, 'noFormat'])
  }

  @Test
  void trailingTextAfterDateFormatIsAParseError() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString("@RELATION d\n@ATTRIBUTE when date 'yyyy-MM-dd' extra\n@DATA\n2026-03-18\n")
    }
    assertTrue(e.message.contains('DATE format'), e.message)
    assertTrue(e.message.contains('line 2'), e.message)
  }

  @Test
  void typeStartingWithDateButNotDateIsUnknown() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION d\n@ATTRIBUTE x DATETIME\n@DATA\n1\n',
          new ArffReadOptions().failOnUnknownAttributeType(true))
    }
    assertTrue(e.message.contains("Unknown @ATTRIBUTE type 'DATETIME'"), e.message)
  }

  @Test
  void percentStartsACommentAnywhereOutsideQuotes() {
    String arff = '''
@RELATION comments % relation comment
@ATTRIBUTE n NUMERIC % numeric column
@ATTRIBUTE s STRING
@ATTRIBUTE c {a,b} % nominal
@DATA
1,'50% done',a % trailing data comment
2,'x',b%no space
{0 3, 1 'y'} % sparse comment
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals('comments', m.matrixName)
    assertEquals(['n', 's', 'c'], m.columnNames())
    assertEquals(3, m.rowCount())
    assertEquals('50% done', m[0, 's'])
    assertEquals('a', m[0, 'c'])
    assertEquals('b', m[1, 'c'])
    assertEquals(3 as BigDecimal, m[2, 'n'])
    assertEquals('y', m[2, 's'])
  }

  @Test
  void percentInsideQuotesIsNotAComment() {
    Matrix m = MatrixArffReader.readString("@RELATION 'a % b'\n@ATTRIBUTE 'p % q' STRING\n@DATA\n'50%'\n")

    assertEquals('a % b', m.matrixName)
    assertEquals('p % q', m.columnNames()[0])
    assertEquals('50%', m[0, 0])
  }

  @Test
  void sparseOmittedAttributesTakeArffDefaultValues() {
    String arff = '''
@RELATION sparse_defaults
@ATTRIBUTE score NUMERIC
@ATTRIBUTE count INTEGER
@ATTRIBUTE status {yes,no}
@ATTRIBUTE note STRING
@ATTRIBUTE never STRING
@ATTRIBUTE when DATE 'yyyy-MM-dd'
@DATA
{0 1.5, 2 no}
{1 7, 3 'hello', 5 ?}
{}
{0 ?, 2 ?, 3 'other'}
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals(1.5, m[0, 'score'])
    assertEquals(0, m[0, 'count'])
    assertEquals('no', m[0, 'status'])
    assertEquals('hello', m[0, 'note'], 'omitted STRING resolves to the first explicit value in the column')
    assertNull(m[0, 'never'], 'a column with no explicit value has nothing index 0 can denote')
    assertEquals(new Date(0L), m[0, 'when'])

    assertEquals(BigDecimal.ZERO, m[1, 'score'])
    assertEquals(7, m[1, 'count'])
    assertEquals('yes', m[1, 'status'])
    assertEquals('hello', m[1, 'note'])
    assertNull(m[1, 'when'], 'an explicit ? is missing')

    assertEquals([BigDecimal.ZERO, 0, 'yes', 'hello', null, new Date(0L)], m.row(2))

    assertNull(m[3, 'score'])
    assertEquals(0, m[3, 'count'])
    assertNull(m[3, 'status'])
    assertEquals('other', m[3, 'note'])
  }

  @Test
  void denseValuesCountTowardsTheStringDictionary() {
    String arff = '''
@RELATION dict
@ATTRIBUTE id INTEGER
@ATTRIBUTE note STRING
@DATA
{0 1}
2,'dense first'
{0 3, 1 'sparse later'}
{0 4}
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals(['dense first', 'dense first', 'sparse later', 'dense first'], m.column('note'))
  }

  @Test
  void omittedStringFallbackAppliesOnlyToColumnsWithoutAnyValue() {
    String arff = '''
@RELATION fallback
@ATTRIBUTE note STRING
@ATTRIBUTE never STRING
@DATA
{0 'hello'}
{}
{0 ?, 1 ?}
'''.trim()

    Matrix m = MatrixArffReader.readString(arff, new ArffReadOptions().omittedStringFallback('0'))

    assertEquals(['hello', 'hello', null], m.column('note'), 'a column with a dictionary entry is unaffected')
    assertEquals(['0', '0', null], m.column('never'), 'no dictionary entry: the fallback applies; an explicit ? stays missing')
    assertEquals([null, null, null], MatrixArffReader.readString(arff).column('never'), 'null without the option')
    assertEquals('0', ArffReadOptions.fromMap([omittedStringFallback: '0']).omittedStringFallback)
    assertEquals([omittedStringFallback: '0'], new ArffReadOptions().omittedStringFallback('0').toMap())
  }

  @Test
  void localDateAndLocalDateTimeAreWrittenWithoutZoneShift() {
    TimeZone original = TimeZone.default
    TimeZone.default = TimeZone.getTimeZone('America/New_York')
    try {
      Matrix m = Matrix.builder('ld')
          .columns(d: [LocalDate.of(2026, 3, 18)], dt: [LocalDateTime.of(2026, 3, 18, 0, 30)])
          .types([LocalDate, LocalDateTime])
          .build()

      String arff = MatrixArffWriter.writeString(m, new ArffWriteOptions().dateFormat('yyyy-MM-dd HH:mm'))

      assertTrue(arff.contains("'2026-03-18 00:00','2026-03-18 00:30'"), arff)
    } finally {
      TimeZone.default = original
    }
  }

  @Test
  void nanAndInfinityAreWrittenAsMissing() {
    Matrix m = Matrix.builder('nan')
        .columns(d: [Double.NaN, Double.POSITIVE_INFINITY, 1.5d], f: [Float.NaN, 2.5f, Float.NEGATIVE_INFINITY])
        .types([Double, Float])
        .build()

    String arff = MatrixArffWriter.writeString(m)
    List<String> data = lines(arff).dropWhile { it != '@DATA' }.drop(1)

    assertEquals(['?,?', '?,2.5', '1.5,?'], data)
    Matrix back = MatrixArffReader.readString(arff)
    assertNull(back[0, 'd'])
    assertEquals(1.5, back[2, 'd'])
  }
}
