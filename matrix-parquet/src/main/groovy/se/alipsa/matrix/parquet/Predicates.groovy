package se.alipsa.matrix.parquet

import java.time.*

/** Factory for immutable, schema-aware predicates. Null comparisons use explicit null tests. */
final class Predicates {
  private static final String KEY_EQ = 'eq'
  private static final String KEY_NE = 'ne'
  private static final String KEY_LT = 'lt'
  private static final String KEY_LE = 'le'
  private static final String KEY_GT = 'gt'
  private static final String KEY_GE = 'ge'

  private Predicates() {}

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, Number value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, String value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, Boolean value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, LocalDate value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, LocalDateTime value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, Instant value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, LocalTime value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using eq; values must match its logical schema. */
  static ParquetPredicate eq(String column, byte[] value) {
    new ParquetPredicate(KEY_EQ, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, Number value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, String value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, Boolean value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, LocalDate value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, LocalDateTime value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, Instant value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, LocalTime value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using ne; values must match its logical schema. */
  static ParquetPredicate ne(String column, byte[] value) {
    new ParquetPredicate(KEY_NE, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, Number value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, String value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, Boolean value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, LocalDate value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, LocalDateTime value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, Instant value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, LocalTime value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using lt; values must match its logical schema. */
  static ParquetPredicate lt(String column, byte[] value) {
    new ParquetPredicate(KEY_LT, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, Number value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, String value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, Boolean value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, LocalDate value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, LocalDateTime value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, Instant value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, LocalTime value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using le; values must match its logical schema. */
  static ParquetPredicate le(String column, byte[] value) {
    new ParquetPredicate(KEY_LE, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, Number value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, String value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, Boolean value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, LocalDate value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, LocalDateTime value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, Instant value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, LocalTime value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using gt; values must match its logical schema. */
  static ParquetPredicate gt(String column, byte[] value) {
    new ParquetPredicate(KEY_GT, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, Number value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, String value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, Boolean value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, LocalDate value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, LocalDateTime value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, Instant value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, LocalTime value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Compares a column using ge; values must match its logical schema. */
  static ParquetPredicate ge(String column, byte[] value) {
    new ParquetPredicate(KEY_GE, column, value)
  }

  /** Tests whether the column is null. */
  static ParquetPredicate isNull(String column) { new ParquetPredicate('isNull', column, null) }

  /** Tests whether the column is non-null. */
  static ParquetPredicate isNotNull(String column) { new ParquetPredicate('isNotNull', column, null) }

  /** Combines two predicates with AND. */
  static ParquetPredicate and(ParquetPredicate left, ParquetPredicate right) {
    new ParquetPredicate('and', null, null, [left, right])
  }

  /** Combines two predicates with OR. */
  static ParquetPredicate or(ParquetPredicate left, ParquetPredicate right) {
    new ParquetPredicate('or', null, null, [left, right])
  }

  /** Negates an exact predicate, including its documented null behavior. */
  static ParquetPredicate not(ParquetPredicate predicate) {
    new ParquetPredicate('not', null, null, [predicate])
  }
}
