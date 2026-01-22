import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.csv.CsvExporter
import org.apache.commons.csv.CSVFormat
import static org.junit.jupiter.api.Assertions.*

class CsvExporterTest {

  @Test
  void exportCsv() {
    StringWriter writer = new StringWriter()
    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.DEFAULT, writer)
    def content = writer.toString().split("\r\n")
    assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    assertEquals('Volvo 142E,21.4,4,121,109,4.11,2.78,18.6,1,1,4,2', content[content.length -1])
  }

  @Test
  void exportToFile() {
    File file = File.createTempFile('mtcars', '.csv')
    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.MYSQL, file)
    def content = file.text.split("\n")
    assertEquals('model\tmpg\tcyl\tdisp\thp\tdrat\twt\tqsec\tvs\tam\tgear\tcarb', content[0])
    assertEquals('Volvo 142E\t21.4\t4\t121\t109\t4.11\t2.78\t18.6\t1\t1\t4\t2', content[content.length -1])
    file.delete()

    file = File.createTempFile('mtcars', '.csv')
    CsvExporter.exportToCsv(Dataset.mtcars(), file)
    content = file.text.split("\r\n")
    assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    assertEquals('Volvo 142E,21.4,4,121,109,4.11,2.78,18.6,1,1,4,2', content[content.length -1])
    file.delete()
  }

  // Phase 2a: Null/Invalid Input Tests

  @Test
  void testNullMatrixValidation() {
    StringWriter writer = new StringWriter()
    NullPointerException ex = assertThrows(NullPointerException) {
      CsvExporter.exportToCsv(null, writer)
    }
    // NullPointerException is acceptable for null input
  }

  @Test
  void testEmptyMatrixCanExport() {
    // Empty matrix with columns should be exportable (just headers)
    def matrix = se.alipsa.matrix.core.Matrix.builder()
        .matrixName('empty')
        .data(id: [], name: [])
        .build()

    StringWriter writer = new StringWriter()
    CsvExporter.exportToCsv(matrix, writer)

    def content = writer.toString().trim()
    assertEquals('id,name', content, "Should export just headers for empty matrix")
  }

  // Phase 2c: Export Variations

  @Test
  void testExportWithoutHeader() {
    StringWriter writer = new StringWriter()
    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.DEFAULT, writer, false)
    def content = writer.toString().split("\r\n")
    // First line should be data, not headers
    assertTrue(content[0].startsWith('Mazda'), "First line should be data when withHeader=false")
    assertFalse(content[0].contains('model'), "Should not contain header row")
  }

  @Test
  void testExportToDirectoryCreatesFile() {
    File tempDir = File.createTempDir('csv-test', '')
    try {
      // Export to directory - should create file named after matrix
      CsvExporter.exportToCsv(Dataset.mtcars(), tempDir)

      // Check the file exists
      File[] files = tempDir.listFiles()
      assertNotNull(files, "Directory should contain files")
      assertTrue(files.length > 0, "Directory should not be empty")

      File exportedFile = new File(tempDir, 'mtcars.csv')
      assertTrue(exportedFile.exists(), "File should be created in directory with matrix name")

      def content = exportedFile.text.split("\r\n")
      assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    } finally {
      tempDir.deleteDir()
    }
  }

  @Test
  void testExportToDirectoryWithCustomMatrixName() {
    File tempDir = File.createTempDir('csv-test', '')
    try {
      def matrix = Dataset.mtcars().clone()
      matrix.matrixName = 'custom-name'

      CsvExporter.exportToCsv(matrix, tempDir)

      File exportedFile = new File(tempDir, 'custom-name.csv')
      assertTrue(exportedFile.exists(), "File should use matrix name: ${tempDir.listFiles()*.name}")

      def content = exportedFile.text.split("\r\n")
      assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    } finally {
      tempDir.deleteDir()
    }
  }

  @Test
  void testExportToPrintWriter() {
    StringWriter stringWriter = new StringWriter()
    PrintWriter printWriter = new PrintWriter(stringWriter)

    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.DEFAULT, printWriter)
    printWriter.flush()

    def content = stringWriter.toString().split("\r\n")
    assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
  }

  @Test
  void testExportWithCustomDelimiter() {
    StringWriter writer = new StringWriter()
    CSVFormat format = CSVFormat.Builder.create()
        .setDelimiter(';')
        .build()

    CsvExporter.exportToCsv(Dataset.mtcars(), format, writer)
    def content = writer.toString().split("\r\n")
    assertTrue(content[0].contains(';'), "Should use semicolon delimiter")
    assertEquals('model;mpg;cyl;disp;hp;drat;wt;qsec;vs;am;gear;carb', content[0])
  }

  @Test
  void testExportWithCustomQuote() {
    StringWriter writer = new StringWriter()
    CSVFormat format = CSVFormat.Builder.create()
        .setQuote('\'' as Character)
        .build()

    // Create matrix with values that need quoting
    def matrix = se.alipsa.matrix.core.Matrix.builder()
        .matrixName('test')
        .data(
            name: ['John, Jr.', 'Jane'],
            value: [100, 200]
        )
        .build()

    CsvExporter.exportToCsv(matrix, format, writer)
    def content = writer.toString()
    assertTrue(content.contains("'"), "Should use single quote character")
  }

  @Test
  void testExportMatrixWithNoName() {
    File tempDir = File.createTempDir('csv-test', '')
    try {
      def matrix = se.alipsa.matrix.core.Matrix.builder()
          .data(id: [1, 2], value: [10, 20])
          .build()

      CsvExporter.exportToCsv(matrix, tempDir)

      // Should use default name 'matrix.csv'
      File exportedFile = new File(tempDir, 'matrix.csv')
      assertTrue(exportedFile.exists(), "File should use default name 'matrix.csv'")
    } finally {
      tempDir.deleteDir()
    }
  }

  // Phase 4: API Enhancements

  @Test
  void testExportToExcelCsv() {
    File file = File.createTempFile('excel', '.csv')
    try {
      CsvExporter.exportToExcelCsv(Dataset.mtcars(), file)
      def content = file.text.split("\r\n")
      // Excel format uses comma delimiter and CRLF line endings
      assertTrue(content[0].contains(','), "Should use comma delimiter")
      assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
      assertTrue(content[content.length - 1].startsWith('Volvo 142E'), "Last row should be Volvo")
    } finally {
      file.delete()
    }
  }

  @Test
  void testExportToTsv() {
    File file = File.createTempFile('data', '.tsv')
    try {
      CsvExporter.exportToTsv(Dataset.mtcars(), file)
      def content = file.text
      // TDF format uses tab delimiter
      assertTrue(content.contains('\t'), "Should use tab delimiter")
      assertTrue(content.startsWith('model\tmpg\tcyl\tdisp\thp\tdrat\twt\tqsec\tvs\tam\tgear\tcarb'),
          "Should start with header row")
      assertTrue(content.contains('Volvo 142E'), "Should contain Volvo data")
    } finally {
      file.delete()
    }
  }

  @Test
  void testExportToExcelCsvWithoutHeader() {
    File file = File.createTempFile('excel-noheader', '.csv')
    try {
      CsvExporter.exportToExcelCsv(Dataset.mtcars(), file, false)
      def content = file.text.split("\r\n")
      // First line should be data, not headers
      assertTrue(content[0].startsWith('Mazda'), "First line should be data when withHeader=false")
      assertFalse(content[0].contains('model'), "Should not contain header row")
    } finally {
      file.delete()
    }
  }
}
