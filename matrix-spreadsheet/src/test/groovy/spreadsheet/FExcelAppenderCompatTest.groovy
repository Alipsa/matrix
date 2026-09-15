package spreadsheet

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter

import javax.xml.parsers.DocumentBuilderFactory

class FExcelAppenderCompatTest {

  private static final Matrix DATA = Matrix.builder().data(a: [1, 2], b: ['x', 'y']).build()

  private static File base() {
    XlsxTestUtil.createXlsx { ws -> ws.value(0, 0, 'h'); ws.value(1, 0, 42) }
  }

  @Test
  void prefixedWorkbookXmlIsRecognised() {
    File source = base()
    String workbook = OdsTestUtil.readEntry(source, 'xl/workbook.xml')
    // turn <workbook xmlns="..."> into <x:workbook xmlns:x="..."> with prefixed children
    String prefixed = workbook.replace('xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"',
        'xmlns:x="http://schemas.openxmlformats.org/spreadsheetml/2006/main"')
        .replaceAll('<(/?)(workbook|workbookPr|sheets|sheet|bookViews|workbookView|calcPr)\\b', '<$1x:$2')
    File file = XlsxTestUtil.rewriteEntries(source, ['xl/workbook.xml': prefixed])
    // replace path (readSheets must find <x:sheet>) and add path (createChild must emit <x:sheet>, <Relationship>, <Override>)
    List<String> names = SpreadsheetWriter.writeSheets([DATA, DATA], file, ['Sheet1', 'Extra'])
    assertEquals(['Sheet1', 'Extra'], names)
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      assertEquals(['Sheet1', 'Extra'], reader.sheetNames)
    }
    assertEquals(['a', 'b'], SpreadsheetImporter.importSpreadsheet(file.absolutePath, 'Sheet1', true).columnNames())
    assertEquals(['a', 'b'], SpreadsheetImporter.importSpreadsheet(file.absolutePath, 'Extra', true).columnNames())
    // attribute order is not guaranteed by DOM/Transformer, so parse instead of matching text
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance()
    dbf.namespaceAware = true
    org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(OdsTestUtil.readEntry(file, 'xl/workbook.xml').getBytes('UTF-8')))
    org.w3c.dom.NodeList sheets = doc.getElementsByTagNameNS('http://schemas.openxmlformats.org/spreadsheetml/2006/main', 'sheet')
    List<String> sheetNames = (0..<sheets.length).collect { ((org.w3c.dom.Element) sheets.item(it)).getAttribute('name') }
    assertEquals(['Sheet1', 'Extra'], sheetNames, 'new sheet element must be in the spreadsheetml namespace')
    org.w3c.dom.Element extra = (org.w3c.dom.Element) sheets.item(1)
    assertEquals('sheet', extra.localName)
    assertEquals('x', extra.prefix, 'new element must reuse the document prefix')
    assertFalse(extra.getAttributeNS('http://schemas.openxmlformats.org/officeDocument/2006/relationships', 'id').isEmpty(), 'r:id must be namespace-qualified')
    // a second append must still see both sheets (re-parses the file we just wrote)
    SpreadsheetWriter.write(DATA, file, 'Third')
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      assertEquals(['Sheet1', 'Extra', 'Third'], reader.sheetNames)
    }
  }

  @Test
  void absoluteRelationshipTargetAndNonNumericRelIdAreTolerated() {
    File source = base()
    String rels = OdsTestUtil.readEntry(source, 'xl/_rels/workbook.xml.rels')
    String workbook = OdsTestUtil.readEntry(source, 'xl/workbook.xml')
    // find the sheet relationship id and rewrite it to an absolute target with an unusual id
    def matcher = (rels =~ /Id="(rId\d+)"[^>]*Target="(worksheets\/sheet1\.xml)"/)
    assertTrue(matcher.find(), 'expected a worksheet relationship')
    String oldId = matcher.group(1)
    String newRels = rels.replace("Id=\"$oldId\"", 'Id="R1a2b3c"').replace('Target="worksheets/sheet1.xml"', 'Target="/xl/worksheets/sheet1.xml"')
    String newWorkbook = workbook.replace("r:id=\"$oldId\"", 'r:id="R1a2b3c"')
    File file = XlsxTestUtil.rewriteEntries(source, ['xl/_rels/workbook.xml.rels': newRels, 'xl/workbook.xml': newWorkbook])
    SpreadsheetWriter.writeSheets([DATA], file, ['Extra'])
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      assertEquals(['Sheet1', 'Extra'], reader.sheetNames)
    }
    assertEquals(42, SpreadsheetImporter.importSpreadsheet(file.absolutePath, 'Sheet1', true)[0, 'h'])
  }

}
