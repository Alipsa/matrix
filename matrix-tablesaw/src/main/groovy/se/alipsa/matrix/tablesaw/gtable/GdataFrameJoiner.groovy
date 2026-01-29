package se.alipsa.matrix.tablesaw.gtable


import tech.tablesaw.api.Table
import tech.tablesaw.joining.DataFrameJoiner

/**
 * Groovy-friendly wrapper for Tablesaw's DataFrameJoiner that returns Gtable instances.
 *
 * <p>This class extends {@link DataFrameJoiner} and overrides all join methods to return
 * {@link Gtable} instances instead of plain Table instances, making it more convenient
 * to use in Groovy code where Gtable's additional features are desirable.
 *
 * <p>Supports all standard SQL join types:
 * <ul>
 *   <li>Inner join - returns only matching rows from both tables</li>
 *   <li>Left outer join - returns all rows from left table, matching rows from right</li>
 *   <li>Right outer join - returns all rows from right table, matching rows from left</li>
 *   <li>Full outer join - returns all rows from both tables</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Gtable employees = ...
 * Gtable departments = ...
 *
 * GdataFrameJoiner joiner = new GdataFrameJoiner(employees, "deptId")
 * Gtable result = joiner.inner(departments, "id")
 * }</pre>
 *
 * @see DataFrameJoiner
 * @see Gtable
 */
class GdataFrameJoiner extends DataFrameJoiner {
  /**
   * Constructs a joiner for the specified table and join columns.
   *
   * @param table the table to join (left side of the join)
   * @param joinColumnNames the column names to use as join keys in the left table
   */
  GdataFrameJoiner(Table table, String... joinColumnNames) {
    super(table, joinColumnNames)
  }

  /**
   * Performs an inner join with multiple tables, returning only rows with matching keys in all tables.
   *
   * @param tables the tables to join with
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(Table... tables) {
    return Gtable.create(super.inner(tables))
  }

  /**
   * Performs an inner join with multiple tables, optionally allowing duplicate column names.
   *
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param tables the tables to join with
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.inner(allowDuplicateColumnNames, tables))
  }

  /**
   * Performs an inner join with a single table using a specified join column in the right table.
   *
   * @param table2 the table to join with
   * @param col2Name the join column name in the right table
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(Table table2, String col2Name) {
    return Gtable.create(super.inner(table2, col2Name))
  }

  /**
   * Performs an inner join using multiple join columns in the right table.
   *
   * @param table2 the table to join with
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(Table table2, String[] col2Names) {
    return Gtable.create(super.inner(table2, col2Names))
  }

  /**
   * Performs an inner join with options for duplicate column names.
   *
   * @param table2 the table to join with
   * @param col2Name the join column name in the right table
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(Table table2, String col2Name, boolean allowDuplicateColumnNames) {
    return Gtable.create(super.inner(table2, col2Name, allowDuplicateColumnNames))
  }

  /**
   * Performs an inner join with options for duplicate column names.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
    return Gtable.create(super.inner(table2, allowDuplicateColumnNames, col2Names))
  }

  /**
   * Performs an inner join with full control over join key columns.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param keepAllJoinKeyColumns if true, keeps join key columns from both tables
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the inner join result
   */
  @Override
  Gtable inner(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.inner(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }

  /**
   * Performs a full outer join with multiple tables, returning all rows from all tables.
   *
   * @param tables the tables to join with
   * @return a Gtable containing the full outer join result
   */
  @Override
  Gtable fullOuter(Table... tables) {
    return Gtable.create(super.fullOuter(tables))
  }

  /**
   * Performs a full outer join with options for duplicate column names.
   *
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param tables the tables to join with
   * @return a Gtable containing the full outer join result
   */
  @Override
  Gtable fullOuter(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.fullOuter(allowDuplicateColumnNames, tables))
  }

  /**
   * Performs a full outer join with full control over join options.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param keepAllJoinKeyColumns if true, keeps join key columns from both tables
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the full outer join result
   */
  @Override
  Gtable fullOuter(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.fullOuter(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }

  /**
   * Performs a full outer join using a single join column.
   *
   * @param table2 the table to join with
   * @param col2Name the join column name in the right table
   * @return a Gtable containing the full outer join result
   */
  @Override
  Gtable fullOuter(Table table2, String col2Name) {
    return Gtable.create(super.fullOuter(table2, col2Name))
  }

  /**
   * Performs a left outer join, returning all rows from the left table and matching rows from right tables.
   *
   * @param tables the tables to join with
   * @return a Gtable containing the left outer join result
   */
  @Override
  Gtable leftOuter(Table... tables) {
    return Gtable.create(super.leftOuter(tables))
  }

  /**
   * Performs a left outer join with options for duplicate column names.
   *
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param tables the tables to join with
   * @return a Gtable containing the left outer join result
   */
  @Override
  Gtable leftOuter(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.leftOuter(allowDuplicateColumnNames, tables))
  }

  /**
   * Performs a left outer join using multiple join columns.
   *
   * @param table2 the table to join with
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the left outer join result
   */
  @Override
  Gtable leftOuter(Table table2, String[] col2Names) {
    return Gtable.create(super.leftOuter(table2, col2Names))
  }

  /**
   * Performs a left outer join using a single join column.
   *
   * @param table2 the table to join with
   * @param col2Name the join column name in the right table
   * @return a Gtable containing the left outer join result
   */
  @Override
  Gtable leftOuter(Table table2, String col2Name) {
    return Gtable.create(super.leftOuter(table2, col2Name))
  }

  /**
   * Performs a left outer join with options for duplicate column names.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the left outer join result
   */
  @Override
  Gtable leftOuter(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
    return Gtable.create(super.leftOuter(table2, allowDuplicateColumnNames, col2Names))
  }

  /**
   * Performs a left outer join with full control over join options.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param keepAllJoinKeyColumns if true, keeps join key columns from both tables
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the left outer join result
   */
  @Override
  Gtable leftOuter(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.leftOuter(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }

  /**
   * Performs a right outer join, returning all rows from the right tables and matching rows from the left table.
   *
   * @param tables the tables to join with
   * @return a Gtable containing the right outer join result
   */
  @Override
  Gtable rightOuter(Table... tables) {
    return Gtable.create(super.rightOuter(tables))
  }

  /**
   * Performs a right outer join with options for duplicate column names.
   *
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param tables the tables to join with
   * @return a Gtable containing the right outer join result
   */
  @Override
  Gtable rightOuter(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.rightOuter(allowDuplicateColumnNames, tables))
  }

  /**
   * Performs a right outer join using a single join column.
   *
   * @param table2 the table to join with
   * @param col2Name the join column name in the right table
   * @return a Gtable containing the right outer join result
   */
  @Override
  Gtable rightOuter(Table table2, String col2Name) {
    return Gtable.create(super.rightOuter(table2, col2Name))
  }

  /**
   * Performs a right outer join using multiple join columns.
   *
   * @param table2 the table to join with
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the right outer join result
   */
  @Override
  Gtable rightOuter(Table table2, String[] col2Names) {
    return Gtable.create(super.rightOuter(table2, col2Names))
  }

  /**
   * Performs a right outer join with options for duplicate column names.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the right outer join result
   */
  @Override
  Gtable rightOuter(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
    return Gtable.create(super.rightOuter(table2, allowDuplicateColumnNames, col2Names))
  }

  /**
   * Performs a right outer join with full control over join options.
   *
   * @param table2 the table to join with
   * @param allowDuplicateColumnNames if true, allows duplicate column names in the result
   * @param keepAllJoinKeyColumns if true, keeps join key columns from both tables
   * @param col2Names the join column names in the right table
   * @return a Gtable containing the right outer join result
   */
  @Override
  Gtable rightOuter(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.rightOuter(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }
}
