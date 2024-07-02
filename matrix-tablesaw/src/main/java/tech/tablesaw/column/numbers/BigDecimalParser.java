package tech.tablesaw.column.numbers;

import com.google.common.collect.Lists;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.columns.AbstractColumnParser;
import tech.tablesaw.io.ReadOptions;

import java.math.BigDecimal;

public class BigDecimalParser extends AbstractColumnParser<BigDecimal> {

  public BigDecimalParser(ColumnType columnType) {
    super(columnType);
  }

  public BigDecimalParser(BigDecimalColumnType doubleColumnType, ReadOptions readOptions) {
    super(doubleColumnType);
    if (readOptions.missingValueIndicators().length > 0) {
      missingValueStrings = Lists.newArrayList(readOptions.missingValueIndicators());
    }
  }

  @Override
  public boolean canParse(String s) {
    if (s == null) return true;
    try {
      new BigDecimal(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public BigDecimal parse(String s) {
    return new BigDecimal(s);
  }
}
