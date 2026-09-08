package tech.tablesaw.io;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import tech.tablesaw.io.ods.OdsWriteOptions;
import tech.tablesaw.io.xlsx.XlsxWriteOptions;
import tech.tablesaw.io.xml.XmlWriteOptions;

class FormatOptionsTest {

  @Test
  void buildsEveryWriteOptionFromEveryDestinationKind() {
    Destination destination = new Destination(new ByteArrayOutputStream());
    assertSame(destination, OdsWriteOptions.builder(destination).build().destination());
    assertSame(destination, XmlWriteOptions.builder(destination).build().destination());
    assertSame(destination, XlsxWriteOptions.builder(destination).build().destination());

    assertInstanceOf(
        ByteArrayOutputStream.class,
        OdsWriteOptions.builder(new ByteArrayOutputStream()).build().destination().stream());
    assertInstanceOf(
        ByteArrayOutputStream.class,
        XmlWriteOptions.builder(new ByteArrayOutputStream()).build().destination().stream());
    assertInstanceOf(
        ByteArrayOutputStream.class,
        XlsxWriteOptions.builder(new ByteArrayOutputStream()).build().destination().stream());

    assertInstanceOf(
        StringWriter.class, OdsWriteOptions.builder(new StringWriter()).build().destination().writer());
    assertInstanceOf(
        StringWriter.class, XmlWriteOptions.builder(new StringWriter()).build().destination().writer());
    assertInstanceOf(
        StringWriter.class, XlsxWriteOptions.builder(new StringWriter()).build().destination().writer());

    assertNull(OdsWriteOptions.builder(new File("unused.ods")).build().destination().writer());
    assertNull(XmlWriteOptions.builder("unused.xml").build().destination().writer());
    assertNull(XlsxWriteOptions.builder(new File("unused.xlsx")).build().destination().writer());
  }
}
