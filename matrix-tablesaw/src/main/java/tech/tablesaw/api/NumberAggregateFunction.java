package tech.tablesaw.api;

import java.math.BigDecimal;
import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.column.numbers.BigDecimalColumnType;

public abstract class NumberAggregateFunction extends AggregateFunction<BigDecimalColumn, BigDecimal> {

  public NumberAggregateFunction(String name) {
    super(name);
  }
  /**
   * Returns true if the given {@link ColumnType} is compatible with this function
   *
   * @param type the column type
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
   */
  @Override
  public ColumnType returnType() {
    return BigDecimalColumnType.instance();
  }
}
