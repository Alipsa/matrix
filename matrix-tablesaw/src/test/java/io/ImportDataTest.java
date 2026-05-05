package io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.tablesaw.api.ColumnType.*;

import org.junit.jupiter.api.Test;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.json.JsonReadOptions;
import tech.tablesaw.io.ods.OdsReadOptions;
import tech.tablesaw.io.xml.XmlReadOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImportDataTest {

  @Test
  public void testJsonImport() throws IOException {
    var jsonUrl = getClass().getResource("/glaciers.json");

    var options = JsonReadOptions.builder(jsonUrl)
        .columnTypes(f -> switch (f) {
          case "Year", "Number of observations" -> INTEGER;
          case "Mean cumulative mass balance" -> DOUBLE;
          default -> STRING;
        })
        .build();

    var glaciers = Table.read().usingOptions(options);
    assertEquals(70, glaciers.rowCount());
    assertEquals(3, glaciers.columnCount(), "Number of columns");
    assertEquals(ColumnType.INTEGER, glaciers.column("Year").type(), "Year column type");
    assertEquals(ColumnType.DOUBLE, glaciers.column("Mean cumulative mass balance").type(), "Mean cumulative mass balance column type");
    assertEquals(ColumnType.INTEGER, glaciers.column("Number of observations").type(), "Number of observations column type");
  }

  @Test
  public void testXmlImport() throws IOException {
    var xmlUrl = getClass().getResource("/glaciers.xml");
    var options = XmlReadOptions.builder(xmlUrl)
        .columnTypes(f -> switch (f) {
          case "Year", "Number of observations" -> INTEGER;
          case "Mean cumulative mass balance" -> DOUBLE;
          default -> STRING;
        })
        .build();
    var glaciers = Table.read().usingOptions(options);
    assertEquals(70, glaciers.rowCount(), "Number of rows");
    assertEquals(3, glaciers.columnCount(), "Number of columns");
    assertEquals(ColumnType.INTEGER, glaciers.column("Year").type(), "Year column type");
    assertEquals(ColumnType.DOUBLE, glaciers.column("Mean cumulative mass balance").type(), "Mean cumulative mass balance column type");
    assertEquals(ColumnType.INTEGER, glaciers.column("Number of observations").type(), "Number of observations column type");
  }

  @Test
  public void testOdsImport() throws IOException {
    var url = getClass().getResource("/glaciers.ods");
    var options = OdsReadOptions.builder(url)
        .columnTypes(f -> switch (f) {
          case "Year", "Number of observations" -> INTEGER;
          case "Mean cumulative mass balance" -> DOUBLE;
          default -> STRING;
        })
        .build();
    var glaciers = Table.read().usingOptions(options);
    assertEquals(70, glaciers.rowCount(), "Number of rows");
    assertEquals(3, glaciers.columnCount(), "Number of columns");
    assertEquals(ColumnType.INTEGER, glaciers.column("Year").type(), "Year column type");
    assertEquals(ColumnType.DOUBLE, glaciers.column("Mean cumulative mass balance").type(), "Mean cumulative mass balance column type");
    assertEquals(ColumnType.INTEGER, glaciers.column("Number of observations").type(), "Number of observations column type");
  }

  @Test
  public void testOdsImportWithEmptyCells() throws Exception {
    File odsFile = File.createTempFile("partial", ".ods");
    try (FileOutputStream fos = new FileOutputStream(odsFile)) {
      com.github.miachm.sods.SpreadSheet spread = new com.github.miachm.sods.SpreadSheet();
      com.github.miachm.sods.Sheet sheet = new com.github.miachm.sods.Sheet("Sheet1", 4, 3);
      sheet.getRange(0, 0).setValue("A");
      sheet.getRange(0, 1).setValue("B");
      sheet.getRange(0, 2).setValue("C");
      // Row 1: all values present
      sheet.getRange(1, 0).setValue("x1");
      sheet.getRange(1, 1).setValue("y1");
      sheet.getRange(1, 2).setValue("z1");
      // Row 2: partially empty (middle cell null)
      sheet.getRange(2, 0).setValue("x2");
      sheet.getRange(2, 1).setValue(null);
      sheet.getRange(2, 2).setValue("z2");
      // Row 3: all empty (should be skipped)
      sheet.getRange(3, 0).setValue(null);
      sheet.getRange(3, 1).setValue(null);
      sheet.getRange(3, 2).setValue(null);
      spread.addSheet(sheet, 0);
      spread.save(fos);
    }

    var options = OdsReadOptions.builder(odsFile).build();
    var table = Table.read().usingOptions(options);
    assertEquals(2, table.rowCount(), "Should have 2 data rows (all-empty row skipped)");
    assertEquals(3, table.columnCount());
    assertEquals("x1", table.get(0, 0));
    assertEquals("y1", table.get(0, 1));
    assertEquals("z1", table.get(0, 2));
    assertEquals("x2", table.get(1, 0));
    assertTrue(table.column(1).isMissing(1), "Missing cell should be missing, not 'null' string");
    assertEquals("z2", table.get(1, 2));

    odsFile.deleteOnExit();
  }

}
