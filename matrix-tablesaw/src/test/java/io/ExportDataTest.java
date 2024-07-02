package io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.ods.OdsWriteOptions;
import tech.tablesaw.io.xml.XmlWriteOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

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
}
