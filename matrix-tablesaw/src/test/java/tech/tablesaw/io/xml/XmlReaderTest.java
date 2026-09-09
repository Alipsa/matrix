package tech.tablesaw.io.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.tablesaw.io.RuntimeIOException;

class XmlReaderTest {

  @TempDir Path tempDir;

  @Test
  void rejectsExternalEntitiesWithoutExposingContent() throws IOException {
    Path secret = tempDir.resolve("secret.txt");
    Files.writeString(secret, "xml-secret-value");
    String xml =
        "<!DOCTYPE table [<!ENTITY xxe SYSTEM \""
            + secret.toUri()
            + "\">]><table><tr><td name=\"value\">&xxe;</td></tr></table>";

    RuntimeIOException exception =
        assertThrows(
            RuntimeIOException.class,
            () -> new XmlReader().read(XmlReadOptions.builderFromString(xml).build()));

    assertFalse(exception.toString().contains("xml-secret-value"));
    assertInstanceOf(IOException.class, exception.getCause());
  }

  @Test
  void rejectsEveryDoctype() {
    String xml =
        "<!DOCTYPE table [<!ENTITY value \"safe\">]>"
            + "<table><tr><td name=\"value\">&value;</td></tr></table>";

    assertThrows(
        RuntimeIOException.class,
        () -> new XmlReader().read(XmlReadOptions.builderFromString(xml).build()));
  }

  @Test
  void closesReaderAfterSuccessAndFailure() {
    TrackingReader valid =
        new TrackingReader("<table><tr><td name=\"value\">1</td></tr></table>");
    new XmlReader().read(XmlReadOptions.builder(valid).build());
    assertTrue(valid.closed);

    TrackingReader invalid = new TrackingReader("<table>");
    assertThrows(
        RuntimeIOException.class,
        () -> new XmlReader().read(XmlReadOptions.builder(invalid).build()));
    assertTrue(invalid.closed);
  }

  @Test
  void rejectsMissingAndDuplicateColumnNames() {
    RuntimeIOException missing = assertInvalid("<td>1</td>");
    assertTrue(missing.getCause().getMessage().contains("missing or blank name"));

    RuntimeIOException duplicate =
        assertInvalid("<td name=\"Name\">1</td><td name=\"name\">2</td>");
    assertTrue(duplicate.getCause().getMessage().contains("Duplicate XML column name: name"));
  }

  @Test
  void renamesAllowedDuplicatesWithoutSuffixCollisions() {
    String xml =
        "<table><tr><td name=\"a\">1</td><td name=\"A\">2</td>"
            + "<td name=\"a-2\">3</td></tr></table>";
    XmlReadOptions.Builder builder = XmlReadOptions.builderFromString(xml);
    builder.allowDuplicateColumnNames(true);

    var table = new XmlReader().read(builder.build());

    assertArrayEquals(new String[] {"a", "A-3", "a-2"}, table.columnNames().toArray());
  }

  @Test
  void rejectsShortAndLongRowsWithUsefulMessages() {
    RuntimeIOException shortRow =
        assertMalformedRows("<td name=\"a\">2</td>");
    assertTrue(shortRow.getCause().getMessage().contains("row 1 contains 1 cells; expected 2"));

    RuntimeIOException longRow =
        assertMalformedRows("<td name=\"a\">2</td><td name=\"b\">3</td><td name=\"c\">4</td>");
    assertTrue(longRow.getCause().getMessage().contains("row 1 contains 3 cells; expected 2"));
  }

  private static RuntimeIOException assertInvalid(String cells) {
    String xml = "<table><tr>" + cells + "</tr></table>";
    return assertThrows(
        RuntimeIOException.class,
        () -> new XmlReader().read(XmlReadOptions.builderFromString(xml).build()));
  }

  private static RuntimeIOException assertMalformedRows(String secondRow) {
    String xml =
        "<table><tr><td name=\"a\">1</td><td name=\"b\">1</td></tr><tr>"
            + secondRow
            + "</tr></table>";
    return assertThrows(
        RuntimeIOException.class,
        () -> new XmlReader().read(XmlReadOptions.builderFromString(xml).build()));
  }

  private static final class TrackingReader extends StringReader {
    private boolean closed;

    private TrackingReader(String value) {
      super(value);
    }

    @Override
    public void close() {
      closed = true;
      super.close();
    }
  }
}
