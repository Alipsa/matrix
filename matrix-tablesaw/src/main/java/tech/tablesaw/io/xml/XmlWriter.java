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

public class XmlWriter implements DataWriter<XmlWriteOptions> {

  private static final XmlWriter INSTANCE = new XmlWriter();

  static {
    register(Table.defaultWriterRegistry);
  }

  public static void register(WriterRegistry registry) {
    registry.registerExtension("xml", INSTANCE);
    registry.registerOptions(XmlWriteOptions.class, INSTANCE);
  }

  @Override
  public void write(Table table, Destination dest) {
    write(table, XmlWriteOptions.builder(dest).build());
  }

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
