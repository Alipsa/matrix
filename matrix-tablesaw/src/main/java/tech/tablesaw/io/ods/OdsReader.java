package tech.tablesaw.io.ods;

import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import org.apache.commons.io.input.ReaderInputStream;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for ODS (OpenDocument Spreadsheet) files, which are used by applications like LibreOffice Calc and Apache OpenOffice Calc.
 * <p>
 * This reader utilizes the 'sods' library to parse ODS files and convert them into a Tablesaw Table.
 * <p>
 * Supported options include:
 * <ul>
 *     <li>sheetIndex: The index of the sheet to read (default is 0, the first sheet).</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * Table table = Table.read().ods("data.ods", OdsReadOptions.builder().sheetIndex(1).build());
 * }</pre>
 */
public class OdsReader implements DataReader<OdsReadOptions> {

  private static final OdsReader INSTANCE = new OdsReader();

  private OdsReader() {
    // singleton
  }

  static {
    register(Table.defaultReaderRegistry);
  }

  /**
   * Register this reader with the given registry.
   *
   * @param registry the reader registry to register with
   */
  public static void register(ReaderRegistry registry) {
    registry.registerExtension("ods", INSTANCE);
    registry.registerMimeType("application/vnd.oasis.opendocument.spreadsheet", INSTANCE);
    registry.registerOptions(OdsReadOptions.class, INSTANCE);
  }

  /**
   * Read a table from the source using default options.
   * Reads the first sheet (index 0) by default.
   *
   * @param source the source to read from
   * @return the table read from the source
   */
  @Override
  public Table read(Source source) {
    return read(OdsReadOptions.builder(source).build());
  }

  /**
   * Read a table from an ODS file using the specified options.
   *
   * <p>Reads data from the specified sheet index (default is 0, the first sheet).
   * The first row is treated as column headers. Interior all-missing rows are preserved so
   * missing data keeps its position, and trailing all-missing rows are kept by default so round
   * trips are lossless; enable {@code trimTrailingMissingRows(true)} to drop trailing empty rows
   * that ODF producers commonly declare past the data range.
   * All cell values are read as strings and then converted to appropriate types based on
   * the read options.
   *
   * @param options the read options specifying the source, sheet index, and parsing configuration
   * @return the table read from the ODS file
   * @throws RuntimeIOException if an I/O error occurs during reading
   */
  @Override
  public Table read(OdsReadOptions options) {

    try (InputStream is = getInputStream(options)) {

      SpreadSheet spreadSheet = new SpreadSheet(is);
      int sheetIndex = options.sheetIndex == null ? 0 : options.sheetIndex;

      Sheet sheet = spreadSheet.getSheet(sheetIndex);
      int lastRow = sheet.getMaxRows();
      int lastColumn = sheet.getMaxColumns();

      List<String> columnNames = new ArrayList<>(lastColumn);

      for (int colNum = 0; colNum < lastColumn; colNum++) {
        Object val = sheet.getRange(0, colNum).getValue();
        columnNames.add(String.valueOf(val));
      }

      List<String[]> dataRows = new ArrayList<>();
      for (int rowNum = 1; rowNum < lastRow; rowNum++) {
        String[] rowValues = new String[columnNames.size()];
        for (int colNum = 0; colNum < lastColumn; colNum++) {
          Object val = sheet.getRange(rowNum, colNum).getValue();
          rowValues[colNum] = val == null ? null : String.valueOf(val);
        }
        dataRows.add(rowValues);
      }
      // Drop trailing all-missing rows, which ODF producers commonly declare past the data range,
      // but keep interior all-missing rows so missing data round-trips with its row position
      if (options.trimTrailingMissingRows) {
        while (!dataRows.isEmpty() && allMissing(dataRows.get(dataRows.size() - 1))) {
          dataRows.remove(dataRows.size() - 1);
        }
      }
      return TableBuildingUtils.build(columnNames, dataRows, options);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  /**
   * Checks whether every value in the row is null (missing).
   *
   * @param rowValues the row to check
   * @return true if all values are null
   */
  private static boolean allMissing(String[] rowValues) {
    for (String value : rowValues) {
      if (value != null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get an InputStream from the source specified in the read options.
   * Handles different source types: file, reader, or input stream.
   *
   * @param options the read options containing the source
   * @return an InputStream for reading the ODS data
   * @throws IOException if the source cannot be accessed
   * @throws FileNotFoundException if the source file does not exist
   */
  private InputStream getInputStream(ReadOptions options) throws IOException {
    if (options.source().file() != null) {
      return new FileInputStream(options.source().file());
    }
    if (options.source().reader() != null) {
      return ReaderInputStream.builder()
          .setReader(options.source().reader())
          .setCharset(StandardCharsets.UTF_8)
          .get();
      //return new ReaderInputStream(options.source().reader(), StandardCharsets.UTF_8);
    }
    return options.source().inputStream();
  }

}
