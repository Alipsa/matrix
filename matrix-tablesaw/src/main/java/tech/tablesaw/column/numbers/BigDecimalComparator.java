package tech.tablesaw.column.numbers;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class BigDecimalComparator implements Comparator<BigDecimal> {


  public static int compareBigDecimals(BigDecimal o1, BigDecimal o2) {
    if ((o1 == null) && (o2 == null)) return 0;
    if ((o1 != null) && (o2 == null)) return 1;
    if (o1 == null) return -1;
    return o1.compareTo(o2);
  }

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
