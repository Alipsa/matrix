package tech.tablesaw.io.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

class XmlNonUtf8DefaultEncodingTest {

  @Test
  void writesCorrectDeclarationsForStreamAndWriterDestinations() {
    assertNotEquals(StandardCharsets.UTF_8, Charset.defaultCharset());
    Table table = Table.create("people").addColumns(StringColumn.create("name", new String[] {"Åsa"}));

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    table.write().usingOptions(XmlWriteOptions.builder(bytes).build());
    String streamXml = bytes.toString(StandardCharsets.UTF_8);
    assertTrue(streamXml.contains("encoding=\"UTF-8\""));
    assertTrue(streamXml.contains("Åsa"));

    StringWriter writer = new StringWriter();
    table.write().usingOptions(XmlWriteOptions.builder(writer).build());
    String writerXml = writer.toString();
    assertTrue(writerXml.startsWith("<?xml version=\"1.0\"?>"));
    assertFalse(writerXml.contains("encoding="));
    assertTrue(writerXml.contains("Åsa"));
  }
}
