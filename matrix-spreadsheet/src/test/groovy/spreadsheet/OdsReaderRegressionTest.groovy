package spreadsheet

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.fastods.reader.OdsDataReader

class OdsReaderRegressionTest {

  @Test
  void testAnnotationsDoNotLeakIntoCellTextAndHeadingsAreRetained() {
    String xml = OdsTestUtil.contentXml('''
      <table:table table:name="Sheet1">
        <table:table-row>
          <table:table-cell office:value-type="string"><office:annotation><dc:creator>per</dc:creator><text:p>a comment</text:p></office:annotation><text:p>value</text:p></table:table-cell>
          <table:table-cell office:value-type="string"><text:p>line1</text:p><text:p>line2</text:p></table:table-cell>
          <table:table-cell office:value-type="string" xmlns:ext="urn:example:ext"><ext:annotation><text:p>kept</text:p></ext:annotation><text:p>v</text:p></table:table-cell>
          <table:table-cell office:value-type="string"><text:h>heading</text:h></table:table-cell>
        </table:table-row>
      </table:table>''')
    OdsTestUtil.createOds(xml).withInputStream { InputStream input ->
      Sheet sheet = OdsDataReader.create().readOds(input, 'Sheet1', 1, 1, 1, 4)
      assertEquals('value', sheet[0][0])
      assertEquals('line1\nline2', sheet[0][1])
      assertEquals('kept\nv', sheet[0][2])
      assertEquals('heading', sheet[0][3])
    }
  }

}
