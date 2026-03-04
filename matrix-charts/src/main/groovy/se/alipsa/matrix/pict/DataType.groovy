package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Classifies column types as either {@link #NUMERIC} or {@link #CHARACTER} for chart validation.
 *
 * <p>When validating series inputs, columns of the same category (e.g. Long and Double are both numeric)
 * are considered compatible, while a mismatch (e.g. String vs Double) is not.</p>
 */
@CompileStatic
enum DataType {

  NUMERIC, CHARACTER

  /**
   * Classifies a column type as either {@link #NUMERIC} or {@link #CHARACTER}.
   *
   * @param columnType the class to classify
   * @return the DataType for the given class
   */
  static DataType of(Class columnType) {
    if (Number.isAssignableFrom(columnType)) {
      return NUMERIC
    }
    CHARACTER
  }

  /**
   * Checks whether two column types belong to the same DataType category.
   *
   * @param one the first column type
   * @param two the second column type
   * @return true if both types have the same DataType
   */
  static boolean equals(Class one, Class two) {
    of(one) == of(two)
  }

  /**
   * Checks whether two column types belong to different DataType categories.
   *
   * @param one the first column type
   * @param two the second column type
   * @return true if the types have different DataTypes
   */
  static boolean differs(Class one, Class two) {
    !equals(one, two)
  }

  /**
   * Checks whether a column type is classified as {@link #CHARACTER}.
   *
   * @param columnType the class to check
   * @return true if the type is CHARACTER
   */
  static boolean isCharacter(Class columnType) {
    of(columnType) == CHARACTER
  }

  /**
   * Maps a column type to its corresponding SQL type name.
   *
   * @param columnType the class to map
   * @param varcharSize optional VARCHAR size (defaults to 8000)
   * @return the SQL type string
   */
  static String sqlType(Class columnType, int... varcharSize) {
    switch (columnType) {
      case Short -> 'SMALLINT'
      case Integer -> 'INTEGER'
      case Long -> 'BIGINT'
      case Float -> 'REAL'
      case Boolean -> 'BIT'
      case String -> "VARCHAR(${varcharSize.length > 0 ? varcharSize[0] : 8000})"
      case Double -> 'DOUBLE'
      case LocalDate -> 'DATE'
      case LocalTime -> 'TIME'
      case LocalDateTime, Instant -> 'TIMESTAMP'
      default -> 'BLOB'
    }
  }
}
