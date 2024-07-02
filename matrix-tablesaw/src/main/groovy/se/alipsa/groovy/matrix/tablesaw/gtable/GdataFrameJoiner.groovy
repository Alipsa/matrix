package se.alipsa.groovy.matrix.tablesaw.gtable


import tech.tablesaw.api.Table
import tech.tablesaw.joining.DataFrameJoiner

class GdataFrameJoiner extends DataFrameJoiner {
  /**
   * Constructor.
   *
   * @param table The table to join on.
   * @param joinColumnNames The join column names to join on.
   */
  GdataFrameJoiner(Table table, String... joinColumnNames) {
    super(table, joinColumnNames)
  }

  @Override
  Gtable inner(Table... tables) {
    return Gtable.create(super.inner(tables))
  }

  @Override
  Gtable inner(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.inner(allowDuplicateColumnNames, tables))
  }

  @Override
  Gtable inner(Table table2, String col2Name) {
    return Gtable.create(super.inner(table2, col2Name))
  }

  @Override
  Gtable inner(Table table2, String[] col2Names) {
    return Gtable.create(super.inner(table2, col2Names))
  }

  @Override
  Gtable inner(Table table2, String col2Name, boolean allowDuplicateColumnNames) {
    return Gtable.create(super.inner(table2, col2Name, allowDuplicateColumnNames))
  }

  @Override
  Gtable inner(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
    return Gtable.create(super.inner(table2, allowDuplicateColumnNames, col2Names))
  }

  @Override
  Gtable inner(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.inner(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }

  @Override
  Gtable fullOuter(Table... tables) {
    return Gtable.create(super.fullOuter(tables))
  }

  @Override
  Gtable fullOuter(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.fullOuter(allowDuplicateColumnNames, tables))
  }

  @Override
  Gtable fullOuter(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.fullOuter(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }

  @Override
  Gtable fullOuter(Table table2, String col2Name) {
    return Gtable.create(super.fullOuter(table2, col2Name))
  }

  @Override
  Gtable leftOuter(Table... tables) {
    return Gtable.create(super.leftOuter(tables))
  }

  @Override
  Gtable leftOuter(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.leftOuter(allowDuplicateColumnNames, tables))
  }

  @Override
  Gtable leftOuter(Table table2, String[] col2Names) {
    return Gtable.create(super.leftOuter(table2, col2Names))
  }

  @Override
  Gtable leftOuter(Table table2, String col2Name) {
    return Gtable.create(super.leftOuter(table2, col2Name))
  }

  @Override
  Gtable leftOuter(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
    return Gtable.create(super.leftOuter(table2, allowDuplicateColumnNames, col2Names))
  }

  @Override
  Gtable leftOuter(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.leftOuter(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }

  @Override
  Gtable rightOuter(Table... tables) {
    return Gtable.create(super.rightOuter(tables))
  }

  @Override
  Gtable rightOuter(boolean allowDuplicateColumnNames, Table... tables) {
    return Gtable.create(super.rightOuter(allowDuplicateColumnNames, tables))
  }

  @Override
  Gtable rightOuter(Table table2, String col2Name) {
    return Gtable.create(super.rightOuter(table2, col2Name))
  }

  @Override
  Gtable rightOuter(Table table2, String[] col2Names) {
    return Gtable.create(super.rightOuter(table2, col2Names))
  }

  @Override
  Gtable rightOuter(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
    return Gtable.create(super.rightOuter(table2, allowDuplicateColumnNames, col2Names))
  }

  @Override
  Gtable rightOuter(Table table2, boolean allowDuplicateColumnNames, boolean keepAllJoinKeyColumns, String... col2Names) {
    return Gtable.create(super.rightOuter(table2, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names))
  }
}
