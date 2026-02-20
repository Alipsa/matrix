package tech.tablesaw.api;

import static se.alipsa.matrix.core.ValueConverter.asBigDecimal;
import static se.alipsa.matrix.core.ValueConverter.asDouble;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import se.alipsa.matrix.core.Stat;
import tech.tablesaw.columns.Column;

/**
 * A collection of aggregate functions that can be applied to {@link BigDecimalColumn} instances.
 */
public class BigDecimalAggregateFunctions {

  /**
   * Utility class.
   */
  private BigDecimalAggregateFunctions() {
  }

  /**
   * A function that takes a {@link NumericColumn} argument and returns the mean of the values in
   * the column
   */
  public static final NumberAggregateFunction mean =
      new NumberAggregateFunction("Mean") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return Stat.mean(toList(column));
        }
      };

  /**
   * A function that takes a {@link NumericColumn} argument and returns the median of the values in
   * the column
   */
  public static final NumberAggregateFunction median =
      new NumberAggregateFunction("Median") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return Stat.median(toList(column));
        }
      };

  /**
   * A function that takes a {@link NumericColumn} argument and returns the coefficient of variation
   * (normalized root-mean-square deviation) of the values in the column
   */
  public static final NumberAggregateFunction cv =
      new NumberAggregateFunction("CV") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          List<Double> nums = toDoubleList(column);
          return Stat.variance(nums).sqrt(MathContext.DECIMAL64).divide(Stat.mean(nums), MathContext.DECIMAL64);
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

  /**
   * A function that takes a {@link NumericColumn} argument and returns the range ({@code max - min})
   * of the values in the column.
   */
  public static final NumberAggregateFunction range =
      new NumberAggregateFunction("Range") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          List<BigDecimal> data = toBigDecimalList(column);
          return Stat.max(data).subtract(Stat.min(data));
        }
      };

  /**
   * A function that takes a {@link NumericColumn} argument and returns the smallest value in the
   * column
   */
  public static final NumberAggregateFunction min =
      new NumberAggregateFunction("Min") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return Stat.min(toBigDecimalList(column));
        }
      };

  /**
   * A function that takes a {@link NumericColumn} argument and returns the largest value in the
   * column
   */
  public static final NumberAggregateFunction max =
      new NumberAggregateFunction("Max") {

        @Override
        public BigDecimal summarize(BigDecimalColumn column) {
          return asBigDecimal(Stat.max(toList(column)));
        }
      };


  /**
   * Converts a Column<BigDecimal> to a List<Number>
   *
   * @param column the column to convert
   * @return a new, converted list
   */
  static List<Number> toList(Column<BigDecimal> column) {
    List<Number> list = new ArrayList<>();
    for (var v : column) {
      list.add(v);
    }
    return list;
  }

  /**
   * Converts a Column<BigDecimal> to a List<BigDecimal>
   *
   * @param column the column to convert
   * @return a new, converted list
   */
  static List<BigDecimal> toBigDecimalList(Column<BigDecimal> column) {
    List<BigDecimal> list = new ArrayList<>();
    for (var v : column) {
      list.add(asBigDecimal(v));
    }
    return list;
  }

  /**
   * Converts a Column<BigDecimal> to a List<Double>
   *
   * @param column the column to convert
   * @return a new, converted list
   */
  static List<Double> toDoubleList(Column<BigDecimal> column) {
    List<Double> list = new ArrayList<>();
    for (var v : column) {
      list.add(asDouble(v));
    }
    return list;
  }
}
