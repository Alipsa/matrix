package tech.tablesaw.column.numbers;

import tech.tablesaw.api.BigDecimalColumn;
import tech.tablesaw.columns.AbstractColumnType;
import tech.tablesaw.io.ReadOptions;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Column type descriptor for {@link BigDecimal} values.
 */
public class BigDecimalColumnType extends AbstractColumnType {
  /** Logical byte size used by Tablesaw metadata for this type. */
  public static final int BYTE_SIZE = 8;

  /** Default parser used for BigDecimal values. */
  public static final BigDecimalParser DEFAULT_PARSER = new BigDecimalParser(BigDecimalColumnType.instance());

  private static BigDecimalColumnType INSTANCE = new BigDecimalColumnType(BYTE_SIZE, "BIGDECIMAL", "BigDecimal");

  /**
   * Returns the singleton instance for this type.
   *
   * @return the singleton {@code BigDecimalColumnType}
   */
  public static BigDecimalColumnType instance() {
    if (INSTANCE == null) {
      INSTANCE = new BigDecimalColumnType(BYTE_SIZE, "BIGDECIMAL", "BigDecimal");
    }
    return INSTANCE;
  }

  private BigDecimalColumnType(int byteSize, String name, String printerFriendlyName) {
    super(byteSize, name, printerFriendlyName);
  }

  /**
   * Returns the value used to represent missing values.
   *
   * @return missing value indicator, which is {@code null}
   */
  public static BigDecimal missingValueIndicator() {
    // There is no NaN in BigDecimal, so we use null instead
    return null;
  }

  /**
   * Returns whether a value should be treated as missing.
   *
   * @param value value to check
   * @return {@code true} if the value is missing
   */
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
