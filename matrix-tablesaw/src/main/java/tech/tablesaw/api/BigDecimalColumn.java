package tech.tablesaw.api;


import static com.google.common.base.Preconditions.checkArgument;
import static tech.tablesaw.column.numbers.BigDecimalComparator.compareBigDecimals;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.*;
import tech.tablesaw.column.numbers.BigDecimalColumnFormatter;
import tech.tablesaw.column.numbers.BigDecimalColumnType;
import tech.tablesaw.column.numbers.BigDecimalComparator;
import tech.tablesaw.column.numbers.BigDecimalParser;
import tech.tablesaw.columns.AbstractColumnParser;
import tech.tablesaw.columns.Column;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;
import tech.tablesaw.columns.numbers.NumberFillers;
import tech.tablesaw.columns.numbers.fillers.DoubleRangeIterable;
import tech.tablesaw.selection.BitmapBackedSelection;
import tech.tablesaw.selection.Selection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BigDecimalColumn extends NumberColumn<BigDecimalColumn, BigDecimal>
    implements NumberFillers<BigDecimalColumn> {

  protected final ObjectArrayList<BigDecimal> data;

  public static BigDecimalColumn create(String name, double... arr) {
    final BigDecimal[] values = new BigDecimal[arr.length];
    for (int i = 0; i < arr.length; i++) {
      values[i] = BigDecimal.valueOf(arr[i]);
    }
    return new BigDecimalColumn(name, new ObjectArrayList<>(values));
  }

  public static BigDecimalColumn create(String name, float... arr) {
    final BigDecimal[] values = new BigDecimal[arr.length];
    for (int i = 0; i < arr.length; i++) {
      values[i] = BigDecimal.valueOf(arr[i]);
    }
    return new BigDecimalColumn(name, new ObjectArrayList<>(values));
  }

  public static BigDecimalColumn create(String name, int... arr) {
    final BigDecimal[] values = new BigDecimal[arr.length];
    for (int i = 0; i < arr.length; i++) {
      values[i] = BigDecimal.valueOf(arr[i]);
    }
    return new BigDecimalColumn(name, new ObjectArrayList<>(values));
  }

  public static BigDecimalColumn create(String name, long... arr) {
    final BigDecimal[] values = new BigDecimal[arr.length];
    for (int i = 0; i < arr.length; i++) {
      values[i] = BigDecimal.valueOf(arr[i]);
    }
    return new BigDecimalColumn(name, new ObjectArrayList<>(values));
  }

  public static BigDecimalColumn create(String name, Collection<? extends Number> numberList) {
    BigDecimalColumn newColumn = new BigDecimalColumn(name, new ObjectArrayList<>(0));
    for (Number number : numberList) {
      newColumn.append(toBigDecimal(number));
    }
    return newColumn;
  }

  public static BigDecimalColumn create(String name, Number[] numbers) {
    BigDecimalColumn newColumn = new BigDecimalColumn(name, new ObjectArrayList<>(0));
    for (Number number : numbers) {
      newColumn.append(toBigDecimal(number));
    }
    return newColumn;
  }

  public static BigDecimalColumn create(String name, int initialSize) {
    BigDecimalColumn column = new BigDecimalColumn(name);
    for (int i = 0; i < initialSize; i++) {
      column.appendMissing();
    }
    return column;
  }

  public static BigDecimalColumn create(String name, Stream<BigDecimal>stream) {
    ObjectArrayList<BigDecimal> list = new ObjectArrayList<>();
    stream.forEach(list::add);
    return new BigDecimalColumn(name, list);
  }

  protected BigDecimalColumn(String name, ObjectArrayList<BigDecimal> data) {
    super(BigDecimalColumnType.instance(), name, BigDecimalColumnType.DEFAULT_PARSER);
    setPrintFormatter(BigDecimalColumnFormatter.floatingPointDefault());
    this.data = data;
  }

  protected BigDecimalColumn(String name) {
    super(BigDecimalColumnType.instance(), name, BigDecimalColumnType.DEFAULT_PARSER);
    setPrintFormatter(BigDecimalColumnFormatter.floatingPointDefault());
    this.data = new ObjectArrayList<>(DEFAULT_ARRAY_SIZE);
  }

  public static BigDecimalColumn create(String name, BigDecimal... arr) {
    return new BigDecimalColumn(name, new ObjectArrayList<>(arr));
  }

  public static BigDecimalColumn create(String name) {
    return new BigDecimalColumn(name);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn createCol(String name, int initialSize) {
    return create(name, initialSize);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn createCol(String name) {
    return create(name);
  }

  /** {@inheritDoc} */
  @Override
  public String getString(int row) {
    final BigDecimal value = getBigDecimal(row);
    return String.valueOf(getPrintFormatter().format(value));
  }

  /** {@inheritDoc} */
  @Override
  public int size() {
    return data.size();
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    data.clear();
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn setMissing(int index) {
    set(index, BigDecimalColumnType.missingValueIndicator());
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimal get(int index) {
    BigDecimal result = getBigDecimal(index);
    return isMissingValue(result) ? null : result;
  }

  protected BigDecimal getBigDecimal(int index) {
    return data.get(index);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn where(Selection selection) {
    return (BigDecimalColumn) super.where(selection);
  }

  public Selection isNotIn(final BigDecimal... values) {
    final Selection results = new BitmapBackedSelection();
    results.addRange(0, size());
    results.andNot(isIn(values));
    return results;
  }

  public Selection isIn(final BigDecimal... values) {
    final Selection results = new BitmapBackedSelection();
    final List<BigDecimal> valueList = Arrays.asList(values);
    final List<BigDecimal> filtered = valueList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    final ObjectRBTreeSet<BigDecimal> doubleSet = new ObjectRBTreeSet<>(filtered);
    for (int i = 0; i < size(); i++) {
      BigDecimal val = getBigDecimal(i);
      if (val == null && valueList.contains(null)) {
        results.add(i);
      } else if (val != null && doubleSet.contains(val)) {
        results.add(i);
      }
    }
    return results;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn subset(int[] rows) {
    final BigDecimalColumn c = this.emptyCopy();
    for (final int row : rows) {
      c.append(getBigDecimal(row));
    }
    return c;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn unique() {
    final ObjectSet<BigDecimal> values = new ObjectLinkedOpenHashSet<>();
    for (int i = 0; i < size(); i++) {
      values.add(getBigDecimal(i));
    }
    final BigDecimalColumn column = BigDecimalColumn.create(name() + " Unique values");
    values.forEach(column::append);
    return column;
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public BigDecimalColumn top(int n) {
    ObjectArrayList<BigDecimal> top = new ObjectArrayList<>();
    // parallelQuickSort cannot handle null values, but they make no sense to order anyway to we remove them
    List<BigDecimal> cleaned = data.stream().filter(Objects::nonNull).toList();
    BigDecimal[] values = cleaned.toArray(new BigDecimal[]{});
    ObjectArrays.parallelQuickSort(values, ObjectComparators.OPPOSITE_COMPARATOR);
    for (int i = 0; i < n && i < values.length; i++) {
      top.add(values[i]);
    }
    return new BigDecimalColumn(name() + "[Top " + n + "]", top);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn bottom(final int n) {
    ObjectArrayList<BigDecimal> bottom = new ObjectArrayList<>();
    // parallelQuickSort cannot handle null values, but they make no sense to order anyway to we remove them
    List<BigDecimal> cleaned = data.stream().filter(Objects::nonNull).collect(Collectors.toList());
    BigDecimal[] values = cleaned.toArray(new BigDecimal[]{});
    ObjectArrays.parallelQuickSort(values);
    for (int i = 0; i < n && i < values.length; i++) {
      bottom.add(values[i]);
    }
    return new BigDecimalColumn(name() + "[Bottoms " + n + "]", bottom);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn lag(int n) {
    final int srcPos = n >= 0 ? 0 : -n;
    final BigDecimal[] dest = new BigDecimal[size()];
    final int destPos = Math.max(n, 0);
    final int length = n >= 0 ? size() - n : size() + n;

    for (int i = 0; i < size(); i++) {
      dest[i] = BigDecimalColumnType.missingValueIndicator();
    }

    BigDecimal[] array = data.toArray(new BigDecimal[]{});

    System.arraycopy(array, srcPos, dest, destPos, length);
    return new BigDecimalColumn(name() + " lag(" + n + ")", new ObjectArrayList<>(dest));
  }

  @Override
  public double getDouble(int index) {
    BigDecimal bd = get(index);
    return bd == null ? Double.NaN : bd.doubleValue();
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn removeMissing() {
    BigDecimalColumn result = copy();
    result.clear();
    for (BigDecimal v : data) {
      if (!isMissingValue(v)) {
        result.append(v);
      }
    }
    return result;
  }

  /** Adds the given BigDecimal to this column */
  public BigDecimalColumn append(final BigDecimal f) {
    data.add(f);
    return this;
  }

  /** Adds the given float to this column */
  public BigDecimalColumn append(final float f) {
    data.add(BigDecimal.valueOf(f));
    return this;
  }

  /** Adds the given double to this column */
  public BigDecimalColumn append(double d) {
    data.add(BigDecimal.valueOf(d));
    return this;
  }

  public BigDecimalColumn append(int i) {
    data.add(BigDecimal.valueOf(i));
    return this;
  }

  public BigDecimalColumn append(long l) {
    data.add(BigDecimal.valueOf(l));
    return this;
  }

  public BigDecimalColumn append(Number val) {
    if (val == null) {
      appendMissing();
    } else {
      append(toBigDecimal(val));
    }
    return this;
  }

  public BigDecimalColumn append(String val) {
    if (val == null) {
      appendMissing();
    } else {
      append(parse(val));
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn append(final Column<BigDecimal> column) {
    Preconditions.checkArgument(
        column.type() == this.type(),
        "Column '%s' has type %s, but column '%s' has type %s.",
        name(),
        type(),
        column.name(),
        column.type());
    final BigDecimalColumn numberColumn = (BigDecimalColumn) column;
    final int size = numberColumn.size();
    for (int i = 0; i < size; i++) {
      append(numberColumn.getBigDecimal(i));
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn append(Column<BigDecimal> column, int row) {
    checkArgument(
        column.type() == this.type(),
        "Column '%s' has type %s, but column '%s' has type %s.",
        name(),
        type(),
        column.name(),
        column.type());
    BigDecimalColumn bdColumn = (BigDecimalColumn) column;
    return append(bdColumn.getBigDecimal(row));
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn appendMissing() {
    return append(BigDecimalColumnType.missingValueIndicator());
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn appendObj(Object obj) {
    if (obj == null) {
      return appendMissing();
    }
    if (obj instanceof BigDecimal) {
      return append(((BigDecimal) obj));
    }

    if (obj instanceof Number) {
      return append((Number) obj);
    }

    if (obj instanceof String) {
      return append((String) obj);
    }

    throw new IllegalArgumentException("Could not append " + obj.getClass());
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn appendCell(final String value) {
    try {
      return append(parse(value));
    } catch (final NumberFormatException e) {
      throw new NumberFormatException(
          "Error adding value to column " + name() + ": " + e.getMessage());
    }
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn appendCell(final String value, AbstractColumnParser<?> parser) {
    try {
      Object val = parser.parse(value);
      if (val instanceof BigDecimal) {
        return append((BigDecimal) val);
      } else if (val instanceof Number) {
        return append((Number) val);
      } else {
        return append(BigDecimal.valueOf(parser.parseDouble(value)));
      }
    } catch (final NumberFormatException e) {
      throw new NumberFormatException(
          "Error adding value to column " + name() + ": " + e.getMessage());
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getUnformattedString(final int row) {
    final BigDecimal value = getBigDecimal(row);
    if (BigDecimalColumnType.valueIsMissing(value)) {
      return "";
    }
    return String.valueOf(value);
  }

  /** {@inheritDoc} */
  @Override
  public int valueHash(int rowNumber) {
    BigDecimal value = getBigDecimal(rowNumber);
    return value == null ? Integer.MIN_VALUE : value.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(int rowNumber1, int rowNumber2) {
    BigDecimal val1 = getBigDecimal(rowNumber1);
    BigDecimal val2 = getBigDecimal(rowNumber2);
    if (val1 == null && val2 == null) return true;
    if (val1 != null) {
      return val1.equals(val2);
    }
    return false ;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn copy() {
    BigDecimalColumn copy = new BigDecimalColumn(name(), data.clone());
    copy.setPrintFormatter(getPrintFormatter());
    copy.locale = locale;
    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<BigDecimal> iterator() {
    return data.iterator();
  }

  public BigDecimal[] asBigDecimalArray() {
    return data.toArray(new BigDecimal[]{});
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimal[] asObjectArray() {
    final BigDecimal[] output = new BigDecimal[size()];
    for (int i = 0; i < size(); i++) {
      if (!isMissing(i)) {
        output[i] = getBigDecimal(i);
      } else {
        output[i] = null;
      }
    }
    return output;
  }

  /** {@inheritDoc} */
  @Override
  public int compare(BigDecimal o1, BigDecimal o2) {
    return compareBigDecimals(o1, o2);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn set(int i, BigDecimal val) {
    data.set(i, val);
    return this;
  }

  /**
   * Updates this column where values matching the selection are replaced with the corresponding
   * value from the given column
   */
  public BigDecimalColumn set(Predicate<BigDecimal> condition, NumericColumn<?> other) {
    for (int row = 0; row < size(); row++) {
      if (condition.test(getBigDecimal(row))) {
        set(row, toBigDecimal(other.get(row)));
      }
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Column<BigDecimal> set(int row, String stringValue, AbstractColumnParser<?> parser) {
    if (parser instanceof BigDecimalParser) {
      return set(row, ((BigDecimalParser)parser).parse(stringValue));
    }
    return set(row, new BigDecimal(stringValue));
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn set(int row, Column<BigDecimal> column, int sourceRow) {
    Preconditions.checkArgument(column.type() == this.type());
    BigDecimalColumn bdColumn = (BigDecimalColumn) column;
    return set(row, bdColumn.getBigDecimal(sourceRow));
  }

  /**
   * Returns a new NumberColumn with only those rows satisfying the predicate
   *
   * @param test the predicate
   * @return a new NumberColumn with only those rows satisfying the predicate
   */
  public BigDecimalColumn filter(Predicate<? super BigDecimal> test) {
    BigDecimalColumn result = BigDecimalColumn.create(name());
    for (int i = 0; i < size(); i++) {
      BigDecimal d = getBigDecimal(i);
      if (d != null && test.test(d)) {
        result.append(d);
      }
    }
    return result;
  }

  /** {@inheritDoc}
   * TODO: uses double which is imprecise
   *   Something like this should be more accurate but size will vary
   *   BigDecimal value = get(rowNumber);
   *   return value == null ? null : value.unscaledValue().toByteArray();
   *   or, alternatively
   *   BigDecimal value = get(rowNumber);
   *   return value == null ? null : value.toString().getBytes();
   */
  @Override
  public byte[] asBytes(int rowNumber) {
    BigDecimal val = getBigDecimal(rowNumber);
    return val == null ? null : val.toString().getBytes();
    /* TODO: not sure how this is supposed to work. A fixed sice byte array will either need to be huge or values will be lost
    return val == null ? null : ByteBuffer.allocate(BigDecimalColumnType.instance().byteSize())
        .put(val.toString().getBytes())
        .array();

     */
  }

  /** {@inheritDoc} */
  @Override
  public Set<BigDecimal> asSet() {
    return new HashSet<>(unique().asList());
  }

  /** {@inheritDoc} */
  @Override
  public int countUnique() {
    ObjectSet<BigDecimal> uniqueElements = new ObjectOpenHashSet<>();
    for (int i = 0; i < size(); i++) {
      uniqueElements.add(getBigDecimal(i));
    }
    return uniqueElements.size();
  }

  public boolean isMissingValue(BigDecimal value) {
    return BigDecimalColumnType.valueIsMissing(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isMissing(int rowNumber) {
    return isMissingValue(getBigDecimal(rowNumber));
  }

  /** {@inheritDoc} */
  @Override
  public void sortAscending() {
    data.sort(new BigDecimalComparator());
  }

  /** {@inheritDoc} */
  @Override
  public void sortDescending() {
    data.sort(new BigDecimalComparator().reversed());
  }


  public BigDecimalColumn fillWith(final Iterator<BigDecimal> iterator) {
    for (int r = 0; r < size(); r++) {
      if (!iterator.hasNext()) {
        break;
      }
      set(r, iterator.next());
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn fillWith(final DoubleIterator iterator) {
    for (int r = 0; r < size(); r++) {
      if (!iterator.hasNext()) {
        break;
      }
      set(r, BigDecimal.valueOf(iterator.nextDouble()));
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn fillWith(final DoubleRangeIterable iterable) {
    DoubleIterator iterator = iterable.iterator();
    for (int r = 0; r < size(); r++) {
      if (!iterator.hasNext()) {
        iterator = iterable.iterator();
        if (!iterator.hasNext()) {
          break;
        }
      }
      set(r, BigDecimal.valueOf(iterator.nextDouble()));
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn fillWith(final DoubleSupplier supplier) {
    for (int r = 0; r < size(); r++) {
      try {
        set(r, BigDecimal.valueOf(supplier.getAsDouble()));
      } catch (final Exception e) {
        break;
      }
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimalColumn fillWith(double d) {
    for (int r = 0; r < size(); r++) {
      set(r, BigDecimal.valueOf(d));
    }
    return this;
  }

  /** {@inheritDoc} */
  public BigDecimalColumn fillWith(BigDecimal d) {
    for (int r = 0; r < size(); r++) {
      set(r, d);
    }
    return this;
  }

  /**
   * Returns a new LongColumn containing a value for each value in this column, truncating if
   * necessary
   *
   * <p>A narrowing primitive conversion such as this one may lose information about the overall
   * magnitude of a numeric value and may also lose precision and range. Specifically, if the value
   * is too small (a negative value of large magnitude or negative infinity), the result is the
   * smallest representable value of type long.
   *
   * <p>Similarly, if the value is too large (a positive value of large magnitude or positive
   * infinity), the result is the largest representable value of type long.
   *
   * <p>Despite the fact that overflow, underflow, or other loss of information may occur, a
   * narrowing primitive conversion never results in a run-time exception.
   *
   * <p>A missing value in the receiver is converted to a missing value in the result
   */
  @Override
  public LongColumn asLongColumn() {
    LongColumn result = LongColumn.create(name());
    for (BigDecimal d : data) {
      if (BigDecimalColumnType.valueIsMissing(d)) {
        result.appendMissing();
      } else {
        result.append(d.longValue());
      }
    }
    return result;
  }

  /**
   * Returns a new IntColumn containing a value for each value in this column, truncating if
   * necessary.
   *
   * <p>A narrowing primitive conversion such as this one may lose information about the overall
   * magnitude of a numeric value and may also lose precision and range. Specifically, if the value
   * is too small (a negative value of large magnitude or negative infinity), the result is the
   * smallest representable value of type int.
   *
   * <p>Similarly, if the value is too large (a positive value of large magnitude or positive
   * infinity), the result is the largest representable value of type int.
   *
   * <p>Despite the fact that overflow, underflow, or other loss of information may occur, a
   * narrowing primitive conversion never results in a run-time exception.
   *
   * <p>A missing value in the receiver is converted to a missing value in the result
   */
  @Override
  public IntColumn asIntColumn() {
    IntColumn result = IntColumn.create(name());
    for (BigDecimal d : data) {
      if (BigDecimalColumnType.valueIsMissing(d)) {
        result.appendMissing();
      } else {
        result.append(d.intValue());
      }
    }
    return result;
  }

  /**
   * Returns a new ShortColumn containing a value for each value in this column, truncating if
   * necessary.
   *
   * <p>A narrowing primitive conversion such as this one may lose information about the overall
   * magnitude of a numeric value and may also lose precision and range. Specifically, if the value
   * is too small (a negative value of large magnitude or negative infinity), the result is the
   * smallest representable value of type int.
   *
   * <p>Similarly, if the value is too large (a positive value of large magnitude or positive
   * infinity), the result is the largest representable value of type short.
   *
   * <p>Despite the fact that overflow, underflow, or other loss of information may occur, a
   * narrowing primitive conversion never results in a run-time exception.
   *
   * <p>A missing value in the receiver is converted to a missing value in the result
   */
  @Override
  public ShortColumn asShortColumn() {
    ShortColumn result = ShortColumn.create(name());
    for (BigDecimal d : data) {
      if (BigDecimalColumnType.valueIsMissing(d)) {
        result.appendMissing();
      } else {
        result.append(d.shortValue());
      }
    }
    return result;
  }

  /**
   * Returns a new FloatColumn containing a value for each value in this column, truncating if
   * necessary.
   *
   * <p>A narrowing primitive conversion such as this one may lose information about the overall
   * magnitude of a numeric value and may also lose precision and range. Specifically, if the value
   * is too small (a negative value of large magnitude or negative infinity), the result is the
   * smallest representable value of type float.
   *
   * <p>Similarly, if the value is too large (a positive value of large magnitude or positive
   * infinity), the result is the largest representable value of type float.
   *
   * <p>Despite the fact that overflow, underflow, or other loss of information may occur, a
   * narrowing primitive conversion never results in a run-time exception.
   *
   * <p>A missing value in the receiver is converted to a missing value in the result
   */
  @Override
  public FloatColumn asFloatColumn() {
    FloatColumn result = FloatColumn.create(name());
    for (BigDecimal d : data) {
      if (BigDecimalColumnType.valueIsMissing(d)) {
        result.appendMissing();
      } else {
        result.append(d.floatValue());
      }
    }
    return result;
  }

  /**
   * TODO: maybe handle AtomicLong, AtomicInteger, DoubleAccumulator etc. since they are also subclasses of Number
   *
   * @param number the number to convert
   * @return as BigDecimal corresponding to the number
   */
  protected static BigDecimal toBigDecimal(Number number) {
    if (number == null) return null;
    if (number instanceof Integer
        || number instanceof Long
        || number instanceof Short
        || number instanceof Byte) {
      return BigDecimal.valueOf(number.longValue());
    }
    if (number instanceof BigInteger) {
      return new BigDecimal((BigInteger) number);
    }
    return BigDecimal.valueOf(number.doubleValue());
  }

  /**
   * Add all the big decimals in the list to this column
   *
   * @param values a list of values
   */
  public BigDecimalColumn addAll(List<BigDecimal> values) {
    for (BigDecimal value : values) {
      append(value);
    }
    return this;
  }

  protected BigDecimal parse(String val) {
    return parser().parse(val);
  }

  protected BigDecimalColumnFormatter getPrintFormatter() {
    return (BigDecimalColumnFormatter)super.getPrintFormatter();
  }

  public void setPrintFormatter(NumberColumnFormatter formatter) {
    if (formatter instanceof BigDecimalColumnFormatter) {
      super.setPrintFormatter(formatter);
    } else {
      throw new IllegalArgumentException("Formatter must be an instance of BigDecimalColumnFormatter");
    }
  }

  protected void setPrintFormatter(BigDecimalColumnFormatter formatter) {
    super.setPrintFormatter(formatter);
  }

  // BigDecimalColumn Specific methods
  public BigDecimalColumn setScale(int numDecimals, RoundingMode... roundingMode) {
    RoundingMode mode = roundingMode.length > 0 ? roundingMode[0] : RoundingMode.HALF_EVEN;
    for (int i = 0; i < size(); i++) {
      BigDecimal val = getBigDecimal(i);
      if (val != null) {
        set(i, val.setScale(numDecimals, mode));
      }
    }
    return this;
  }

  public BigDecimalColumn add(BigDecimalColumn column) {
    return plus(column);
  }

  /**
   * naming it plus() has the nice benefit of overloading the + operator in groovy
   * so you can do column1 + column2
   */
  public BigDecimalColumn plus(BigDecimalColumn column) {
    if (size() > column.size()) {
      for (int i = 0; i < column.size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.add(addVal));
        }
      }
    } else {
      for (int i = 0; i < size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.add(addVal));
        }
      }
    }
    return this;
  }

  public BigDecimalColumn subtract(BigDecimalColumn column) {
    if (size() > column.size()) {
      for (int i = 0; i < column.size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.subtract(addVal));
        }
      }
    } else {
      for (int i = 0; i < size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.subtract(addVal));
        }
      }
    }
    return this;
  }

  public BigDecimalColumn multiply(BigDecimalColumn column) {
    if (size() > column.size()) {
      for (int i = 0; i < column.size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.multiply(addVal));
        }
      }
    } else {
      for (int i = 0; i < size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.multiply(addVal));
        }
      }
    }
    return this;
  }

  public BigDecimalColumn divide(BigDecimalColumn column) {
    if (size() > column.size()) {
      for (int i = 0; i < column.size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.divide(addVal, RoundingMode.HALF_EVEN));
        }
      }
    } else {
      for (int i = 0; i < size(); i++) {
        var orgVal = getBigDecimal(i);
        var addVal = column.getBigDecimal(i);
        if (orgVal == null || addVal == null) {
          setMissing(i);
        } else {
          set(i, orgVal.divide(addVal, RoundingMode.HALF_EVEN));
        }
      }
    }
    return this;
  }

  public BigDecimalColumn apply(Function<BigDecimal, BigDecimal> function) {
    for (int i = 0; i < size(); i++) {
      var val = getBigDecimal(i);
      if (val != null) {
        set(i, function.apply(val));
      }
    }
    return this;
  }


}
