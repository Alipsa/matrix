package io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.ods.OdsWriteOptions;
import tech.tablesaw.io.xml.XmlWriteOptions;
import tech.tablesaw.io.xlsx.XlsxWriteOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExportDataTest {

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
    assertEquals(8910, writer.toString().length(), "Unexpected XML content length: " + xml.length());
    assertTrue(xml.contains("<table name=\"glaciers\">"));
    assertTrue(xml.contains("<td name=\"Year\">2014</td>"));
    assertTrue(xml.contains("<td name=\"Mean cumulative mass balance\">-28.652</td>"));
    assertTrue(xml.contains("<td name=\"Number of observations\">24</td>"));
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
    System.out.println("Wrote " + destFile + " deleting it now.");
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
}
