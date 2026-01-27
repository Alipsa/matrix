package tech.tablesaw.column.numbers;

import com.google.common.collect.Lists;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.columns.AbstractColumnParser;
import tech.tablesaw.io.ReadOptions;

import java.math.BigDecimal;

/**
 * Parser for BigDecimal column values.
 *
 * <p>This parser converts string representations to {@link BigDecimal} values. It uses the
 * {@link BigDecimal#BigDecimal(String)} constructor for parsing, which supports standard
 * decimal number formats including scientific notation.
 *
 * <p>The parser treats configured missing value indicators as missing and returns the
 * column's missing value indicator for those inputs.
 *
 * @see BigDecimalColumnType
 * @see AbstractColumnParser
 */
public class BigDecimalParser extends AbstractColumnParser<BigDecimal> {

  /**
   * Constructs a parser with the specified column type.
   *
   * @param columnType the column type
   */
  public BigDecimalParser(ColumnType columnType) {
    super(columnType);
  }

  /**
   * Constructs a parser with the specified column type and read options.
   * Missing value indicators from the read options are used to identify missing values.
   *
   * @param bigDecimalColumnType the BigDecimal column type
   * @param readOptions the read options containing missing value indicators
   */
  public BigDecimalParser(BigDecimalColumnType bigDecimalColumnType, ReadOptions readOptions) {
    super(bigDecimalColumnType);
    if (readOptions.missingValueIndicators().length > 0) {
      missingValueStrings = Lists.newArrayList(readOptions.missingValueIndicators());
    }
  }

  /**
   * Checks if the given string can be parsed as a BigDecimal.
   *
   * @param s the string to check
   * @return true if the string can be parsed as a BigDecimal or is a configured missing value, false otherwise
   */
  @Override
  public boolean canParse(String s) {
    if (isMissing(s)) return true;
    try {
      new BigDecimal(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Parses the given string as a BigDecimal.
   *
   * @param s the string to parse
   * @return the parsed BigDecimal value, or the missing value indicator if the input is missing
   * @throws NumberFormatException if the string cannot be parsed as a valid BigDecimal
   */
  @Override
  public BigDecimal parse(String s) {
    if (isMissing(s)) {
      return BigDecimalColumnType.missingValueIndicator();
    }
    return new BigDecimal(s);
  }
}
