package se.alipsa.matrix.charts

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime;

/**
 * When plotting a chart we need to be able to determine whether the column is numeric or categorical
 * When validating a series input we can consider LONG and DOUBLE columns to be OK but not a STRING (categorical) and
 * a DOUBLE (numeric). This class makes this easy to do that.
 * TODO: maybe do something with dates
 */
class DataType {

  public static final String NUMERIC = "numeric"
  public static final String CHARACTER = "character"

  static boolean equals(Class one, Class two) {
    String oneType = dataType(one)
    String twoType = dataType(two)
    return oneType.equals(twoType)
  }

  static boolean differs(Class one, Class two) {
    return !equals(one, two)
  }

  static String dataType(Class columnType) {
    if (Number.isAssignableFrom(columnType)) {
      return NUMERIC
    }
    return CHARACTER
  }

  static boolean isCharacter(Class columnType) {
    return CHARACTER.equals(dataType(columnType))
  }

  static String sqlType(Class columnType, int... varcharSize) {
    if (Short == columnType) {
      return "SMALLINT"
    }
    if (Integer == columnType) {
      return "INTEGER"
    }
    if (Long == columnType) {
      return "BIGINT"
    }
    if (Float == columnType) {
      return "REAL"
    }
    if (Boolean == columnType) {
      return "BIT"
    }
    if (String == columnType) {
      return "VARCHAR(" + (varcharSize.length > 0 ? varcharSize[0] : 8000) + ")"
    }
    if (Double == columnType) {
      return "DOUBLE"
    }
    if (LocalDate == columnType) {
      return "DATE"
    }
    if(LocalTime == columnType) {
      return "TIME"
    }
    if (LocalDateTime == columnType) {
      return "TIMESTAMP"
    }
    if (Instant == columnType) {
      return "TIMESTAMP"
    }
    return "BLOB"
  }
}
