package se.alipsa.groovy.matrix.tablesaw

import se.alipsa.groovy.matrix.Matrix
import tech.tablesaw.api.*
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.columns.Column

import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

class TableUtil {

  static Table frequency(Column<?> column) {
    Map<Object, AtomicInteger> freq = new HashMap<>()
    column.forEach(v -> {
      freq.computeIfAbsent(v, k -> new AtomicInteger(0)).incrementAndGet()
    })
    int size = column.size()
    def table = Table.create(column.name())
    def valueCol = ColumnType.STRING.create("Value")
    def freqCol = ColumnType.INTEGER.create("Frequency")
    def percentCol = ColumnType.DOUBLE.create("Percent")
    table.addColumns(valueCol, freqCol, percentCol)
    for (Map.Entry<Object, AtomicInteger> entry : freq.entrySet()) {
      Row row = table.appendRow()
      row.setString("Value", String.valueOf(entry.getKey()))
      int numOccurrence = entry.getValue().intValue()
      row.setInt("Frequency", numOccurrence)
      row.setDouble("Percent", round(numOccurrence * 100.0 / size, 2))
    }
    return table.sortDescendingOn("Frequency")
  }

  static Table frequency(Table table, String columnName) {
    return frequency(table.column(columnName))
  }

  static double round(double value, int numDecimals) {
    if (numDecimals < 0) throw new IllegalArgumentException("numDecimals cannot be a negative number: was " + numDecimals)

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_UP)
    return bd.doubleValue()
  }

  static float round(float value, int numDecimals) {
    if (numDecimals < 0) throw new IllegalArgumentException("numDecimals cannot be a negative number: was " + numDecimals)

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_UP)
    return bd.floatValue()
  }

  static Column<?> round(Column<?> column, int numDecimals) {
    if (column instanceof NumberColumn) {
      return round(column as NumberColumn, numDecimals)
    }
    return column
  }

  static NumberColumn round(NumberColumn column, int numDecimals) {
    if (numDecimals < 0) throw new IllegalArgumentException("numDecimals cannot be a negative number: was " + numDecimals)

    if (column instanceof BigDecimalColumn) {
      column.setScale(numDecimals)
    }

    if (column instanceof DoubleColumn) {
      for (int i = 0; i < column.size(); i++) {
        double val = column.getDouble(i)
        column.set(i, round(val, numDecimals))
      }
    }

    if (column instanceof FloatColumn) {
      for (int i = 0; i < column.size(); i++) {
        float val = column.getFloat(i)
        column.set(i, round(val, numDecimals))
      }
    }
    // everything else (IntColumn, ShortColumn, LongColumn cannot be rounded as they have no decimals
    return column
  }

  static List<List<Object>> toRowList(Table table) {
    List<List<Object>> rowList = new ArrayList<>(table.rowCount())
    int ncol = table.columnCount()
    for (Row row : table) {
      List<Object> r = new ArrayList<>()
      for (int i = 0; i < ncol; i++) {
        r.add(row.getObject(i))
      }
      rowList.add(r)
    }
    return  rowList
  }

  static Matrix fromTablesaw(Table table) {
    List<List<?>> rows = toRowList(table)
    List<Class<?>> columnTypes = new ArrayList<>()
    for (ColumnType type : table.types()) {
      columnTypes.add(classForColumnType(type))
    }
    return Matrix.create(
        table.name(),
        table.columnNames(),
        rows,
        columnTypes
    )
  }

  static Table toTablesaw(Matrix matrix) {
    List<Column<?>> columns = new ArrayList<>()
    for(int i = 0; i < matrix.columnCount(); i++) {
      ColumnType type = columnTypeForClass(matrix.columnType(i))
      Column<?> col = createColumn(type, matrix.columnNames().get(i), matrix.column(i))
      columns.add(col)
    }
    return Table.create(matrix.getName(), columns)
  }

  static <T> Column<T> createColumn(T type, String name, List<?> values) {
    if (type == ColumnType.STRING) {
      var col = StringColumn.create(name)
      for (Object val : values) {
        col.append((String)val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.BOOLEAN) {
      var col = BooleanColumn.create(name)
      for (Object val : values) {
        col.append((Boolean)val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LOCAL_DATE) {
      var col = DateColumn.create(name)
      for (Object val : values) {
        col.append((LocalDate) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LOCAL_DATE_TIME) {
      var col = DateTimeColumn.create(name)
      for (Object val : values) {
        col.append((LocalDateTime) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.INSTANT) {
      var col = InstantColumn.create(name)
      for (Object val : values) {
        col.append((Instant) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LOCAL_TIME) {
      var col = TimeColumn.create(name)
      for (Object val : values) {
        col.append((LocalTime) val)
      }
      return (Column<T>) col
    }
    if (type == BigDecimalColumnType.instance()) {
      var col = BigDecimalColumn.create(name)
      for (Object val : values) {
        col.append((BigDecimal) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.DOUBLE) {
      var col = DoubleColumn.create(name)
      for (Object val : values) {
        col.append((Double) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.FLOAT) {
      var col = FloatColumn.create(name)
      for (Object val : values) {
        col.append((Float) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.INTEGER) {
      var col = IntColumn.create(name)
      for (Object val : values) {
        col.append((Integer) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LONG) {
      var col = LongColumn.create(name)
      for (Object val : values) {
        col.append((Long) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.SHORT) {
      var col = ShortColumn.create(name)
      for (Object val : values) {
        col.append((Short) val)
      }
      return (Column<T>) col
    }

    return null

  }

  static ColumnType columnTypeForClass(Class<?> columnType) {
    if (columnType == String.class) {
      return ColumnType.STRING
    } else if(columnType == Boolean.class) {
      return ColumnType.BOOLEAN
    } else if(columnType == LocalDate.class) {
      return ColumnType.LOCAL_DATE
    } else if(columnType == LocalDateTime.class) {
      return ColumnType.LOCAL_DATE_TIME
    } else if (columnType == Instant.class) {
      return ColumnType.INSTANT
    } else if (columnType == LocalTime.class) {
      return ColumnType.LOCAL_TIME
    } else if (columnType == BigDecimal.class) {
      return BigDecimalColumnType.instance()
    } else if (columnType == Double.class) {
      return ColumnType.DOUBLE
    } else if (columnType == Float.class) {
      return ColumnType.FLOAT
    } else if (columnType == Integer.class) {
      return ColumnType.INTEGER
    } else if (columnType == Long.class) {
      return ColumnType.LONG
    } else if (columnType == Short.class) {
      return ColumnType.SHORT
    } else {
      return ColumnType.SKIP
    }
  }

  static Class<?> classForColumnType(ColumnType type) {
    Class<?> typeClass = type.getClass()
    if (StringColumn.class.isAssignableFrom(typeClass)) {
      return String
    } else if (BooleanColumn.class.isAssignableFrom(typeClass)) {
      return Boolean
    } else if (DateColumn.class.isAssignableFrom(typeClass)) {
      return LocalDate
    } else if (DateTimeColumn.isAssignableFrom(typeClass)) {
      return LocalDateTime
    } else if (InstantColumn.isAssignableFrom(typeClass)) {
      return Instant
    } else if (TimeColumn.isAssignableFrom(typeClass)) {
      return LocalTime
    } else if (BigDecimalColumn.isAssignableFrom(typeClass)) {
      return BigDecimal
    } else if (DoubleColumn.isAssignableFrom(typeClass)) {
      return Double
    } else if (FloatColumn.isAssignableFrom(typeClass)) {
      return Float.class
    } else if (IntColumn.isAssignableFrom(typeClass)) {
      return Integer
    } else if (LongColumn.isAssignableFrom(typeClass)) {
      return Long
    } else if (ShortColumn.isAssignableFrom(typeClass)) {
      return Short
    } else if (BigDecimalColumn.isAssignableFrom(typeClass)) {
      return BigDecimal
    }else {
      // it is some custom column type made outside the "official" tablesaw api
      return Object.class
    }
  }
}
