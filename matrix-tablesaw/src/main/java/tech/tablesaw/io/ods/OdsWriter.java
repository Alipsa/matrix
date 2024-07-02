package tech.tablesaw.io.ods;

import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import org.apache.commons.io.output.WriterOutputStream;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.DataWriter;
import tech.tablesaw.io.Destination;
import tech.tablesaw.io.RuntimeIOException;
import tech.tablesaw.io.WriterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OdsWriter implements DataWriter<OdsWriteOptions> {

  private static final OdsWriter INSTANCE = new OdsWriter();

  static {
    register(Table.defaultWriterRegistry);
  }

  public static void register(WriterRegistry registry) {
    registry.registerExtension("ods", INSTANCE);
    registry.registerOptions(OdsWriteOptions.class, INSTANCE);
  }

  @Override
  public void write(Table table, Destination dest) {
    write(table, OdsWriteOptions.builder(dest).build());
  }

  @Override
  public void write(Table table, OdsWriteOptions options) {
    try {
      SpreadSheet spreadSheet = new SpreadSheet();
      Sheet sheet = new Sheet(table.name(), table.rowCount() + 1, table.columnCount());
      spreadSheet.appendSheet(sheet);
      List<String> colNames = table.columnNames();

      // Header
      for (int i = 0; i < colNames.size(); i++) {
        sheet.getRange(0, i).setValue(colNames.get(i));
      }

      // table content
      int rowNum = 1;
      for (var row : table) {
        int colIdx = 0;
        for (String col : colNames) {
          sheet.getRange(rowNum, colIdx++).setValue(row.getObject(col));
        }
        rowNum++;
      }

      try (OutputStream os = options.destination().stream()) {
        if (os != null) {
          spreadSheet.save(os);
        } else {
          try(Writer writer = options.destination().writer();
              OutputStream wos = new WriterOutputStream(writer, StandardCharsets.UTF_8)) {
            spreadSheet.save(wos);
          }
        }
      }

    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }
}
