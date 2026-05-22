package se.alipsa.matrix.core

/**
 * Describes a typed column conversion for builder-style matrix construction.
 *
 * <p>A converter binds a target column name, the destination type, and the
 * closure used to transform incoming values.</p>
 */
class Converter {

  String columnName
  Class<?> type
  Closure converter

  static Converter of(String columnName, Class<?> type, Closure converter) {
    return new Converter(columnName, type, converter)
  }

  Converter(String columnName, Class<?> type, Closure converter) {
    this.columnName = columnName
    this.type = type
    this.converter = converter
  }

}
