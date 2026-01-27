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

/**
 * Writer for ODS (OpenDocument Spreadsheet) format.
 *
 * <p>This writer exports Tablesaw tables to ODS format files using the SODS library.
 * Each table is written as a single sheet in the ODS file, with column names as the first row
 * followed by the table data.
 *
 * <p>The writer is automatically registered for the ".ods" extension in the default writer registry.
 *
 * @see OdsWriteOptions
 * @see OdsReader
 */
public class OdsWriter implements DataWriter<OdsWriteOptions> {

  private static final OdsWriter INSTANCE = new OdsWriter();

  static {
    register(Table.defaultWriterRegistry);
  }

  /**
   * Register this writer with the given registry.
   *
   * @param registry the writer registry to register with
   */
  public static void register(WriterRegistry registry) {
    registry.registerExtension("ods", INSTANCE);
    registry.registerOptions(OdsWriteOptions.class, INSTANCE);
  }

  /**
   * Write the table to the destination using default options.
   *
   * @param table the table to write
   * @param dest the destination to write to
   */
  @Override
  public void write(Table table, Destination dest) {
    write(table, OdsWriteOptions.builder(dest).build());
  }

  /**
   * Write the table to the destination using the specified options.
   *
   * <p>The table is written as a single sheet with the table name as the sheet name.
   * The first row contains column headers, followed by the data rows.
   *
   * @param table the table to write
   * @param options the write options specifying the destination and other settings
   * @throws RuntimeIOException if an I/O error occurs during writing
   */
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
              OutputStream wos = WriterOutputStream.builder().setWriter(writer).setCharset(StandardCharsets.UTF_8).get()) {
            spreadSheet.save(wos);
          }
        }
      }

    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }
}
