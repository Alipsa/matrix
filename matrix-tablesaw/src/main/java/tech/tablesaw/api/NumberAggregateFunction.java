package tech.tablesaw.api;

import java.math.BigDecimal;
import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.column.numbers.BigDecimalColumnType;

/**
 * Base class for aggregate functions that operate on numeric columns and return {@link BigDecimal}
 * results.
 */
public abstract class NumberAggregateFunction extends AggregateFunction<BigDecimalColumn, BigDecimal> {

  /**
   * Constructs a named numeric aggregate function.
   *
   * @param name function display name
   */
  public NumberAggregateFunction(String name) {
    super(name);
  }
  /**
   * Returns true if the given {@link ColumnType} is compatible with this function
   *
   * @param type the column type
   * @return {@code true} if the type is numeric and supported
   */
  @Override
  public boolean isCompatibleColumn(ColumnType type) {
    return type.equals(ColumnType.DOUBLE)
        || type.equals(ColumnType.FLOAT)
        || type.equals(ColumnType.INTEGER)
        || type.equals(ColumnType.SHORT)
        || type.equals(ColumnType.LONG)
        || type.equals(BigDecimalColumnType.instance());
  }

  /**
   * Returns the {@link ColumnType} to be used for the values returned by this function
   *
   * @return {@link BigDecimalColumnType#instance()}
   */
  @Override
  public ColumnType returnType() {
    return BigDecimalColumnType.instance();
  }
}
