package se.alipsa.matrix.sql

import groovy.transform.CompileStatic

/**
 * Utility methods for rendering SQL identifiers in generated statements.
 */
@CompileStatic
class SqlIdentifier {

  /**
   * Quote an identifier with standard SQL double quotes.
   *
   * @param identifier the identifier to quote
   * @return the quoted identifier
   */
  static String quote(String identifier) {
    String value = requireIdentifier(identifier)
    "\"${value.replace('"', '""')}\""
  }

  /**
   * Render an identifier, optionally quoted.
   *
   * @param identifier the identifier to render
   * @param addQuotes true to quote the identifier, false to render it unquoted
   * @return a rendered SQL identifier
   */
  static String render(String identifier, boolean addQuotes = true) {
    String value = requireIdentifier(identifier)
    addQuotes ? quote(value) : value
  }

  /**
   * Render a table identifier. Simple table names are left unquoted for source compatibility;
   * names containing spaces, punctuation, reserved words, or embedded quotes are quoted.
   *
   * @param tableName the table name to render
   * @param addQuotes true to quote when needed, false to always render unquoted
   * @return a rendered table identifier
   */
  static String renderTable(String tableName, boolean addQuotes = true) {
    String value = requireIdentifier(tableName)
    addQuotes && requiresQuoting(value) ? quote(value) : value
  }

  /**
   * Render multiple identifiers.
   *
   * @param identifiers the identifiers to render
   * @param addQuotes true to quote identifiers, false to render them unquoted
   * @return a list of rendered SQL identifiers
   */
  static List<String> renderAll(Collection<String> identifiers, boolean addQuotes = true) {
    identifiers.collect { String identifier -> render(identifier, addQuotes) }
  }

  /**
   * Create a constraint identifier from a prefix and table name.
   *
   * @param prefix the constraint prefix
   * @param tableName the table name
   * @param addQuotes true to quote the generated constraint name
   * @return a rendered constraint identifier
   */
  static String constraintName(String prefix, String tableName, boolean addQuotes = true) {
    String normalized = "${requireIdentifier(prefix)}_${requireIdentifier(tableName)}"
        .replaceAll(/[^A-Za-z0-9_]/, '_')
        .replaceAll(/_+/, '_')
        .replaceAll(/^_+|_+$/, '')
    render(normalized, addQuotes)
  }

  private static String requireIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("SQL identifier is required but was '$identifier'")
    }
    identifier
  }

  private static boolean requiresQuoting(String identifier) {
    !(identifier ==~ /[A-Za-z_][A-Za-z0-9_]*/) || SQL_KEYWORDS.contains(identifier.toUpperCase())
  }

  private static final Set<String> SQL_KEYWORDS = [
      'ALL',
      'AND',
      'AS',
      'CREATE',
      'DELETE',
      'DROP',
      'FROM',
      'GROUP',
      'INSERT',
      'INTO',
      'JOIN',
      'NOT',
      'NULL',
      'OR',
      'ORDER',
      'PRIMARY',
      'SELECT',
      'SET',
      'TABLE',
      'UPDATE',
      'VALUES',
      'WHERE'
  ] as Set<String>
}
