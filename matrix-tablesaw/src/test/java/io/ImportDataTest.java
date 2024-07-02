package io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.tablesaw.api.ColumnType.*;

import org.junit.jupiter.api.Test;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.json.JsonReadOptions;
import tech.tablesaw.io.ods.OdsReadOptions;
import tech.tablesaw.io.xml.XmlReadOptions;

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
    //System.out.println(glaciers);
    //System.out.println(glaciers.structure());
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
    //System.out.println(glaciers.summary());
    assertEquals(ColumnType.INTEGER, glaciers.column("Year").type(), "Year column type");
    assertEquals(ColumnType.DOUBLE, glaciers.column("Mean cumulative mass balance").type(), "Mean cumulative mass balance column type");
    assertEquals(ColumnType.INTEGER, glaciers.column("Number of observations").type(), "Number of observations column type");
  }


}
