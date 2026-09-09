package tech.tablesaw.io.xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.*;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reader for XML format using dom4j.
 *
 * <p>This reader imports Tablesaw tables from XML format files created by {@link XmlWriter}.
 * The expected XML structure is:
 * <pre>{@code
 * <table name="tableName">
 *   <tr>
 *     <td name="columnName">value</td>
 *     <td name="columnName2">value2</td>
 *   </tr>
 *   ...
 * </table>
 * }</pre>
 *
 * <p>Column names are extracted from the {@code name} attributes of the {@code <td>} elements
 * in the first {@code <tr>} row. The table name is read from the {@code name} attribute of the
 * root {@code <table>} element.
 *
 * <p>For security, documents containing a {@code DOCTYPE} are rejected and external entities are
 * disabled. The source reader or input stream is closed after every read, including failed reads.
 *
 * <p>The reader is automatically registered for the ".xml" extension and "application/xml"
 * MIME type in the default reader registry.
 *
 * @see XmlReadOptions
 * @see XmlWriter
 */
public class XmlReader implements DataReader<XmlReadOptions> {

  private static final XmlReader INSTANCE = new XmlReader();

  static {
    register(Table.defaultReaderRegistry);
  }

  /**
   * Creates an XML reader.
   */
  public XmlReader() {
  }

  /**
   * Register this reader with the given registry.
   *
   * @param registry the reader registry to register with
   */
  public static void register(ReaderRegistry registry) {
    registry.registerExtension("xml", INSTANCE);
    registry.registerMimeType("application/xml", INSTANCE);
    registry.registerOptions(XmlReadOptions.class, INSTANCE);
  }

  /**
   * Read a table from the source using default options.
   *
   * @param source the source to read from
   * @return the table read from the source
   */
  @Override
  public Table read(Source source) {
    return read(XmlReadOptions.builder(source).build());
  }

  /**
   * Read a table from an XML file using the specified options.
   *
   * <p>Parses the XML document and extracts the table structure. Column names are determined
   * from the {@code name} attributes of {@code <td>} elements in the first row. All row data
   * is read as strings and then converted to appropriate types based on the read options.
   *
   * @param options the read options specifying the source and parsing configuration
   * @return the table read from the XML file
   * @throws RuntimeIOException if an I/O error occurs during reading or if the XML document cannot be parsed
   */
  @Override
  public Table read(XmlReadOptions options) {
    SAXReader reader = secureReader();
    Document document;

    try (Reader sourceReader = options.source().createReader(null)) {
      document = reader.read(sourceReader);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    } catch (DocumentException e) {
      throw new RuntimeIOException(new IOException("Failed to parse XML", e));
    }
    Element root = document.getRootElement();
    boolean isFirstRow = true;
    List<String> columnNames = new ArrayList<>();
    List<String[]> dataRows = new ArrayList<>();
    int rowNumber = 0;
    for (Element row : root.elements("tr")) {
      rowNumber++;
      List<Element> elements = row.elements("td");
      if (isFirstRow) {
        columnNames = columnNames(elements, options.allowDuplicateColumnNames());
        isFirstRow = false;
      }
      validateCellCount(rowNumber - 1, columnNames.size(), elements.size());
      String[] rowValues = new String[columnNames.size()];
      for (int i = 0; i < elements.size(); i++ ) {
        rowValues[i] = elements.get(i).getText();
      }
      dataRows.add(rowValues);
    }
    Table table = TableBuildingUtils.build(columnNames, dataRows, options);
    table.setName(root.attributeValue("name"));
    return table;
  }

  private static SAXReader secureReader() {
    SAXReader reader = new SAXReader();
    try {
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    } catch (SAXException e) {
      throw new RuntimeIOException(new IOException("Failed to configure secure XML parser", e));
    }
    return reader;
  }

  private static List<String> columnNames(List<Element> cells, boolean allowDuplicates) {
    List<String> names = new ArrayList<>();
    Set<String> reservedNames = new HashSet<>();
    for (Element cell : cells) {
      String name = cell.attributeValue("name");
      if (name == null || name.isBlank()) {
        throw invalidXml("First XML row contains a cell with a missing or blank name");
      }
      reservedNames.add(name.toLowerCase(Locale.ROOT));
    }
    Set<String> originalNamesSeen = new HashSet<>();
    Set<String> assignedNames = new HashSet<>();
    for (Element cell : cells) {
      String name = cell.attributeValue("name");
      String candidate = name;
      String normalizedName = name.toLowerCase(Locale.ROOT);
      if (originalNamesSeen.contains(normalizedName)) {
        if (!allowDuplicates) {
          throw invalidXml("Duplicate XML column name: " + name);
        }
        int suffix = 2;
        do {
          candidate = name + "-" + suffix++;
          normalizedName = candidate.toLowerCase(Locale.ROOT);
        } while (reservedNames.contains(normalizedName) || assignedNames.contains(normalizedName));
      }
      originalNamesSeen.add(name.toLowerCase(Locale.ROOT));
      assignedNames.add(normalizedName);
      names.add(candidate);
    }
    return names;
  }

  private static void validateCellCount(int rowNumber, int expected, int actual) {
    if (expected != actual) {
      throw invalidXml(
          "XML data row " + rowNumber + " contains " + actual + " cells; expected " + expected);
    }
  }

  private static RuntimeIOException invalidXml(String message) {
    return new RuntimeIOException(new IOException(message));
  }
}
