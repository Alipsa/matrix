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

public class XmlReader implements DataReader<XmlReadOptions> {

  private static final XmlReader INSTANCE = new XmlReader();

  static {
    register(Table.defaultReaderRegistry);
  }

  public static void register(ReaderRegistry registry) {
    registry.registerExtension("xml", INSTANCE);
    registry.registerMimeType("application/xml", INSTANCE);
    registry.registerOptions(XmlReadOptions.class, INSTANCE);
  }

  @Override
  public Table read(Source source) {
    return read(XmlReadOptions.builder(source).build());
  }

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
