package se.alipsa.matrix.core

import groovy.transform.Immutable

/**
 * Immutable integrity diagnostic. Fields irrelevant to the code are null.
 * columnIndex/name locate a column; rowIndex locates an incompatible cell.
 * expectedLength/actualLength describe ragged columns; expectedType/actualType
 * describe assignability failures, without attempting value conversion.
 */
@Immutable
class MatrixValidationIssue {
  MatrixValidationCode code
  int columnIndex
  String columnName
  Integer rowIndex
  Integer expectedLength
  Integer actualLength
  Class expectedType
  Class actualType
}
