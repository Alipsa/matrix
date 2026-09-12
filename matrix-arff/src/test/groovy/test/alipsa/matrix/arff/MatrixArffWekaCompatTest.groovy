package test.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
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

  @Test
  void dateWithTrailingTextIsRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString("@RELATION d\n@ATTRIBUTE when DATE 'yyyy-MM-dd'\n@DATA\n'2026-03-18garbage'\n")
    }
    assertTrue(e.message.contains("Invalid DATE value '2026-03-18garbage'"), e.message)
    assertTrue(e.message.contains('line 4'), e.message)
  }

  @Test
  void duplicateAttributeNamesAreRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString("@RELATION d\n@ATTRIBUTE a NUMERIC\n@ATTRIBUTE a STRING\n@DATA\n1,'x'\n")
    }
    assertTrue(e.message.contains("Duplicate @ATTRIBUTE name 'a'"), e.message)
    assertTrue(e.message.contains('line 3'), e.message)
  }

  @Test
  void emptyNominalDeclarationIsRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION d\n@ATTRIBUTE a {}\n@DATA\n?\n')
    }
    assertTrue(e.message.contains('at least one value'), e.message)
    assertTrue(e.message.contains('line 2'), e.message)
  }

  @Test
  void integerAttributeAcceptsIntegralDecimalsAndRejectsFractions() {
    Matrix m = MatrixArffReader.readString('@RELATION d\n@ATTRIBUTE n INTEGER\n@DATA\n35.0\n-7\n4E1\n')
    assertEquals([35, -7, 40], m.column('n'))
    assertEquals(Integer, m.type('n'))

    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION d\n@ATTRIBUTE n INTEGER\n@DATA\n3.5\n')
    }
    assertTrue(e.message.contains("Invalid INTEGER value '3.5'"), e.message)

    IllegalArgumentException overflow = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION d\n@ATTRIBUTE n INTEGER\n@DATA\n3000000000\n')
    }
    assertTrue(overflow.message.contains("Invalid INTEGER value '3000000000'"), overflow.message)
  }

  @Test
  void instanceWeightsAreReadIntoTheConfiguredColumn() {
    String arff = '''
@RELATION weighted
@ATTRIBUTE a NUMERIC
@ATTRIBUTE b {x,y}
@DATA
1,x,{5}
2,y
{0 3, 1 y}, {0.5}
{1 y} , {2}
4,x,{0}
'''.trim()

    Matrix m = MatrixArffReader.readString(arff, new ArffReadOptions().instanceWeightColumn('weight'))

    assertEquals(['a', 'b', 'weight'], m.columnNames())
    assertEquals(BigDecimal, m.type('weight'))
    assertEquals(['5', '1', '0.5', '2', '0'], m.column('weight')*.toString(), 'an explicit {0} is zero, not the absent-weight default')
    assertEquals(3 as BigDecimal, m[2, 'a'])
    assertEquals(BigDecimal.ZERO, m[3, 'a'])
    assertEquals('y', m[3, 'b'])
  }

  @Test
  void instanceWeightsAreDiscardedByDefaultAndAreNotRowValues() {
    String arff = '@RELATION w\n@ATTRIBUTE a NUMERIC\n@ATTRIBUTE b {x,y}\n@DATA\n1,x,{5}\n{0 2}, {3}\n'

    Matrix lenient = MatrixArffReader.readString(arff)
    assertEquals(['a', 'b'], lenient.columnNames())
    assertEquals(2, lenient.rowCount())

    Matrix strict = MatrixArffReader.readString(arff, new ArffReadOptions().strict(true))
    assertEquals(2, strict.rowCount())
  }

  @Test
  void instanceWeightColumnMustNotClashWithAnAttribute() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION w\n@ATTRIBUTE weight NUMERIC\n@DATA\n1\n',
          new ArffReadOptions().instanceWeightColumn('weight'))
    }
    assertTrue(e.message.contains("instanceWeightColumn 'weight'"), e.message)
    assertTrue(e.message.contains("in relation 'w'"), e.message)
  }

  @Test
  void nonNumericBraceGroupIsNotAWeight() {
    Matrix m = MatrixArffReader.readString('@RELATION w\n@ATTRIBUTE a NUMERIC\n@DATA\n1,{abc}\n')
    assertEquals(BigDecimal.ONE, m[0, 'a'])

    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION w\n@ATTRIBUTE a NUMERIC\n@DATA\n1,{abc}\n', new ArffReadOptions().strict(true))
    }
    assertTrue(e.message.contains('Row length mismatch'), e.message)
  }

  @Test
  void wekaAttributeWeightsAreIgnored() {
    String arff = '''
@RELATION aw
@ATTRIBUTE a NUMERIC {0.5}
@ATTRIBUTE b {x,y} {2}
@ATTRIBUTE d date 'yyyy' {3}
@ATTRIBUTE s STRING {1.5}
@DATA
1,y,2026,'t'
'''.trim()

    Matrix m = MatrixArffReader.readString(arff, new ArffReadOptions().strict(true))

    assertEquals([BigDecimal, String, Date, String], m.types())
    assertEquals('y', m[0, 'b'])
    assertEquals(Date.from(Instant.parse('2026-01-01T00:00:00Z')), m[0, 'd'])
  }

  @Test
  void instanceWeightColumnIsWrittenAsTrailingBraceGroup() {
    Matrix m = Matrix.builder('w')
        .columns(a: [1, 2, 3, 4], w: [5, null, 0.5, 0])
        .types([Integer, BigDecimal])
        .build()

    String arff = MatrixArffWriter.writeString(m, new ArffWriteOptions().instanceWeightColumn('w'))
    List<String> out = lines(arff)

    assertFalse(out.any { it.startsWith('@ATTRIBUTE w') }, arff)
    assertEquals(['1,{5}', '2', '3,{0.5}', '4,{0}'], out.dropWhile { it != '@DATA' }.drop(1))

    Matrix back = MatrixArffReader.readString(arff, new ArffReadOptions().instanceWeightColumn('w'))
    assertEquals(['5', '1', '0.5', '0'], back.column('w')*.toString())

    Matrix integerWeights = Matrix.builder('iw').columns(a: [1, 2], w: [5, 0]).types([Integer, Integer]).build()
    String integerArff = MatrixArffWriter.writeString(
        integerWeights, new ArffWriteOptions().instanceWeightColumn('w'))
    assertEquals(['1,{5}', '2,{0}'], integerArff.readLines().dropWhile { it != '@DATA' }.drop(1))
  }

  @Test
  void instanceWeightColumnMustBeNumeric() {
    Matrix m = Matrix.builder('w').columns(a: [1], w: ['x']).types([Integer, String]).build()

    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffWriter.writeString(m, new ArffWriteOptions().instanceWeightColumn('w'))
    }
    assertTrue(e.message.contains("instanceWeightColumn 'w'"), e.message)

    Matrix lateInvalid = Matrix.builder('w').columns(a: [1, 2, 3], w: [1, 'x', Double.NaN]).types([Integer, Object]).build()
    StringWriter destination = new StringWriter()
    IllegalArgumentException late = assertThrows(IllegalArgumentException) {
      MatrixArffWriter.write(lateInvalid, destination, new ArffWriteOptions().instanceWeightColumn('w'))
    }
    assertTrue(late.message.contains("instanceWeightColumn 'w'"), late.message)
    assertTrue(late.message.contains('row 1'), late.message)
    assertEquals('', destination.toString(), 'nothing may be written when validation fails')

    Matrix nonFinite = Matrix.builder('w').columns(a: [1, 2], w: [1, Double.NaN]).types([Integer, Object]).build()
    IllegalArgumentException nan = assertThrows(IllegalArgumentException) {
      MatrixArffWriter.writeString(nonFinite, new ArffWriteOptions().instanceWeightColumn('w'))
    }
    assertTrue(nan.message.contains('row 1'), nan.message)
  }

  @Test
  void relationalAttributesAreReadAsNestedMatrices() {
    String arff = '''
@RELATION musk
@ATTRIBUTE molecule {MUSK-1,NON-MUSK-2}
@ATTRIBUTE bag relational
  @ATTRIBUTE f1 NUMERIC
  @ATTRIBUTE f2 {a,b}
@end bag
@ATTRIBUTE class {0,1}
@DATA
MUSK-1,"1,a\\n2,b",1
NON-MUSK-2,'3,b',0
MUSK-1,?,1
{0 NON-MUSK-2, 1 '{1 b}\\n4,a', 2 1}
'''.trim()

    Matrix m = MatrixArffReader.readString(arff)

    assertEquals(['molecule', 'bag', 'class'], m.columnNames())
    assertEquals([String, Matrix, String], m.types())

    Matrix bag0 = m[0, 'bag'] as Matrix
    assertEquals('bag', bag0.matrixName)
    assertEquals(['f1', 'f2'], bag0.columnNames())
    assertEquals([BigDecimal, String], bag0.types())
    assertEquals(2, bag0.rowCount())
    assertEquals(2 as BigDecimal, bag0[1, 'f1'])
    assertEquals('b', bag0[1, 'f2'])

    assertEquals(1, (m[1, 'bag'] as Matrix).rowCount())
    assertNull(m[2, 'bag'])

    Matrix bag3 = m[3, 'bag'] as Matrix
    assertEquals([BigDecimal.ZERO, 'b'], bag3.row(0))
    assertEquals([4 as BigDecimal, 'a'], bag3.row(1))
    assertEquals('1', m[3, 'class'])
  }

  @Test
  void nestedRelationalDeclarationsAndEmptyValuesAreRead() {
    String arff = '''
@RELATION nested
@ATTRIBUTE outer RELATIONAL
  @ATTRIBUTE id INTEGER
  @ATTRIBUTE inner relational
    @ATTRIBUTE v NUMERIC
  @END 'inner'
@END OUTER
@DATA
'1,\\'2\\\\n3\\'\\n4,\\'\\''
''
'''.trim()

    // The first data line is the ARFF text  '1,\'2\\n3\'\n4,\'\''  — the inner value '2\n3' escaped once more by the
    // outer quoting, exactly as Weka writes nested relations. It decodes to the two outer rows  1,'2\n3'  and  4,''.

    Matrix m = MatrixArffReader.readString(arff)

    Matrix outer = m[0, 'outer'] as Matrix
    assertEquals([Integer, Matrix], outer.types())
    assertEquals(2, outer.rowCount())
    assertEquals([2 as BigDecimal, 3 as BigDecimal], (outer[0, 'inner'] as Matrix).column('v'))
    assertEquals(0, (outer[1, 'inner'] as Matrix).rowCount())
    assertEquals(0, (m[1, 'outer'] as Matrix).rowCount())
  }

  @Test
  void relationalDeclarationErrorsHaveLineContext() {
    IllegalArgumentException mismatch = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION r\n@ATTRIBUTE bag relational\n@ATTRIBUTE f NUMERIC\n@END other\n@DATA\n')
    }
    assertTrue(mismatch.message.contains('must be terminated by @END bag'), mismatch.message)
    assertTrue(mismatch.message.contains('line 4'), mismatch.message)

    IllegalArgumentException unterminated = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION r\n@ATTRIBUTE bag relational\n@ATTRIBUTE f NUMERIC\n@DATA\n')
    }
    assertTrue(unterminated.message.contains("'bag' is not terminated"), unterminated.message)

    IllegalArgumentException stray = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION r\n@ATTRIBUTE f NUMERIC\n@END f\n@DATA\n')
    }
    assertTrue(stray.message.contains('@END without'), stray.message)

    IllegalArgumentException empty = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION r\n@ATTRIBUTE bag relational\n@END bag\n@DATA\n')
    }
    assertTrue(empty.message.contains('declares no attributes'), empty.message)

    IllegalArgumentException duplicate = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION r\n@ATTRIBUTE bag relational\n@ATTRIBUTE f NUMERIC\n@ATTRIBUTE f STRING\n@END bag\n@DATA\n')
    }
    assertTrue(duplicate.message.contains("Duplicate @ATTRIBUTE name 'f'"), duplicate.message)
  }

  @Test
  void instanceWeightColumnIsReservedInSubRelations() {
    String arff = "@RELATION r\n@ATTRIBUTE bag relational\n@ATTRIBUTE w NUMERIC\n@END bag\n@DATA\n'1'\n"

    Matrix plain = MatrixArffReader.readString(arff)
    assertEquals(['w'], (plain[0, 'bag'] as Matrix).columnNames(), 'without the option w is an ordinary attribute')

    IllegalArgumentException clash = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arff, new ArffReadOptions().instanceWeightColumn('w'))
    }
    assertTrue(clash.message.contains("instanceWeightColumn 'w'"), clash.message)
    assertTrue(clash.message.contains("in relation 'bag'"), clash.message)

    Matrix renamed = MatrixArffReader.readString(arff, new ArffReadOptions().instanceWeightColumn('weight'))
    assertEquals(['w', 'weight'], (renamed[0, 'bag'] as Matrix).columnNames())

    String noValues = '@RELATION r\n@ATTRIBUTE bag relational\n@ATTRIBUTE w NUMERIC\n@END bag\n@DATA\n?\n'
    assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(noValues, new ArffReadOptions().instanceWeightColumn('w'))
    }
  }

  @Test
  void unterminatedRelationalScopeIsRejectedAtEndOfHeaderOnlyFile() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@RELATION r\n@ATTRIBUTE bag relational\n@ATTRIBUTE f NUMERIC\n')
    }
    assertTrue(e.message.contains("'bag' is not terminated"), e.message)
  }
}
