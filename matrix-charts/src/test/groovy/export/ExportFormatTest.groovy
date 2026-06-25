package export

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.chartexport.ExportFormat

class ExportFormatTest {

  @Test
  void testFromExtensionNullReturnsSvg() {
    assertEquals(ExportFormat.SVG, ExportFormat.fromExtension(null))
  }

  @Test
  void testFromExtensionKnownExtensions() {
    assertEquals(ExportFormat.PNG, ExportFormat.fromExtension('png'))
    assertEquals(ExportFormat.PNG, ExportFormat.fromExtension('PNG'))
    assertEquals(ExportFormat.JPEG, ExportFormat.fromExtension('jpg'))
    assertEquals(ExportFormat.JPEG, ExportFormat.fromExtension('jpeg'))
    assertEquals(ExportFormat.JPEG, ExportFormat.fromExtension('JPEG'))
    assertEquals(ExportFormat.PDF, ExportFormat.fromExtension('pdf'))
    assertEquals(ExportFormat.PDF, ExportFormat.fromExtension('PDF'))
    assertEquals(ExportFormat.SVG, ExportFormat.fromExtension('svg'))
    assertEquals(ExportFormat.SVG, ExportFormat.fromExtension('SVG'))
  }

  @Test
  void testFromExtensionUnknownExtensionThrows() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ExportFormat.fromExtension('txt')
    }
    assertTrue(exception.message.contains('.png'))
    assertTrue(exception.message.contains('.jpg'))
    assertTrue(exception.message.contains('.pdf'))
    assertTrue(exception.message.contains('.svg'))
  }

  @Test
  void testFromExtensionEmptyExtensionThrows() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ExportFormat.fromExtension('')
    }
    assertTrue(exception.message.contains('.png'))
    assertTrue(exception.message.contains('.jpg'))
    assertTrue(exception.message.contains('.pdf'))
    assertTrue(exception.message.contains('.svg'))
  }

  @Test
  void testFromFileExtensionlessReturnsSvg() {
    assertEquals(ExportFormat.SVG, ExportFormat.fromFile(new File('chart')))
    assertEquals(ExportFormat.SVG, ExportFormat.fromFile(new File('/path/to/chart')))
  }

  @Test
  void testFromFileTrailingDotThrows() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ExportFormat.fromFile(new File('chart.'))
    }
    assertTrue(exception.message.contains('.png'))
    assertTrue(exception.message.contains('.jpg'))
    assertTrue(exception.message.contains('.pdf'))
    assertTrue(exception.message.contains('.svg'))
  }

}
