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

public class OdsReader implements DataReader<OdsReadOptions> {

  private static final OdsReader INSTANCE = new OdsReader();

  static {
    register(Table.defaultReaderRegistry);
  }

  public static void register(ReaderRegistry registry) {
    registry.registerExtension("ods", INSTANCE);
    registry.registerMimeType("application/vnd.oasis.opendocument.spreadsheet", INSTANCE);
    registry.registerOptions(OdsReadOptions.class, INSTANCE);
  }

  @Override
  public Table read(Source source) {
    return read(OdsReadOptions.builder(source).build());
  }

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
        int nullCount = 0;
        for (int colNum = 0; colNum < lastColumn; colNum++) {
          Object val = sheet.getRange(rowNum, colNum).getValue();
          if (val == null) nullCount++;
          rowValues[colNum] = String.valueOf(val);
        }
        // Skip rows where all values are missing
        if (nullCount != lastColumn) {
          dataRows.add(rowValues);
        }
      }
      return TableBuildingUtils.build(columnNames, dataRows, options);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  private InputStream getInputStream(ReadOptions options) throws FileNotFoundException {
    if (options.source().file() != null) {
      return new FileInputStream(options.source().file());
    }
    if (options.source().reader() != null) {
      return new ReaderInputStream(options.source().reader(), StandardCharsets.UTF_8);
    }
    return options.source().inputStream();
  }

}
