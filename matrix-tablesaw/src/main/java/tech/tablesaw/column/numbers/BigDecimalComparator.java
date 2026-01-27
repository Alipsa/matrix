package tech.tablesaw.column.numbers;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Comparator for BigDecimal values with null-safe comparison.
 *
 * <p>This comparator handles null values by treating them as less than any non-null value.
 * When both values are null, they are considered equal. For non-null values, it delegates
 * to {@link BigDecimal#compareTo(BigDecimal)}.
 *
 * <p>Comparison rules:
 * <ul>
 *   <li>null == null (returns 0)</li>
 *   <li>null &lt; non-null (returns -1)</li>
 *   <li>non-null &gt; null (returns 1)</li>
 *   <li>For two non-null values, uses BigDecimal.compareTo()</li>
 * </ul>
 *
 * @see BigDecimalColumnType
 */
public class BigDecimalComparator implements Comparator<BigDecimal> {

  /**
   * Compares two BigDecimal values with null-safe logic.
   *
   * @param o1 the first BigDecimal to compare (may be null)
   * @param o2 the second BigDecimal to compare (may be null)
   * @return 0 if both are null or equal, -1 if o1 is null or less than o2, 1 if o2 is null or o1 is greater than o2
   */
  public static int compareBigDecimals(BigDecimal o1, BigDecimal o2) {
    if ((o1 == null) && (o2 == null)) return 0;
    if ((o1 != null) && (o2 == null)) return 1;
    if (o1 == null) return -1;
    return o1.compareTo(o2);
  }

  /**
   * Compares two BigDecimal values.
   *
   * @param o1 the first BigDecimal to compare
   * @param o2 the second BigDecimal to compare
   * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second
   */
  @Override
  public int compare(BigDecimal o1, BigDecimal o2) {
    return compareBigDecimals(o1, o2);
  }

  @Override
  public Comparator<BigDecimal> reversed() {
    return Comparator.super.reversed();
  }

  @Override
  public Comparator<BigDecimal> thenComparing(Comparator<? super BigDecimal> other) {
    return Comparator.super.thenComparing(other);
  }

  @Override
  public <U> Comparator<BigDecimal> thenComparing(Function<? super BigDecimal, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
    return Comparator.super.thenComparing(keyExtractor, keyComparator);
  }

  @Override
  public <U extends Comparable<? super U>> Comparator<BigDecimal> thenComparing(Function<? super BigDecimal, ? extends U> keyExtractor) {
    return Comparator.super.thenComparing(keyExtractor);
  }

  @Override
  public Comparator<BigDecimal> thenComparingInt(ToIntFunction<? super BigDecimal> keyExtractor) {
    return Comparator.super.thenComparingInt(keyExtractor);
  }

  @Override
  public Comparator<BigDecimal> thenComparingLong(ToLongFunction<? super BigDecimal> keyExtractor) {
    return Comparator.super.thenComparingLong(keyExtractor);
  }

  @Override
  public Comparator<BigDecimal> thenComparingDouble(ToDoubleFunction<? super BigDecimal> keyExtractor) {
    return Comparator.super.thenComparingDouble(keyExtractor);
  }
}
