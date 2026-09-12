package test.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.arff.ArffWriteOptions
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix

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
}
