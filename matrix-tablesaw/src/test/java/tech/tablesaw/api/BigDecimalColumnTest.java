package tech.tablesaw.api;

import static org.junit.jupiter.api.Assertions.*;
import static tech.tablesaw.columns.numbers.fillers.DoubleRangeIterable.range;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.BigDecimalColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.column.numbers.BigDecimalColumnFormatter;
import tech.tablesaw.column.numbers.BigDecimalColumnType;
import tech.tablesaw.columns.AbstractColumnParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;

public class BigDecimalColumnTest {

  BigDecimal[] values = bdArr(9, "1200", null, "3456", "12.1", "3456.4", "985", "1211.9", null, "12.1");
  BigDecimalColumn obs = BigDecimalColumn.create("values", values);
  @Test
  public void testGetString() {
    obs.setPrintFormatter(new BigDecimalColumnFormatter(NumberFormat.getNumberInstance(Locale.US)));
    assertEquals("12.1", obs.getString(3));

    // TODO is this what we should expect?
    assertEquals("", obs.getString(1));
  }

  @Test
  public void testSize() {
    assertEquals(9, obs.size());
  }

  @Test
  public void testCopyAndSetMissing() {
    var copy = obs.copy();
    copy.setMissing(0);
    assertEquals(BigDecimalColumnType.missingValueIndicator(), copy.get(0));
  }

  @Test
  void testCopyAndSetWithPredicate(){
    var copy = obs.copy();
    var replacementValues = new DoubleArrayList(new double[]{1200, 2, 3456, 12.1, 3456.4, 985, 1211.9, 8, 12.1});
    var replacementColumn = new DoubleColumn("replacement", replacementValues);
    copy.set(Objects::isNull, replacementColumn);
    assertEquals(BigDecimal.valueOf(2.0), copy.get(1));
    assertEquals(BigDecimal.valueOf(8.0), copy.get(7));
  }

  @Test
  public void testCopyAndSetWithString() {
    var copy = obs.copy();
    copy.set(3, "-23.8", BigDecimalColumnType.DEFAULT_PARSER);
    assertEquals(new BigDecimal("-23.8"), copy.getBigDecimal(3));
  }

  @Test
  public void setFromColumn() {
    var copy = obs.copy();
    copy.set(5, BigDecimalColumn.create("another", List.of(BigDecimal.valueOf(111), new BigDecimal("3.14"))), 1);
    assertEquals(new BigDecimal("3.14"), copy.get(5));
  }

  @Test
  public void testWhere() {
    var selection = obs.where(obs.isGreaterThan(1000.0));
    assertEquals(4, selection.size());
  }

  @Test
  public void testIsNotIn() {
    var excluded = obs.isNotIn(BigDecimal.valueOf(1200), new BigDecimal("12.1"));
    assertEquals(6, excluded.size(), "all not 12.1");
    excluded = obs.isNotIn((BigDecimal) null);
    assertEquals(7, excluded.size(), "all not null");
  }

  @Test
  public void testIsIn() {
    var included = obs.isIn(BigDecimal.valueOf(1200), new BigDecimal("12.1"));
    assertEquals(3, included.size(), "contains 1200 and 12.1");
    included = obs.isIn((BigDecimal) null);
    assertEquals(2, included.size(), "number of nulls");
  }

  @Test
  public void testSubset() {
    var subset = obs.subset(new int[]{0,1,6});
    assertEquals(3, subset.size(), "subset of row 0,1,6");
  }

  @Test
  public void testUnique(){
    var actual = obs.unique().asBigDecimalArray();
    assertArrayEquals(bdArr(9, "1200", null, "3456", "12.1", "3456.4", "985", "1211.9"),
        actual ,
        "unique values: " + Arrays.asList(actual));
  }

  @Test
  public void testTop() {
    assertArrayEquals(bdArr(9, "3456.4", "3456", "1211.9"), obs.top(3).asBigDecimalArray());
  }

  @Test
  public void testBottom() {
    assertArrayEquals(bdArr(9, "12.1", "12.1", "985"), obs.bottom(3).asObjectArray());
  }

  @Test
  public void testLag() {
    var previous = obs.lag(1);
    assertArrayEquals(bdArr(9, null, "1200", null, "3456", "12.1", "3456.4", "985", "1211.9", null), previous.asBigDecimalArray(), "lag 1");
  }

  @Test
  public void testGetDouble() {
    assertEquals(
        3456.4,
        obs.getDouble(4),
        "getDouble"
    );
  }

  @Test
  public void testRemoveMissing() {
    assertArrayEquals(
        bdArr(9, "1200", "3456", "12.1", "3456.4", "985", "1211.9", "12.1"),
        obs.removeMissing().asBigDecimalArray(),
        "remove missing"
    );
  }

  @Test
  public void testAppendBigDecimal() {
    var appended = obs.copy().append(new BigDecimal("123.333"));
    assertEquals(new BigDecimal("123.333"), appended.get(9), "Append BigDecimal");
  }

  @Test
  public void testAppendFloat() {
    var appended = obs.copy().append(123.333f);
    assertEquals(123.333f, appended.get(9).floatValue(), "Append float");
  }

  @Test
  public void testAppendDouble() {
    var appended = obs.copy().append(123.333d);
    assertEquals(123.333d, appended.get(9).doubleValue(), "Append double");
  }

  @Test
  public void testAppendInt() {
    var appended = obs.copy().append(1231);
    assertEquals(1231, appended.get(9).intValue(), "Append int");
  }

  @Test
  public void testAppendLong() {
    var appended = obs.copy().append(1231123411234511234L);
    assertEquals(1231123411234511234L, appended.get(9).longValue(), "Append long");
  }

  @Test
  public void testAppendNumber() {
    var appended = obs.copy().append(BigInteger.valueOf(1231123411234511234L));
    assertEquals(BigDecimal.valueOf(1231123411234511234L), appended.get(9), "Append number");
  }

  @Test
  public void testAppendString() {
    var appended = obs.copy().append("123.333444666777888999");
    assertEquals(new BigDecimal("123.333444666777888999"), appended.get(9), "Append String");
  }

  @Test
  public void testAppendColumn() {
    var nums = BigDecimalColumn.create(
        "nums",
        BigDecimal.valueOf(12), new BigDecimal("15.123"), BigDecimal.valueOf(26.3)
    );
    var col = obs.copy().append(nums);
    assertEquals(12, col.size(), "appended column size");
    assertEquals(
        new BigDecimal("15.123"),
        col.getBigDecimal(10),
        "value of appended col at index 10"
    );
  }

  @Test
  public void testAppendValueFromColumn() {
    var nums = BigDecimalColumn.create(
        "nums",
        BigDecimal.valueOf(12), new BigDecimal("15.123"), BigDecimal.valueOf(26.3)
    );
    var col = obs.copy().append(nums, 2);
    assertEquals(10, col.size(), "appended column size");
    assertEquals(
        BigDecimal.valueOf(26.3),
        col.getBigDecimal(9),
        "value of appended value in col");
  }

  @Test
  public void testAppendMissing() {
    var col = obs.copy().appendMissing();
    assertEquals(10, col.size(), "appended column size");
    assertNull(col.getBigDecimal(9), "value of appended missing");
  }

  @Test
  public void testAppendObject() {
    Object object = new BigDecimal("1211.1212");
    assertEquals(
        object,
        obs.copy().appendObj(object).get(9)
    );
  }

  @Test
  public void testAppendCell() {
    String val = "1231.1232";
    assertEquals(
        new BigDecimal(val),
        obs.copy().appendCell(val).get(9)
    );
  }

  @Test
  public void testAppendCellWithParser() {
    var parser = new AbstractColumnParser<BigDecimal>(BigDecimalColumnType.instance()) {
      @Override
      public boolean canParse(String s) {
        return true;
      }
      @Override
      public BigDecimal parse(String s) {
        return s == null ? null : new BigDecimal(s.replaceAll(",", "\\."));
      }
    };

    assertEquals(
        new BigDecimal("101.234"),
        obs.copy().appendCell("101,234", parser).get(9)
    );

  }

  @Test
  public void testAppendCellWithParserMissingValue() {
    var parser = new AbstractColumnParser<BigDecimal>(BigDecimalColumnType.instance()) {
      @Override
      public boolean canParse(String s) {
        return true;
      }
      @Override
      public BigDecimal parse(String s) {
        return "NA".equals(s) ? null : new BigDecimal(s);
      }
    };

    var col = obs.copy().appendCell("NA", parser);
    assertEquals(10, col.size(), "appended column size");
    assertNull(col.getBigDecimal(9), "value of appended missing");
  }

  @Test
  public void testGetUnformattedString() {
    assertEquals("", obs.getUnformattedString(1), "unformatted string for null");
    assertEquals("1200.000000000", obs.getUnformattedString(0));
  }

  @Test
  public void testValueHash() {
    assertEquals(new BigDecimal("1211.9").setScale(9, RoundingMode.HALF_EVEN).hashCode(),
        obs.valueHash(6), "Hashcode");
  }

  @Test
  public void testEquals() {
    assertTrue(obs.equals(3,8), "Column equals");
  }

  @Test
  public void testIterator() {
    int i = 0;
    for (BigDecimal val : obs) {
      assertEquals(values[i++], val, "Iterator");
    }
  }

  @Test
  public void testCompare() {
    var bd1 = new BigDecimal("345.678");
    var bd2 = new BigDecimal("12.123");
    var bd3 = new BigDecimal("678.56");
    assertEquals(bd1.compareTo(bd2), obs.compare(bd1, bd2));
    assertEquals(bd1.compareTo(bd3), obs.compare(bd1, bd3));
  }

  @Test
  public void testFilter() {
    assertArrayEquals(bdArr(9, "12.1", "985", "12.1"),
        obs.filter(p -> p.compareTo(new BigDecimal("985").setScale(9, RoundingMode.HALF_EVEN)) < 1)
            .asBigDecimalArray());
  }

  @Test
  public void testAsBytes() {
    var exp = values[4].toString();
    assertArrayEquals(
        exp.getBytes(),
        obs.asBytes(4),
        "as bytes: " + exp + " vs " + new String(obs.asBytes(4))
    );
  }

  @Test
  public void testAsSet() {
    var exp = new HashSet<>(Arrays.asList(values));
    assertEquals(exp, obs.asSet(), "as set");
  }

  @Test
  public void testCountUnique() {
    assertEquals(
        7,
        obs.countUnique(),
        "unique value count"
    );
  }

  @Test
  public void testIsMissingValue() {
    assertTrue(obs.isMissingValue(null));
    assertFalse(obs.isMissingValue(BigDecimal.valueOf(12)));
  }

  @Test
  public void testIsMissing() {
    assertTrue(obs.isMissing(1));
    assertFalse(obs.isMissing(0));
  }

  @Test
  public void testSortAscending() {
    var actual = obs.copy();
    actual.sortAscending();
    assertArrayEquals(
        bdArr(9, null, null, "12.1", "12.1", "985", "1200", "1211.9", "3456", "3456.4"),
        actual.asBigDecimalArray()
    );
  }

  @Test
  public void testSortDescending() {
    var actual = obs.copy();
    actual.sortDescending();
    assertArrayEquals(
        bdArr(9, "3456.4", "3456", "1211.9", "1200", "985", "12.1", "12.1", null, null),
        actual.asBigDecimalArray()
    );
  }

  @Test
  public void testFillWith() {
    var col = BigDecimalColumn.create("test", 5);
    var arr = bdArr(123, 234, 345, 1.1, 1.2);
    col.fillWith(Arrays.asList(arr).iterator());
    assertArrayEquals(arr, col.asBigDecimalArray(), "fillWith bigdecimals");

    col = BigDecimalColumn.create("test", 5);
    var arr2 = DoubleColumn.create("doubles", new Double[]{123.0, 234.2, 345d, 1.1, 1.2});
    col.fillWith((DoubleListIterator)arr2.iterator());
    assertArrayEquals(arr2.asDoubleArray(), col.asDoubleArray(), "fillWith doubles");

    col = BigDecimalColumn.create("test", 5);
    var arr3 = DoubleColumn.create("doubles", new Double[]{123.0, 234.2, 345d, 1.1, 1.2});
    col.fillWith(arr3.range());
    // 345 - 1.1 = 343.9
    assertArrayEquals(
        new double[]{343.9, 343.9, 343.9, 343.9, 343.9},
        col.asDoubleArray(),
        "fillWith double range"
    );

    col = BigDecimalColumn.create("test", 5);
    var actual = col.fillWith(range(1.0, 12.0, 3.1));
    assertArrayEquals(
        new double[]{1.0, 4.1, 7.2, 10.3, 1.0},
        actual.asDoubleArray(), 0.0000001,
        "fill with range: " + actual.print()
    );

    col = BigDecimalColumn.create("test", 5);
    var val = new BigDecimal("234.65654");
    col.fillWith(new BigDecimal("234.65654"));
    assertEquals(val.multiply(
        BigDecimal.valueOf(5)).setScale(5, RoundingMode.HALF_EVEN),
        BigDecimal.valueOf(col.sum()).setScale(5, RoundingMode.HALF_EVEN),
        "fillWith bigdecimal"
    );
  }

  @Test
  public void testAsLongColumn() {
    assertArrayEquals(
        new Long[]{1200L, null, 3456L, 12L, 3456L, 985L, 1211L, null, 12L},
        obs.asLongColumn().asObjectArray(),
        "as long column");
  }

  @Test
  public void testAsIntColumn() {
    assertArrayEquals(
        new Integer[]{1200, null, 3456, 12, 3456, 985, 1211, null, 12},
        obs.asIntColumn().asObjectArray(),
        "as int column");
  }

  @Test
  public void testAsShortColumn() {
    assertArrayEquals(
        new Short[]{1200, null, 3456, 12, 3456, 985, 1211, null, 12},
        obs.asShortColumn().asObjectArray(),
        "as short column");
  }

  @Test
  public void testAsFloatColumn() {
    assertArrayEquals(
        new Float[]{1200f, null, 3456f, 12.1f, 3456.4f, 985f, 1211.9f, null, 12.1f},
        obs.asFloatColumn().asObjectArray(),
        "as float column"
    );
  }

  @Test
  public void testAddAll() {
    var col = BigDecimalColumn.create("test");
    col.addAll(List.of(new BigDecimal("99.123"), BigDecimal.valueOf(445)));
    assertEquals(2, col.size());
    assertEquals(new BigDecimal("99.123"), col.getBigDecimal(0));
  }

  @Test
  public void testSetPrintFormatter() {

    var formatter = new BigDecimalColumnFormatter(NumberFormat.getNumberInstance(Locale.US));
    assertNotNull(formatter.getFormat(), "No format assigned");
    formatter.getFormat().setMinimumFractionDigits(3);
    obs.setPrintFormatter(formatter);
    var nl = System.lineSeparator();
    assertEquals("Column: values" + nl
                    + "1,200.000" + nl
                    + nl
                    + "3,456.000" + nl
                    + "12.100" + nl
                    + "3,456.400" + nl
                    + "985.000" + nl
                    + "1,211.900" + nl
                    + nl
                    + "12.100" + nl, obs.print());
  }

  @Test
  void testAddition() {
    assertArrayEquals(
        bdArr(9, "1201.1", null, "3457.1", "13.2", "3457.5", "986.1", "1213.0", null, "13.2"),
        obs.add(BigDecimalColumn.create("plus",
            bdArr(1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1))).asBigDecimalArray()
    );
  }

  @Test
  void testSubtraction() {
    assertArrayEquals(
        new double[]{1200-1.1, Double.NaN, 3456-1.1, 12.1-1.1, 3456.4-1.1, 985-1.1, 1211.9-1.1, Double.NaN, 12.1-1.1},
        obs.subtract(BigDecimalColumn.create("minus",
            bdArr(1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1))).asDoubleArray(),
        0.00000001
    );
  }

  @Test
  void testMultiply() {
    assertArrayEquals(
        new double[]{1200*1.1, Double.NaN, 3456*1.1, 12.1*1.1, 3456.4*1.1, 985*1.1, 1211.9*1.1, Double.NaN, 12.1*1.1},

        obs.multiply(BigDecimalColumn.create("multiply",
            bdArr(1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1))).asDoubleArray(),
        0.00000001
    );
  }

  @Test
  void testDivide() {
    assertArrayEquals(
        bdArr(9, "1090.909090909", null, "3141.818181818", "11.000000000", "3142.181818182", "895.454545455", "1101.727272727", null, "11.000000000"),
        obs.copy().divide(BigDecimalColumn.create("divide", bdArr(1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1)))
            .setScale(9)
            .asBigDecimalArray()
    );
  }

  @Test
  void testApply() {
    var exp = new BigDecimal[values.length];
    int i = 0;
    for (var val : values) {
      exp[i++] = val == null ? null : val.pow(3).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN);
    }
    var act = obs.copy().apply(bd -> bd.pow(3).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN));
    assertArrayEquals(exp, act.asObjectArray());
  }

  @Test
  void testAppendAtomicTypes() {
    var col = BigDecimalColumn.create("test");

    // Test AtomicInteger
    col.append(new AtomicInteger(42));
    assertEquals(BigDecimal.valueOf(42), col.get(0), "AtomicInteger conversion");

    // Test AtomicLong
    col.append(new AtomicLong(9876543210L));
    assertEquals(BigDecimal.valueOf(9876543210L), col.get(1), "AtomicLong conversion");

    // Test DoubleAccumulator
    DoubleAccumulator accumulator = new DoubleAccumulator(Double::sum, 0.0);
    accumulator.accumulate(123.456);
    col.append(accumulator);
    assertEquals(BigDecimal.valueOf(123.456), col.get(2), "DoubleAccumulator conversion");

    assertEquals(3, col.size(), "Column size after appending atomic types");
  }

  @Test
  void testAppendBigDecimalNoPrecisionLoss() {
    var col = BigDecimalColumn.create("test");

    // Test that BigDecimal is returned as-is without precision loss
    BigDecimal preciseValue = new BigDecimal("123.456789012345678901234567890");
    col.append(preciseValue);

    // Verify the value is preserved exactly (no double conversion)
    assertEquals(preciseValue, col.get(0), "BigDecimal should be preserved without precision loss");
    assertEquals(preciseValue.toPlainString(), col.get(0).toPlainString(), "String representation should match exactly");
  }

  private BigDecimal[] bdArr(Number... numbers) {
    BigDecimal[] arr = new BigDecimal[numbers.length];
    for (int i = 0; i < numbers.length; i++) {
      Number num = numbers[i];
      arr[i] = num == null ? null : num instanceof Long || num instanceof Integer
          ? BigDecimal.valueOf(num.longValue())
          : BigDecimal.valueOf(num.doubleValue());
    }
    return arr;
  }

  private BigDecimal[] bdArr(int numDecimals, String... numbers) {
    BigDecimal[] arr = new BigDecimal[numbers.length];
    for (int i = 0; i < numbers.length; i++) {
      String num = numbers[i];
      arr[i] = num == null ? null : new BigDecimal(num).setScale(numDecimals, RoundingMode.HALF_EVEN);
    }
    return arr;
  }

}
