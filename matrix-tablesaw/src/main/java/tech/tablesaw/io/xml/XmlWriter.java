package tech.tablesaw.io.xml;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.DataWriter;
import tech.tablesaw.io.Destination;
import tech.tablesaw.io.RuntimeIOException;
import tech.tablesaw.io.WriterRegistry;

import java.io.IOException;
import java.io.Writer;

/**
 * Writer for XML format using dom4j.
 *
 * <p>This writer exports Tablesaw tables to XML format files. The generated XML structure is:
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
 * <p>Each row is represented as a {@code <tr>} element, with each column value as a {@code <td>}
 * element containing a name attribute for the column name and the value as text content. Null values
 * result in empty {@code <td>} elements.
 *
 * <p>The writer is automatically registered for the ".xml" extension in the default writer registry.
 *
 * @see XmlWriteOptions
 * @see XmlReader
 */
public class XmlWriter implements DataWriter<XmlWriteOptions> {

  private static final XmlWriter INSTANCE = new XmlWriter();

  static {
    register(Table.defaultWriterRegistry);
  }

  /**
   * Register this writer with the given registry.
   *
   * @param registry the writer registry to register with
   */
  public static void register(WriterRegistry registry) {
    registry.registerExtension("xml", INSTANCE);
    registry.registerOptions(XmlWriteOptions.class, INSTANCE);
  }

  /**
   * Write the table to the destination using default options.
   *
   * @param table the table to write
   * @param dest the destination to write to
   */
  @Override
  public void write(Table table, Destination dest) {
    write(table, XmlWriteOptions.builder(dest).build());
  }

  /**
   * Write the table to an XML file using the specified options.
   *
   * <p>Creates an XML document with a root {@code <table>} element containing the table name
   * as an attribute. Each row becomes a {@code <tr>} element with {@code <td>} child elements
   * for each column value.
   *
   * @param table the table to write
   * @param options the write options specifying the destination
   * @throws RuntimeIOException if an I/O error occurs during writing
   */
  @Override
  public void write(Table table, XmlWriteOptions options) {
    Document doc = DocumentHelper.createDocument();
    Element root = doc.addElement("table");
    root.addAttribute("name", table.name());
    for (Row row : table) {
      Element r = root.addElement("tr");
      for (String name : row.columnNames()) {
        var element = r.addElement("td");
        element.addAttribute("name", name);
        var value = row.getObject(name);
        if (value != null){
          element.setText(String.valueOf(value));
        }
      }
    }
    try (Writer writer = options.destination().createWriter()) {
      doc.write(writer);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }
}
