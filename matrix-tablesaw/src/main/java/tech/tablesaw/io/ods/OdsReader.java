package tech.tablesaw.io.ods;

import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reader for ODS (OpenDocument Spreadsheet) files, which are used by applications like LibreOffice Calc and Apache OpenOffice Calc.
 * <p>
 * This reader utilizes the 'sods' library to parse ODS files and convert them into a Tablesaw Table.
 * ODS is a binary (ZIP) format, so Reader-backed sources are rejected with
 * {@link IllegalArgumentException}.
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
   * The first row is treated as column headers. Trailing rows where all values are missing are
   * dropped by default (ODF producers commonly declare empty rows past the data range), while
   * interior all-missing rows are preserved so missing data keeps its position; disable the trim
   * with {@code trimTrailingMissingRows(false)} to keep a legitimate trailing all-missing data
 * row, for example when round-tripping a file written by {@link OdsWriter}.
 * Empty or blank header cells are named {@code C<zero-based column index>}; if that name is
 * already taken by a real header, a {@code -2}, {@code -3}, ... suffix is appended. Duplicate
 * non-blank headers are handled the same way, case-insensitively. All cell values are read as
 * strings and then converted to appropriate types based on the read options.
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

      List<String> rawHeaders = new ArrayList<>();
      for (int colNum = 0; colNum < lastColumn; colNum++) {
        Object val = sheet.getRange(0, colNum).getValue();
        rawHeaders.add(val == null ? null : String.valueOf(val));
      }
      Set<String> taken = new HashSet<>();
      for (String header : rawHeaders) {
        if (header != null && !header.isBlank()) {
          taken.add(header.toLowerCase(Locale.ROOT));
        }
      }
      Set<String> originalHeadersSeen = new HashSet<>();
      for (int colNum = 0; colNum < lastColumn; colNum++) {
        String header = rawHeaders.get(colNum);
        if (header == null || header.isBlank()) {
          columnNames.add(ColumnNames.unique("C" + colNum, taken));
        } else if (originalHeadersSeen.add(header.toLowerCase(Locale.ROOT))) {
          columnNames.add(header);
        } else {
          columnNames.add(ColumnNames.unique(header, taken));
        }
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
   * Handles binary file and input-stream source types.
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
      throw new IllegalArgumentException("ODS requires a binary InputStream or File source");
    }
    return options.source().inputStream();
  }

}
