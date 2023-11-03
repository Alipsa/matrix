package se.alipsa.groovy.matrix.util;

import se.alipsa.groovy.matrix.Matrix;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * When plotting a chart we need to be able to determine whether the column is numeric or categorical
 * When validating a series input we can consider Long and Double columns to be OK but not a String (categorical) and
 * a Double (numeric). This class makes this easy to do that.
 */
public class DataType {

  public static final String NUMERIC = "numeric";
  public static final String CHARACTER = "character";

  public static boolean equals(Class<?> one, Class<?> two) {
    String oneType = dataType(one);
    String twoType = dataType(two);
    return oneType.equals(twoType);
  }

  public static boolean differs(Class<?> one, Class<?> two) {
    return !equals(one, two);
  }

  public static String dataType(Class<?> columnType) {
    if (Number.class.isAssignableFrom(columnType)) {
      return NUMERIC;
    } else {
      return CHARACTER;
    }
  }

  public static boolean isNumeric(Class<?> columnType) {
    return NUMERIC.equals(dataType(columnType));
  }

  public static boolean isCharacter(Class<?> columnType) {
    return CHARACTER.equals(dataType(columnType));
  }

  public static boolean isCategorical(Matrix matrix, String columnName) {
    Class<?> type = matrix.columnType(columnName);
    return isCharacter(type);
  }

  public static String sqlType(Object value, int... varcharSize) {
    if (value instanceof BigDecimal bd) {
      return "DECIMAL(" + bd.precision() + "," + bd.scale() + ")";
    }
    return value == null ? null : sqlType(value.getClass(), varcharSize);
  }

  public static String sqlType(Class<?> columnType, int... varcharSize) {
    if (Byte.class.equals(columnType)) {
      return "TINYINT";
    }
    if (Short.class.equals(columnType) || short.class.equals(columnType)) {
      return "SMALLINT";
    }
    if (Integer.class.equals(columnType) || int.class.equals(columnType)) {
      return "INTEGER";
    }
    if (Long.class.equals(columnType) || long.class.equals(columnType) || BigInteger.class.equals(columnType)) {
      return "BIGINT";
    }
    if (Float.class.equals(columnType) || float.class.equals(columnType)) {
      return "FLOAT";
    }
    if (BigDecimal.class.equals(columnType)) {
      return "DECIMAL";
    }
    if (Boolean.class.equals(columnType) || boolean.class.equals(columnType)) {
      return "BIT";
    }
    if (String.class.equals(columnType)) {
      if (varcharSize.length > 0 && varcharSize[0] > 8000) {
          return "CLOB";
      }
      return "VARCHAR(" + (varcharSize.length > 0 ? varcharSize[0] : 8000) + ")";
    }
    if (Double.class.equals(columnType) || double.class.equals(columnType)) {
      return "DOUBLE";
    }
    if (LocalDate.class.equals(columnType)) {
      return "DATE";
    }
    if(LocalTime.class.equals(columnType)) {
      return "TIME";
    }
    if (LocalDateTime.class.equals(columnType) || Date.class.isAssignableFrom(columnType)) {
      return "TIMESTAMP";
    }
    if (Instant.class.equals(columnType)) {
      return "TIMESTAMP";
    }
    if (byte[].class.equals(columnType)) {
      return "VARBINARY";
    }
    return "VARCHAR(8000)";
  }
}
