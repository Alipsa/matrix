package tech.tablesaw.io.xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
   * @throws RuntimeIOException if an I/O error occurs during reading
   * @throws RuntimeException if the XML document cannot be parsed
   */
  @Override
  public Table read(XmlReadOptions options) {
    SAXReader reader = new SAXReader();
    Document document;

    try {
      document = reader.read(options.source().createReader(null));
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    } catch (DocumentException e) {
      throw new RuntimeException(e);
    }
    Element root = document.getRootElement();
    boolean isFirstRow = true;
    List<String> columnNames = new ArrayList<>();
    List<String[]> dataRows = new ArrayList<>();
    for (Element row : root.elements("tr")) {
      if (isFirstRow) {
        for (Element cell : row.elements()) {
          columnNames.add(cell.attributeValue("name"));
        }
        isFirstRow = false;
      }
      String[] rowValues = new String[columnNames.size()];
      List<Element> elements = row.elements();
      for (int i = 0; i < elements.size(); i++ ) {
        rowValues[i] = elements.get(i).getText();
      }
      dataRows.add(rowValues);
    }
    Table table = TableBuildingUtils.build(columnNames, dataRows, options);
    table.setName(root.attributeValue("name"));
    return table;
  }

}
