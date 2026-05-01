package test.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.core.Matrix

import java.nio.file.Files

class MatrixAvroEdgeCaseTest {

  @Test
  void testEmptyMatrixZeroRowsRoundTrip() {
    Map<String, List<?>> cols = [:]
    cols['id'] = []
    cols['name'] = []

    Matrix m = Matrix.builder('EmptyRows')
        .columns(cols)
        .types(Integer, String)
        .build()

    File tmp = Files.createTempFile('avro-empty-rows-', '.avro').toFile()
    try {
      MatrixAvroWriter.write(m, tmp)
      Matrix result = MatrixAvroReader.read(tmp)

      assertEquals(0, result.rowCount())
      assertEquals(2, result.columnCount())
      assertEquals(['id', 'name'], result.columnNames())
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testSingleRowRoundTrip() {
    Map<String, List<?>> cols = [:]
    cols['id'] = [1]
    cols['name'] = ['Alice']

    Matrix m = Matrix.builder('SingleRow')
        .columns(cols)
        .types(Integer, String)
        .build()

    File tmp = Files.createTempFile('avro-single-row-', '.avro').toFile()
    try {
      MatrixAvroWriter.write(m, tmp)
      Matrix result = MatrixAvroReader.read(tmp)

      assertEquals(1, result.rowCount())
      assertEquals(2, result.columnCount())
      assertEquals(1, result[0, 'id'])
      assertEquals('Alice', result[0, 'name'])
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testSingleColumnRoundTrip() {
    Map<String, List<?>> cols = [:]
    cols['value'] = [1, 2, 3]

    Matrix m = Matrix.builder('SingleColumn')
        .columns(cols)
        .types(Integer)
        .build()

    File tmp = Files.createTempFile('avro-single-col-', '.avro').toFile()
    try {
      MatrixAvroWriter.write(m, tmp)
      Matrix result = MatrixAvroReader.read(tmp)

      assertEquals(3, result.rowCount())
      assertEquals(1, result.columnCount())
      assertEquals([1, 2, 3], result['value'])
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testSpecialCharactersInStringValuesRoundTrip() {
    Map<String, List<?>> cols = [:]
    cols['text'] = [
        'line1\nline2',
        'tab\tvalue',
        'quote "inside"',
        'backslash \\\\',
        'comma, semicolon;'
    ]

    Matrix m = Matrix.builder('SpecialChars')
        .columns(cols)
        .types(String)
        .build()

    File tmp = Files.createTempFile('avro-special-chars-', '.avro').toFile()
    try {
      MatrixAvroWriter.write(m, tmp)
      Matrix result = MatrixAvroReader.read(tmp)

      assertEquals(5, result.rowCount())
      assertNotNull(result['text'])
      assertEquals(cols['text'], result['text'])
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testUnicodeColumnNameRejected() {
    Map<String, List<?>> cols = [:]
    cols['naïve'] = [1, 2]

    Matrix m = Matrix.builder('UnicodeColumns')
        .columns(cols)
        .types(Integer)
        .build()

    File tmp = Files.createTempFile('avro-unicode-col-', '.avro').toFile()
    try {
      def ex = assertThrows(AvroSchemaException) {
        MatrixAvroWriter.write(m, tmp)
      }
      assertEquals('naïve', ex.columnName)
      assertTrue(ex.message.contains('Avro field name'))
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testLargeFileRoundTrip() {
    int rows = 10_000
    Map<String, List<?>> cols = [:]
    cols['id'] = (1..rows).toList()
    cols['value'] = (1..rows).collect { it * 2 }
    cols['name'] = (1..rows).collect { "row${it}" }

    Matrix m = Matrix.builder('LargeFile')
        .columns(cols)
        .types(Integer, Integer, String)
        .build()

    File tmp = Files.createTempFile('avro-large-', '.avro').toFile()
    try {
      MatrixAvroWriter.write(m, tmp)
      Matrix result = MatrixAvroReader.read(tmp)

      assertEquals(rows, result.rowCount())
      assertEquals(3, result.columnCount())
      assertEquals("row${rows}", result[rows - 1, 'name'])
    } finally {
      tmp.delete()
    }
  }

}
