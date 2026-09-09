package io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.BigDecimalColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.ShortColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.column.numbers.BigDecimalColumnType;
import tech.tablesaw.io.RuntimeIOException;
import tech.tablesaw.io.ods.OdsReadOptions;
import tech.tablesaw.io.ods.OdsWriteOptions;
import tech.tablesaw.io.xml.XmlReadOptions;
import tech.tablesaw.io.xml.XmlWriteOptions;
import tech.tablesaw.io.xlsx.XlsxWriteOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExportDataTest {

  @TempDir File tempDir;

  @Test
  public void testXmlExport() throws IOException {
    var url = getClass().getResource("/glaciers.csv");
    var table = Table.read().csv(url);
    table.setName("glaciers");

    StringWriter writer = new StringWriter();

    var options = XmlWriteOptions.builder(writer)
        .build();
    table.write().usingOptions(options);

    var xml = writer.toString();
    writer.close();
    assertTrue(xml.contains("<table name=\"glaciers\">"));
    assertTrue(xml.contains("<td name=\"Year\">2014</td>"));
    assertTrue(xml.contains("<td name=\"Mean cumulative mass balance\">-28.652</td>"));
    assertTrue(xml.contains("<td name=\"Number of observations\">24</td>"));
  }

  @Test
  public void testXmlRoundTripPreservesStringWhitespace() {
    Table table =
        Table.create("whitespace")
            .addColumns(
                StringColumn.create(
                    "value", new String[] {"  hello   world  ", "a\tb", " "}));
    StringWriter writer = new StringWriter();
    table.write().usingOptions(XmlWriteOptions.builder(writer).build());
    XmlReadOptions.Builder builder = XmlReadOptions.builderFromString(writer.toString());
    builder.columnTypes(new ColumnType[] {ColumnType.STRING});

    Table restored = new tech.tablesaw.io.xml.XmlReader().read(builder.build());

    assertEquals("  hello   world  ", restored.stringColumn("value").get(0));
    assertEquals("a\tb", restored.stringColumn("value").get(1));
    assertEquals(" ", restored.stringColumn("value").get(2));
    assertFalse(restored.stringColumn("value").isMissing(2));
  }

  @Test
  public void testOdsExport() throws IOException {
    var url = getClass().getResource("/glaciers.csv");
    var table = Table.read().csv(url);
    table.setName("glaciers");

    File destFile = File.createTempFile("glaciers", ".ods");
    FileOutputStream out = new FileOutputStream(destFile);
    OdsWriteOptions options = OdsWriteOptions.builder(out)
        .build();

    table.write().usingOptions(options);
    assertTrue(destFile.exists());
    destFile.deleteOnExit();
  }

  @Test
  public void testXlsxExportLocalDateTime() throws IOException {
    var table = Table.create("datetime-test")
        .addColumns(DateTimeColumn.create("dt",
            new java.time.LocalDateTime[]{
                LocalDateTime.parse("2024-06-24T12:34:56")
            }));

    File destFile = File.createTempFile("datetime-test", ".xlsx");
    try (FileOutputStream out = new FileOutputStream(destFile)) {
      XlsxWriteOptions options = XlsxWriteOptions.builder(out).build();
      table.write().usingOptions(options);
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(destFile))) {
      XSSFSheet sheet = workbook.getSheetAt(0);
      Row dataRow = sheet.getRow(1);
      Cell cell = dataRow.getCell(0);
      assertEquals(CellType.NUMERIC, cell.getCellType(), "DateTime should be numeric in Excel");
      assertTrue(cell.getDateCellValue() != null, "Date cell value should not be null");
      var cal = java.util.Calendar.getInstance();
      cal.setTime(cell.getDateCellValue());
      assertEquals(2024, cal.get(java.util.Calendar.YEAR));
      assertEquals(java.util.Calendar.JUNE, cal.get(java.util.Calendar.MONTH));
      assertEquals(24, cal.get(java.util.Calendar.DAY_OF_MONTH));
      assertEquals(12, cal.get(java.util.Calendar.HOUR_OF_DAY));
      assertEquals(34, cal.get(java.util.Calendar.MINUTE));
      assertEquals(56, cal.get(java.util.Calendar.SECOND));
    }

    destFile.deleteOnExit();
  }

  @Test
  public void testXlsxExportNullLocalDateTime() throws IOException {
    var table = Table.create("datetime-null-test")
        .addColumns(DateTimeColumn.create("dt",
            new java.time.LocalDateTime[]{
                LocalDateTime.parse("2024-06-24T12:34:56"),
                null
            }));

    File destFile = File.createTempFile("datetime-null-test", ".xlsx");
    try (FileOutputStream out = new FileOutputStream(destFile)) {
      XlsxWriteOptions options = XlsxWriteOptions.builder(out).build();
      table.write().usingOptions(options);
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(destFile))) {
      XSSFSheet sheet = workbook.getSheetAt(0);
      Row row1 = sheet.getRow(1);
      Cell cell1 = row1.getCell(0);
      assertEquals(CellType.NUMERIC, cell1.getCellType(), "First DateTime should be numeric");
      assertTrue(cell1.getDateCellValue() != null);

      Row row2 = sheet.getRow(2);
      Cell cell2 = row2.getCell(0);
      assertEquals(CellType.BLANK, cell2.getCellType(), "Null DateTime should be blank");
    }

    destFile.deleteOnExit();
  }

  @Test
  public void testXlsxExportBigDecimalAsNumericCell() throws IOException {
    var table = Table.create("bigdecimal-test")
        .addColumns(BigDecimalColumn.create("amount",
            new BigDecimal[]{new BigDecimal("123.45"), null}));

    File destFile = File.createTempFile("bigdecimal-test", ".xlsx");
    try (FileOutputStream out = new FileOutputStream(destFile)) {
      XlsxWriteOptions options = XlsxWriteOptions.builder(out).build();
      table.write().usingOptions(options);
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(destFile))) {
      XSSFSheet sheet = workbook.getSheetAt(0);
      Row row1 = sheet.getRow(1);
      Cell cell1 = row1.getCell(0);
      assertEquals(CellType.NUMERIC, cell1.getCellType(), "BigDecimal should be numeric in Excel");
      assertEquals(123.45d, cell1.getNumericCellValue());

      Row row2 = sheet.getRow(2);
      Cell cell2 = row2.getCell(0);
      assertEquals(CellType.BLANK, cell2.getCellType(), "Null BigDecimal should be blank");
    }

    destFile.deleteOnExit();
  }

  @Test
  public void testMissingValuesRemainBlankInEveryFormat() throws IOException {
    Table table = missingValueTable();

    File xlsx = new File(tempDir, "missing.xlsx");
    table.write().usingOptions(XlsxWriteOptions.builder(xlsx).build());
    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(xlsx))) {
      Row missing = workbook.getSheetAt(0).getRow(2);
      for (int i = 0; i < table.columnCount(); i++) {
        assertEquals(CellType.BLANK, missing.getCell(i).getCellType());
      }
      Row realSentinels = workbook.getSheetAt(0).getRow(3);
      assertEquals(2d, realSentinels.getCell(1).getNumericCellValue());
      assertFalse(realSentinels.getCell(6).getBooleanCellValue());
    }

    ColumnType[] types = table.types().toArray(new ColumnType[0]);
    File xml = new File(tempDir, "missing.xml");
    table.write().usingOptions(XmlWriteOptions.builder(xml).build());
    XmlReadOptions.Builder xmlBuilder = XmlReadOptions.builder(xml);
    xmlBuilder.columnTypes(types);
    assertMissingRows(new tech.tablesaw.io.xml.XmlReader().read(xmlBuilder.build()));

    File ods = new File(tempDir, "missing.ods");
    table.write().usingOptions(OdsWriteOptions.builder(ods).build());
    OdsReadOptions.Builder odsBuilder = OdsReadOptions.builder(ods);
    odsBuilder.columnTypes(types);
    assertMissingRows(Table.read().usingOptions(odsBuilder.build()));

    XmlReadOptions inferredOptions = XmlReadOptions.builder(xml).build();
    Table inferred = new tech.tablesaw.io.xml.XmlReader().read(inferredOptions);
    for (int i = 0; i < inferred.columnCount(); i++) {
      assertTrue(inferred.column(i).isMissing(1));
    }
  }

  @Test
  public void testWriterDestinationsAndSafeSheetNames() throws IOException {
    Table table = Table.create("bad[]:*?/\\name-that-is-far-longer-than-thirty-one-characters")
        .addColumns(IntColumn.create("value", new int[] {1}));
    assertEquals(
        "XLSX requires a binary OutputStream destination",
        assertThrows(
                IllegalArgumentException.class,
                () -> table.write().usingOptions(XlsxWriteOptions.builder(new StringWriter()).build()))
            .getMessage());

    File output = new File(tempDir, "safe.xlsx");
    table.write().usingOptions(XlsxWriteOptions.builder(output).build());
    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(output))) {
      String name = workbook.getSheetAt(0).getSheetName();
      assertTrue(name.length() <= 31);
      assertFalse(name.matches(".*[\\[\\]:*?/\\\\].*"));
    }

    assertEquals("Sheet1", writeAndReadSheetName(Table.create((String) null), "null-name.xlsx"));
    assertEquals("Sheet1", writeAndReadSheetName(Table.create("   "), "blank-name.xlsx"));
    assertEquals("Sheet1", writeAndReadSheetName(Table.create("[]"), "illegal-name.xlsx"));
    assertEquals("normal", writeAndReadSheetName(Table.create("normal"), "normal-name.xlsx"));
  }

  @Test
  public void testFileDestinationsOpenLazilyAndCloseAfterWrite() throws IOException {
    Table table = Table.create("lazy").addColumns(IntColumn.create("value", new int[] {1}));
    File xmlOutput = new File(tempDir, "lazy.xml");
    File odsOutput = new File(tempDir, "lazy.ods");
    File xlsxOutput = new File(tempDir, "lazy.xlsx");
    XmlWriteOptions xmlOptions = XmlWriteOptions.builder(xmlOutput).build();
    OdsWriteOptions odsOptions = OdsWriteOptions.builder(odsOutput).build();
    XlsxWriteOptions xlsxOptions = XlsxWriteOptions.builder(xlsxOutput).build();
    assertFalse(xmlOutput.exists());
    assertFalse(odsOutput.exists());
    assertFalse(xlsxOutput.exists());

    table.write().usingOptions(xmlOptions);
    table.write().usingOptions(odsOptions);
    table.write().usingOptions(xlsxOptions);
    assertTrue(xmlOutput.exists());
    assertTrue(odsOutput.exists());
    assertTrue(xlsxOutput.exists());
    OutputStream closedStream = xmlOptions.destination().stream();
    assertThrows(IOException.class, () -> closedStream.write(1));

    assertThrows(
        RuntimeIOException.class,
        () ->
            table
                .write()
                .usingOptions(XmlWriteOptions.builder(new File(tempDir, "missing/out.xml")).build()));
  }

  @Test
  public void testOdsTrailingAllMissingRow() throws IOException {
    Table table =
        Table.create("trailing")
            .addColumns(
                StringColumn.create("s").append("a").appendMissing(),
                IntColumn.create("i").append(1).appendMissing());

    File ods = new File(tempDir, "trailing.ods");
    table.write().usingOptions(OdsWriteOptions.builder(ods).build());

    // By default trailing all-missing rows are trimmed (ODF padding convention)
    Table trimmed = Table.read().usingOptions(OdsReadOptions.builder(ods).build());
    assertEquals(1, trimmed.rowCount());

    // ...and a legitimate trailing all-missing data row can be preserved explicitly
    Table kept =
        Table.read()
            .usingOptions(OdsReadOptions.builder(ods).trimTrailingMissingRows(false).build());
    assertEquals(2, kept.rowCount());
    assertTrue(kept.column("s").isMissing(1));
    assertTrue(kept.column("i").isMissing(1));
  }

  private static Table missingValueTable() {
    return Table.create("missing")
        .addColumns(
            ShortColumn.create("short").append((short) 1).appendMissing().append((short) 2),
            IntColumn.create("int").append(1).appendMissing().append(2),
            LongColumn.create("long").append(1L).appendMissing().append(2L),
            FloatColumn.create("float").append(1.5f).appendMissing().append(2.5f),
            DoubleColumn.create("double").append(1.5d).appendMissing().append(2.5d),
            BigDecimalColumn.create(
                "decimal", new BigDecimal[] {new BigDecimal("1.5"), null, new BigDecimal("2.5")}),
            BooleanColumn.create("boolean").append(true).appendMissing().append(false));
  }

  private String writeAndReadSheetName(Table table, String filename) throws IOException {
    table.addColumns(IntColumn.create("value", new int[] {1}));
    File output = new File(tempDir, filename);
    table.write().usingOptions(XlsxWriteOptions.builder(output).build());
    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(output))) {
      return workbook.getSheetAt(0).getSheetName();
    }
  }

  private static void assertMissingRows(Table table) {
    assertEquals(3, table.rowCount());
    assertEquals(7, table.columnCount());
    assertEquals(BigDecimalColumnType.instance(), table.column(5).type());
    for (int i = 0; i < table.columnCount(); i++) {
      assertTrue(
          table.column(i).isMissing(1),
          "column " + i + " (" + table.column(i).name() + ", " + table.column(i).type() + ")");
    }
  }
}
