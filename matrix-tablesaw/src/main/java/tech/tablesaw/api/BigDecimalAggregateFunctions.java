package tech.tablesaw.api;

import static se.alipsa.matrix.core.ValueConverter.asBigDecimal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import se.alipsa.matrix.core.Stat;
import tech.tablesaw.columns.Column;

/**
 * A collection of aggregate functions that can be applied to {@link BigDecimalColumn} instances.
 * Mean, median, range, minimum, and maximum return {@code null} for empty or all-missing columns;
 * coefficient of variation returns {@code null} when fewer than two non-missing values remain.
 * Sum retains its empty-input convention of zero.
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
          List<BigDecimal> values = nonMissingValues(column);
          return values.isEmpty() ? null : Stat.mean(values);
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
          List<BigDecimal> values = nonMissingValues(column);
          return values.isEmpty() ? null : Stat.median(values);
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
          List<BigDecimal> nums = nonMissingValues(column);
          if (nums.size() < 2) return null;
          BigDecimal mean = Stat.mean(nums);
          if (mean.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot compute CV: mean is zero");
          }
          return Stat.variance(nums).sqrt(MathContext.DECIMAL64).divide(mean, MathContext.DECIMAL64);
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
          return asBigDecimal(Stat.sum(nonMissingValues(column)));
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
          List<BigDecimal> data = nonMissingValues(column);
          if (data.isEmpty()) return null;
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
          List<BigDecimal> values = nonMissingValues(column);
          return values.isEmpty() ? null : Stat.min(values);
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
          List<BigDecimal> values = nonMissingValues(column);
          return values.isEmpty() ? null : Stat.max(values);
        }
      };


  /**
   * Collects the non-missing values of a column into a new list.
   *
   * @param column the column to collect from
   * @return a new list containing only the non-missing values
   */
  static List<BigDecimal> nonMissingValues(Column<BigDecimal> column) {
    List<BigDecimal> list = new ArrayList<>();
    for (var v : column) {
      if (v != null) {
        list.add(v);
      }
    }
    return list;
  }
}
