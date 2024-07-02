package tech.tablesaw.column.numbers;

import tech.tablesaw.api.BigDecimalColumn;
import tech.tablesaw.columns.AbstractColumnType;
import tech.tablesaw.io.ReadOptions;

import java.math.BigDecimal;
import java.util.Objects;

public class BigDecimalColumnType extends AbstractColumnType {
  public static final int BYTE_SIZE = 8;

  public static final BigDecimalParser DEFAULT_PARSER = new BigDecimalParser(BigDecimalColumnType.instance());

  private static BigDecimalColumnType INSTANCE = new BigDecimalColumnType(BYTE_SIZE, "BIGDECIMAL", "BigDecimal");

  public static BigDecimalColumnType instance() {
    if (INSTANCE == null) {
      INSTANCE = new BigDecimalColumnType(BYTE_SIZE, "BIGDECIMAL", "BigDecimal");
    }
    return INSTANCE;
  }

  private BigDecimalColumnType(int byteSize, String name, String printerFriendlyName) {
    super(byteSize, name, printerFriendlyName);
  }

  public static BigDecimal missingValueIndicator() {
    // There is no NaN in BigDecimal, so we use null instead
    return null;
  }

  public static boolean valueIsMissing(BigDecimal value) {
    return Objects.equals(value, missingValueIndicator());
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn create(String name) {
    return BigDecimalColumn.create(name);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalParser customParser(ReadOptions options) {
    return new BigDecimalParser(this, options);
  }

}
