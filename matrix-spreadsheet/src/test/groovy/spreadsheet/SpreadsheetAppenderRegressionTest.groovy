package spreadsheet

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter

class SpreadsheetAppenderRegressionTest {

  private static final Matrix DATA = Matrix.builder().data(a: [1, 2], b: ['x', 'y']).build()

  @Test
  void testAppendMatchesSheetNamesIgnoringCase() {
    ['.xlsx', '.ods'].each { String extension ->
      File file = File.createTempFile('matrix-case', extension)
      file.delete()
      file.deleteOnExit()
      SpreadsheetWriter.write(DATA, file, 'Sheet1')
      assertEquals('Sheet1', SpreadsheetWriter.write(DATA, file, 'SHEET1'))
      SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
        assertEquals(['Sheet1'], reader.sheetNames)
      }
    }
  }

  @Test
  void testXlsxReplacementRemovesCalcChain() {
    File source = XlsxTestUtil.createXlsx { ws -> ws.value(0, 0, 'h'); ws.value(1, 0, 42) }
    String types = OdsTestUtil.readEntry(source, '[Content_Types].xml').replace('</Types>',
        '<Override PartName="/xl/calcChain.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.calcChain+xml"/></Types>')
    String relationships = OdsTestUtil.readEntry(source, 'xl/_rels/workbook.xml.rels').replace('</Relationships>',
        '<Relationship Id="rId99" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/calcChain" Target="calcChain.xml"/></Relationships>')
    File file = XlsxTestUtil.rewriteEntries(source, [
        '[Content_Types].xml': types,
        'xl/_rels/workbook.xml.rels': relationships,
        'xl/calcChain.xml': '<?xml version="1.0"?><calcChain/>'
    ])
    SpreadsheetWriter.write(DATA, file, 'Sheet1')
    new java.util.zip.ZipFile(file).withCloseable { zip -> assertNull(zip.getEntry('xl/calcChain.xml')) }
    assertFalse(OdsTestUtil.readEntry(file, '[Content_Types].xml').contains('calcChain'))
    assertFalse(OdsTestUtil.readEntry(file, 'xl/_rels/workbook.xml.rels').contains('calcChain'))
  }

  @Test
  void testAppendTwoNamesDifferingOnlyByCaseAreSuffixedNotCollapsed() {
    Matrix replacement = Matrix.builder().data(a: [3, 4], b: ['p', 'q']).build()
    Matrix extra = Matrix.builder().data(c: [5, 6]).build()
    ['.xlsx', '.ods'].each { String extension ->
      File file = File.createTempFile('matrix-case2', extension)
      file.delete()
      file.deleteOnExit()
      SpreadsheetWriter.write(DATA, file, 'Foo')
      List<String> names = SpreadsheetWriter.writeSheets([replacement, extra], file, ['foo', 'FOO'])
      assertEquals(['Foo', 'FOO1'], names, extension)
      SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
        assertEquals(['Foo', 'FOO1'], reader.sheetNames, extension)
      }
      assertEquals(['a', 'b'], SpreadsheetImporter.importSpreadsheet(file.absolutePath, 'Foo', true).columnNames(), extension)
      assertEquals(['c'], SpreadsheetImporter.importSpreadsheet(file.absolutePath, 'FOO1', true).columnNames(), extension)
    }
  }

  @Test
  void testOdsAppendDoesNotNestNewTableInsideSheetWithTableLocalNamedExpressions() {
    String xml = OdsTestUtil.contentXml('''
      <table:table table:name="Sheet1">
        <table:table-row><table:table-cell office:value-type="string"><text:p>h</text:p></table:table-cell></table:table-row>
        <table:named-expressions><table:named-range table:name="local" table:base-cell-address="$Sheet1.$A$1" table:cell-range-address="$Sheet1.$A$1:.$A$1"/></table:named-expressions>
      </table:table>''')
    File file = OdsTestUtil.createOds(xml)
    SpreadsheetWriter.write(DATA, file, 'Added')
    String content = OdsTestUtil.readEntry(file, 'content.xml')
    int added = content.indexOf('table:name="Added"')
    int firstTableEnd = content.indexOf('</table:table>')
    assertTrue(added > 0 && firstTableEnd > 0, 'both elements present')
    assertTrue(added > firstTableEnd, 'new table must not be nested inside the existing sheet')
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      assertEquals(['Sheet1', 'Added'], reader.sheetNames)
    }
  }

  @Test
  void testOdsAddsTablesBeforeNamedExpressions() {
    String xml = OdsTestUtil.contentXml('''
      <table:table table:name="Sheet1"><table:table-row><table:table-cell office:value-type="string"><text:p>h</text:p></table:table-cell></table:table-row></table:table>
      <table:named-expressions><table:named-range table:name="MyRange" table:base-cell-address="$Sheet1.$A$1" table:cell-range-address="$Sheet1.$A$1:.$A$1"/></table:named-expressions>''')
    File file = OdsTestUtil.createOds(xml)
    SpreadsheetWriter.write(DATA, file, 'Added')
    String content = OdsTestUtil.readEntry(file, 'content.xml')
    assertTrue(content.indexOf('table:name="Added"') < content.indexOf('<table:named-expressions'))
  }

}
