package tech.tablesaw.api;

import static se.alipsa.matrix.core.ValueConverter.asBigDecimal;
import static se.alipsa.matrix.core.ValueConverter.asDouble;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import se.alipsa.matrix.core.Stat;
import tech.tablesaw.columns.Column;

public class BigDecimalAggregateFunctions {

  public static final NumberAggregateFunction mean =
      new NumberAggregateFunction("Mean") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return Stat.mean(toList(column));
        }
      };

  public static final NumberAggregateFunction median =
      new NumberAggregateFunction("Median") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return Stat.median(toList(column));
        }
      };

  public static final NumberAggregateFunction cv =
      new NumberAggregateFunction("CV") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          List<Double> nums = toDoubleList(column);
          return asBigDecimal(Math.sqrt(Stat.variance(nums)) / Stat.mean(nums).doubleValue());
        }
      };

  /**
   * A function that takes a {@link NumericColumn} argument and returns the sum of the values in the
   * column
   */
  public static final NumberAggregateFunction sum =
      new NumberAggregateFunction("Sum") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return asBigDecimal(Stat.sum(toList(column)));
        }
      };

  public static final NumberAggregateFunction range =
      new NumberAggregateFunction("Range") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          List<BigDecimal> data = toBigDecimalList(column);
          return Stat.max(data).subtract(Stat.min(data));
        }
      };

  public static final NumberAggregateFunction min =
      new NumberAggregateFunction("Min") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return Stat.min(toBigDecimalList(column));
        }
      };

  /**
   * A function that takes a {@link NumericColumn} argument and returns the largeset value in the
   * column
   */
  public static final NumberAggregateFunction max =
      new NumberAggregateFunction("Max") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return asBigDecimal(Stat.max(toList(column)));
        }
      };

  static List<Number> toList(Column<BigDecimal> column) {
    List<Number> list = new ArrayList<>();
    for (var v : column) {
      list.add(v);
    }
    return list;
  }

  static List<BigDecimal> toBigDecimalList(Column<BigDecimal> column) {
    List<BigDecimal> list = new ArrayList<>();
    for (var v : column) {
      list.add(asBigDecimal(v));
    }
    return list;
  }

  static List<Double> toDoubleList(Column<BigDecimal> column) {
    List<Double> list = new ArrayList<>();
    for (var v : column) {
      list.add(asDouble(v));
    }
    return list;
  }
}
